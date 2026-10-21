"""Reversible integration check on the installed TikTok, with exact settings restoration."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from device_ui import snapshot

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts/tiktok-integration'
ADB=['adb']
MODULE='io.github.sunisup666.tiktools'
TARGET='com.ss.android.ugc.trill'
PREF='/data/user_de/0/'+MODULE+'/shared_prefs/tiktools.xml'
STATUS='/data/user_de/0/'+MODULE+'/shared_prefs/status.xml'

def adb(*args):return subprocess.check_output(ADB+list(args),timeout=35)
def root(command):return adb('exec-out','su','-c',command)
def stop():
    for package in [TARGET,MODULE]:adb('shell','am','force-stop',package)
def launch():
    adb('shell','cmd','statusbar','collapse')
    adb('shell','am','start','-n',MODULE+'/.MainActivity')
    adb('shell','monkey','-p',TARGET,'-c','android.intent.category.LAUNCHER','1')
def settings(xml):
    tree=ET.fromstring(xml)
    node=next(n for n in tree if n.attrib.get('name')=='config')
    return tree,node,json.loads(node.text)
def push(raw):
    staged=OUT/'staged.xml';staged.write_bytes(raw)
    adb('push',str(staged),'/data/local/tmp/codex-tiktools-config.xml')
    root('cat /data/local/tmp/codex-tiktools-config.xml > '+PREF)
def capture(name):
    raw=root('cat '+STATUS)
    tree=ET.fromstring(raw)
    fields={n.attrib['name']:n.text for n in tree if n.tag=='string'}
    report=json.loads(fields.get('report','{}'))
    assert report.get('package')==TARGET and report.get('enabled'),report
    (OUT/(name+'.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    ui=snapshot(ADB)
    (OUT/(name+'.xml')).write_bytes(ui)
    (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
    return report,ET.fromstring(ui)
def tap(node):
    assert node.get('package')==TARGET,('Unexpected input target',node.get('package'))
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def save_current(ui,results):
    previous_fields=ET.fromstring(root('cat '+STATUS))
    previous_id=next((int(n.attrib['value']) for n in previous_fields if n.attrib.get('name')=='last_download_id'),0)
    tool=next(n for n in ui.iter('node') if n.get('text')=='TT');tap(tool);time.sleep(.3)
    _,menu=capture('save-menu')
    tap(next(n for n in menu.iter('node') if n.get('text')=='保存当前直链视频'));time.sleep(.3)
    _,confirm=capture('save-confirm')
    assert any(n.get('text')=='保存当前视频' for n in confirm.iter('node'))
    tap(next(n for n in confirm.iter('node') if n.get('resource-id')=='android:id/button1'));time.sleep(6)
    results['download_action_confirmed']=True
    fields=ET.fromstring(root('cat '+STATUS))
    download_id=next(int(n.attrib['value']) for n in fields if n.attrib.get('name')=='last_download_id')
    assert download_id>0 and download_id!=previous_id
    for attempt in range(60):
        progress=root('content query --uri content://downloads/all_downloads/'+str(download_id)+' --projection _id:status:total_bytes:current_bytes').decode()
        code=re.search(r'status=(\d+)',progress)
        if code and int(code[1])>=200:break
        time.sleep(2)
    results['download_id']=download_id
    results['download_current_media_identified']=True
    results['download_status']=progress.strip()
    assert code and int(code[1])==200,progress

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--purify',action='store_true');parser.add_argument('--save',action='store_true');parser.add_argument('--comments',action='store_true');args=parser.parse_args()
    OUT.mkdir(parents=True,exist_ok=True)
    original=root('cat '+PREF)
    (OUT/'settings-before.xml').write_bytes(original)
    tree,node,config=settings(original)
    config.update(ip_link=False,ui_enabled=True,show_tool_button=args.save)
    expected=['purify_topbar','purify_channel','purify_tabbar','purify_sidebtn','purify_author','purify_desc','purify_music']
    if args.purify:config.update({key:True for key in expected})
    if args.comments:config.update(comment_filter=True,filter_keywords='a,e,i,o,u,的,是,我,你,の,は,이,다')
    node.text=json.dumps(config,ensure_ascii=False)
    results={'mode':'comments' if args.comments else 'purify' if args.purify else 'baseline'}
    try:
        stop();push(ET.tostring(tree,encoding='utf-8',xml_declaration=True));launch();time.sleep(12)
        first,ui=capture(results['mode']+'-first')
        before=adb('shell','pidof',TARGET).strip();assert before
        if args.save and first.get('media_address_observed'):
            results['save_candidates_tried']=1;save_current(ui,results)
        if args.purify:
            missing=[key for key in expected if not first.get('ui_matches',{}).get(key)]
            results['rules_without_match']=missing
        adb('shell','input','swipe','480','1650','480','650','400');time.sleep(10)
        second,ui=capture(results['mode']+'-next')
        after=adb('shell','pidof',TARGET).strip();assert before==after,('TikTok restarted',before,after)
        results.update(module=second.get('module'),same_process=True,first_matches=first.get('ui_matches'),next_matches=second.get('ui_matches'),media_catalog_size=second.get('media_catalog_size'),current_media_identified=second.get('media_address_observed'),native_controls_observed=second.get('native_download_controls_observed'))
        if args.save and not results.get('download_action_confirmed'):
            attempts=0
            while not second.get('media_address_observed') and attempts<4:
                adb('shell','input','swipe','480','1650','480','650','400');time.sleep(4)
                second,ui=capture('save-candidate-'+str(attempts+1));attempts+=1
            results['save_candidates_tried']=attempts+2
            assert second.get('media_address_observed'),'No unambiguous current media URL in sampled videos'
            save_current(ui,results)

        if args.comments:
            candidates=[]
            for attempt in range(6):
                candidates=[n for n in ui.iter('node') if n.get('resource-id','').endswith(':id/eq2')]
                if candidates:break
                adb('shell','input','swipe','480','1650','480','650','400');time.sleep(3)
                _,ui=capture('comments-candidate-'+str(attempt+1))
            assert candidates,'No ordinary video with a comment button was encountered'
            tap(candidates[0])
            time.sleep(8)
            comments,ui=capture('comments-filtered')
            results['filtered_comment_views']=comments.get('ui_total_matches',{}).get('comment_filter',0)
            assert results['filtered_comment_views']>0,comments.get('ui_matches')
        crash=adb('shell','logcat','-b','crash','-d','--pid='+before.decode(),'-t','1000')
        (OUT/'current-process-crash.txt').write_bytes(crash)
        assert b'Fatal signal' not in crash and b'FATAL EXCEPTION' not in crash
    finally:
        stop();push(original)
        restored=root('cat '+PREF)
        assert restored==original,'Settings restoration failed'
        results['settings_restored_sha256']=hashlib.sha256(restored).hexdigest()
        root('rm -f /data/local/tmp/codex-tiktools-config.xml')
        launch()
        (OUT/(results['mode']+'-result.json')).write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(results,ensure_ascii=False),flush=True)

if __name__=='__main__':main()
