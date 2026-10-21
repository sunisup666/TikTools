"""Exercise preset and map flows on the phone, then restore the complete previous configuration."""
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from device_ui import snapshot

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts/presets-map'
ADB=['adb']
MODULE='io.github.sunisup666.tiktools'
PREF='/data/user_de/0/'+MODULE+'/shared_prefs/tiktools.xml'

def adb(*args):return subprocess.check_output(ADB+list(args),timeout=35)
def root(cmd):return adb('exec-out','su','-c',cmd)
def config():return json.loads(next(n.text for n in ET.fromstring(root('cat '+PREF)) if n.get('name')=='config'))
def tree():return ET.fromstring(snapshot(ADB))
def capture(name):
    raw=snapshot(ADB);(OUT/(name+'.xml')).write_bytes(raw);(OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'));return ET.fromstring(raw)
def bounds(node):return tuple(map(int,re.findall(r'-?\d+',node.get('bounds'))))
def tap(node):
    assert node.get('package')==MODULE,node.get('package')
    x1,y1,x2,y2=bounds(node);adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.35)
def click(text,button=False,scroll=False):
    for attempt in range(5 if scroll else 3):
        ui=tree();nodes=[n for n in ui.iter('node') if n.get('text')==text and (not button or n.get('class')=='android.widget.Button')]
        if nodes:tap(nodes[0]);return
        assert all(n.get('package') in [MODULE,'android','com.android.systemui'] for n in ui.iter('node'))
        if scroll:adb('shell','input','swipe','540','1900','540','850','350')
        time.sleep(.4)
    capture('failed-control')
    raise AssertionError('Missing control: '+text)
def picked(ui):
    value=next(n.get('text') for n in ui.iter('node') if n.get('text','').startswith('已选：'))
    return tuple(map(float,re.findall(r'-?\d+\.\d+',value)[:2]))

def main():
    OUT.mkdir(parents=True,exist_ok=True);original=root('cat '+PREF);(OUT/'settings-before.xml').write_bytes(original)
    awake=adb('shell','settings','get','global','stay_on_while_plugged_in').decode().strip();assert awake.isdigit()
    results={}
    try:
        adb('shell','settings','put','global','stay_on_while_plugged_in','7')
        for p in ['com.ss.android.ugc.trill',MODULE]:adb('shell','am','force-stop',p)
        adb('shell','cmd','statusbar','collapse');adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard')
        adb('shell','am','start','-n',MODULE+'/.MainActivity');time.sleep(1)
        click('地区',True);click('美国',True)
        cfg=config();assert cfg['sim']=='us' and cfg['mcc']=='310260' and cfg['tz']=='America/Los_Angeles' and not cfg['ip_link'],cfg
        capture('region-preset');results['region_preset']=True;print('Region preset passed',flush=True)
        click('定位',True);click('选择常用城市',True);click('日本 · 东京')
        cfg=config();assert cfg['sim']=='jp' and cfg['tz']=='Asia/Tokyo' and abs(float(cfg['lat'])-35.6762)<1e-8 and cfg['location_source']=='city',cfg
        capture('city-preset');results['city_preset']=True
        click('设备',True);click('选择设备预设',True);click('本机真实设备（推荐）')
        cfg=config()
        for key,prop in {'spoof_model':'ro.product.model','spoof_brand':'ro.product.brand','spoof_device':'ro.product.device','spoof_fingerprint':'ro.build.fingerprint','spoof_sdk':'ro.build.version.sdk'}.items():assert cfg[key]==adb('shell','getprop',prop).decode().strip(),key
        assert cfg['version_spoof'] is False
        capture('device-preset');results['actual_device_preset']=True;print('Device preset passed',flush=True)
        click('定位',True);click('地图选点',True)
        loaded=False
        for attempt in range(15):
            ui=tree();labels=[n.get('text','') for n in ui.iter('node')]
            if any('地图已加载' in text for text in labels):loaded=True;break
            time.sleep(1)
        assert loaded,labels
        ui=capture('map-loaded');assert any('OpenStreetMap contributors' in n.get('text','') for n in ui.iter('node'))
        map_node=next(n for n in ui.iter('node') if n.get('content-desc')=='位置选择地图')
        x1,y1,x2,y2=bounds(map_node);x=(x1+x2)//2;y=(y1+y2)//2
        before=picked(ui);adb('shell','input','tap',str(x+80),str(y+50));time.sleep(.6)
        selected=picked(tree());assert selected[0]<before[0] and selected[1]>before[1],(before,selected)
        ui=tree();tap(next(n for n in ui.iter('node') if n.get('content-desc')=='放大地图'));assert picked(tree())==selected
        adb('shell','input','swipe',str(x+90),str(y+100),str(x-100),str(y+100),'350');time.sleep(.4);assert picked(tree())==selected
        capture('map-selected');click('使用此位置',True)
        cfg=config();assert cfg['location_source']=='custom' and abs(float(cfg['lat'])-selected[0])<1e-6 and abs(float(cfg['lon'])-selected[1])<1e-6,cfg
        saved=(cfg['lat'],cfg['lon']);results.update(map_tiles_loaded=True,tap_axes_correct=True,zoom_keeps_selection=True,pan_keeps_selection=True,map_save=True)
        click('地图选点',True);ui=tree();node=next(n for n in ui.iter('node') if n.get('content-desc')=='位置选择地图');a,b,c,d=bounds(node)
        adb('shell','input','tap',str((a+c)//2-70),str((b+d)//2-40));time.sleep(.5);click('取消',True)
        cfg=config();assert (cfg['lat'],cfg['lon'])==saved;results['map_cancel_keeps_configuration']=True
        click('地区',True);click('IP 国家联动',scroll=True)
        status=ET.fromstring(root('cat /data/user_de/0/'+MODULE+'/shared_prefs/status.xml'));previous=next((int(n.get('value')) for n in status if n.get('name')=='ip_time'),0)
        click('立即按 IP 更新地区',True,scroll=True)
        changed=False
        for attempt in range(22):
            time.sleep(1);status=ET.fromstring(root('cat /data/user_de/0/'+MODULE+'/shared_prefs/status.xml'))
            latest=next((int(n.get('value')) for n in status if n.get('name')=='ip_time'),0)
            if latest>previous:changed=True;break
        assert changed,'IP refresh did not complete'
        cfg=config();assert (cfg['lat'],cfg['lon'])==saved and cfg['location_source']=='custom'
        results['ip_refresh_preserves_map_selection']=True;capture('ip-link-keeps-point');print('Map selection, save, cancel and IP linkage passed',flush=True)
    finally:
        for p in ['com.ss.android.ugc.trill',MODULE]:adb('shell','am','force-stop',p)
        restore=OUT/'restore.xml';restore.write_bytes(original);adb('push',str(restore),'/data/local/tmp/codex-tiktools-presets.xml');root('cat /data/local/tmp/codex-tiktools-presets.xml > '+PREF)
        assert root('cat '+PREF)==original;root('rm -f /data/local/tmp/codex-tiktools-presets.xml')
        adb('shell','settings','put','global','stay_on_while_plugged_in',awake);results['previous_configuration_restored']=True
        adb('shell','am','start','-n',MODULE+'/.MainActivity')
        (OUT/'result.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(results),flush=True)

if __name__=='__main__':main()
