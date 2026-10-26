"""Device UI checks for app languages; restore locale, display settings and configuration."""
import base64
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from device_ui import snapshot

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts/localization'
ADB=['adb'];MODULE='io.github.sunisup666.tiktools';TARGET='com.ss.android.ugc.trill'
PREF='/data/user_de/0/'+MODULE+'/shared_prefs/tiktools.xml'

def adb(*args):return subprocess.check_output(ADB+list(args),timeout=40)
def root(command):return adb('exec-out','su','-c',command)
def locales():
    raw=adb('shell','cmd','locale','get-app-locales',MODULE).decode()
    return re.search(r'\[(.*?)\]',raw).group(1).strip()
def locale(value):
    command=['shell','cmd','locale','set-app-locales',MODULE]
    if value:command+=['--locales',value]
    adb(*command);time.sleep(.7)
def launch():
    adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard')
    adb('shell','cmd','statusbar','collapse');adb('shell','am','start','-n',MODULE+'/.MainActivity');time.sleep(.5)
def tree():return ET.fromstring(snapshot(ADB))
def bounds(node):return tuple(map(int,re.findall(r'\d+',node.get('bounds',''))))
def tap(node):
    assert node.get('package') in (MODULE,'android'),node.attrib
    x1,y1,x2,y2=bounds(node);assert x2>x1 and y2>y1
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.3)
def has(ui,text):return any(n.get('text','').casefold()==text.casefold() for n in ui.iter('node'))
def click(text,nav=False,prefix=False):
    for attempt in range(9):
        ui=tree();nodes=[n for n in ui.iter('node') if (n.get('text','').casefold().startswith(text.casefold()) if prefix else n.get('text','').casefold()==text.casefold()) and n.get('enabled')=='true']
        if nodes:tap(nodes[0]);return
        views=[n for n in ui.iter('node') if n.get('class')==('android.widget.HorizontalScrollView' if nav else 'android.widget.ScrollView')]
        if not views:break
        x1,y1,x2,y2=bounds(views[0])
        if nav:adb('shell','input','swipe',str(x2-12),str((y1+y2)//2),str(x1+12),str((y1+y2)//2),'300')
        else:adb('shell','input','swipe',str((x1+x2)//2),str(y2-70),str((x1+x2)//2),str(y1+70),'300')
        time.sleep(.2)
    raise AssertionError('Control missing: '+text)
def capture(name):
    raw=snapshot(ADB);(OUT/(name+'.xml')).write_bytes(raw)
    (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'));return ET.fromstring(raw)
def selected(ui):
    text=next(n.get('text') for n in ui.iter('node') if re.search(r'(?:Selected:|已选：).*\d',n.get('text','')))
    return tuple(re.findall(r'-?\d+\.\d+',text))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    for package in [TARGET,MODULE]:adb('shell','am','force-stop',package)
    original=root('cat '+PREF);original_locale=locales()
    awake=adb('shell','settings','get','global','stay_on_while_plugged_in').decode().strip()
    font=adb('shell','settings','get','system','font_scale').decode().strip()
    (OUT/'settings-before.xml').write_bytes(original)
    results={}
    try:
        adb('shell','settings','put','global','stay_on_while_plugged_in','7')
        raw=adb('shell','am','instrument','-w','io.github.sunisup666.tiktools.probe/.LocaleInstrumentation')
        match=re.search(rb'INSTRUMENTATION_RESULT: result_b64=([^\r\n]+)',raw);assert match,raw.decode(errors='replace')
        report=json.loads(base64.b64decode(match[1]));assert report['passed'];results['installed_resource_checks']=report
        locale('en');launch()
        pages=[('Overview','App language'),('Region','Region presets'),('Location','Location presets'),('Device','Device presets'),('Interface','Interface rules'),('Tools','On-device translation')]
        for index,(tab,heading) in enumerate(pages):
            click(tab,nav=True);ui=capture('en-page-'+str(index+1));assert has(ui,heading),(tab,heading)
        results['english_pages']=6;print('Six English settings pages passed',flush=True)
        adb('shell','am','force-stop',MODULE);launch();assert has(tree(),'Overview');results['english_persists_after_restart']=locales().startswith('en')
        click('Choose app language',prefix=True);click('简体中文');time.sleep(.6);assert has(tree(),'概览') and locales().startswith('zh')
        capture('zh-overview');click('选择界面语言',prefix=True);click('English');time.sleep(.6);assert has(tree(),'Overview')
        results['in_app_language_picker_both_directions']=True
        click('Region',nav=True);click('Country / region',prefix=True);ui=capture('en-regions');assert has(ui,'Japan · JP') and has(ui,'Philippines · PH');adb('shell','input','keyevent','KEYCODE_BACK')
        click('Location',nav=True);click('Choose a city');ui=tree();search=next(n for n in ui.iter('node') if n.get('class')=='android.widget.EditText');tap(search);adb('shell','input','text','Tokyo');time.sleep(.4)
        ui=capture('en-city-search');assert has(ui,'Japan · Tokyo');adb('shell','input','keyevent','KEYCODE_BACK')
        if has(tree(),'Cancel'):click('Cancel')
        assert has(tree(),'Location presets');results['english_region_and_city_search']=True
        click('Device',nav=True);click('Choose a device preset');ui=capture('en-devices');assert has(ui,'This device (recommended)');adb('shell','input','keyevent','KEYCODE_BACK')
        click('Location',nav=True);click('Choose on map');assert any(n.get('content-desc')=='Location selection map' for n in tree().iter('node'))
        click('Jump to a city');click('Japan · Tokyo');ui=capture('en-map');point=selected(ui);assert any('Japan · Tokyo' in n.get('text','') for n in ui.iter('node'))
        locale('zh-Hans');ui=capture('zh-map-recreated');assert selected(ui)==point and any('日本 · 东京' in n.get('text','') for n in ui.iter('node'))
        click('取消');results['map_selection_survives_locale_recreation']=True
        locale('en');adb('shell','am','force-stop',MODULE);launch();click('Tools',nav=True)
        adb('shell','settings','put','system','font_scale','1.3');time.sleep(.7);ui=capture('en-tools-large-font');assert has(ui,'On-device translation');results['english_large_font']=True
        adb('shell','settings','put','system','font_scale',font);adb('shell','am','force-stop',MODULE);launch();click('Choose app language',prefix=True);click('Follow system');time.sleep(.7)
        assert locales()=='';results['follow_system_clears_override']=True
        assert root('cat '+PREF)==original,'Language UI changed module settings'
        results['configuration_unchanged']=True
    finally:
        for package in [TARGET,MODULE]:adb('shell','am','force-stop',package)
        if root('cat '+PREF)!=original:
            restore=OUT/'restore.xml';restore.write_bytes(original);adb('push',str(restore),'/data/local/tmp/tiktools-locale-restore.xml');root('cat /data/local/tmp/tiktools-locale-restore.xml > '+PREF);root('rm -f /data/local/tmp/tiktools-locale-restore.xml')
        assert root('cat '+PREF)==original
        locale(original_locale);adb('shell','settings','put','system','font_scale',font);adb('shell','settings','put','global','stay_on_while_plugged_in',awake)
        launch();results['previous_state_restored']=True;results['configuration_sha256']=hashlib.sha256(original).hexdigest()
        (OUT/'result.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(results,ensure_ascii=False),flush=True)

if __name__=='__main__':main()
