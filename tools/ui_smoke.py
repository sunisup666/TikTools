"""Capture all settings tabs via Android UI automation; no production settings are changed."""
import json
import argparse
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from device_ui import snapshot

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts/settings-pages'
ADB=['adb']

def adb(*args):return subprocess.check_output(ADB+list(args),timeout=30)

def tree(name):
    raw=snapshot(ADB)
    (OUT/(name+'.xml')).write_bytes(raw)
    return ET.fromstring(raw)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--start-tab',type=int,default=0);args=parser.parse_args()
    OUT.mkdir(parents=True,exist_ok=True)
    adb('shell','input','keyevent','KEYCODE_WAKEUP')
    adb('shell','wm','dismiss-keyguard')
    adb('shell','cmd','statusbar','collapse')
    adb('shell','am','force-stop','io.github.sunisup666.tiktools')
    adb('shell','am','start','-n','io.github.sunisup666.tiktools/.MainActivity')
    time.sleep(1)
    tabs=['概览','地区','定位','设备','界面','工具']
    results=[]
    for i,label in enumerate(tabs):
        if i<args.start_tab:continue
        layout=tree('navigation')
        nodes=[n for n in layout.iter('node') if n.get('text')==label and n.get('class')=='android.widget.Button']
        if not nodes:
            nav=next(n for n in layout.iter('node') if n.get('class')=='android.widget.HorizontalScrollView')
            nx1,ny1,nx2,ny2=map(int,re.findall(r'\d+',nav.get('bounds')))
            adb('shell','input','swipe',str(nx2-15),str((ny1+ny2)//2),str(nx1+15),str((ny1+ny2)//2),'450');time.sleep(.4);layout=tree('navigation')
            nodes=[n for n in layout.iter('node') if n.get('text')==label and n.get('class')=='android.widget.Button']
        assert nodes,('Tab missing',label)
        x1,y1,x2,y2=map(int,re.findall(r'\d+',nodes[0].get('bounds')))
        assert x2>x1 and y2>y1
        adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.4)
        name=f'{i+1:02d}'
        layout=tree(name)
        assert any(n.get('text')=='TikTools' for n in layout.iter('node'))
        assert all(n.get('package') in ('io.github.sunisup666.tiktools','com.android.systemui','android') for n in layout.iter('node'))
        (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
        results.append({'page':label,'captured':True})
    (OUT/'result.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(results,ensure_ascii=False))

if __name__=='__main__':main()
