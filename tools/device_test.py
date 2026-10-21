"""Device integration checks. Probe fixtures never replace the user's TikTok settings."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts'
ADB=['adb']
MODULE='io.github.sunisup666.tiktools'
PROBE='io.github.sunisup666.tiktools.probe'

def adb(*args,timeout=30):
    return subprocess.check_output(ADB+list(args),timeout=timeout)

def root(command):return adb('exec-out','su','-c',command)

def fixture(profile):
    value={'schema':1,'ip_link':False,'spoof_sdk':'37','spoof_release':'17','spoof_patch':'2026-10-01','tz':'Asia/Tokyo'}
    value.update(enabled=True,region_enabled=True,sim='jp',net='jp',mcc='44010',op='TikTools Test Carrier',
                 language_enabled=True,loc='ja-JP',timezone_enabled=True,tz='Asia/Tokyo',
                 location_inject=True,lat='35.681236',lon='139.767125',altitude='9',accuracy='8',
                 gpsdrift=False,route_mode='fixed',route_radius='20',route_speed='1.4',
                 cell_spoof=True,lac='77',cid='12345',pci='16',wifi_spoof=True,
                 wifi_ssid='TikTools Lab',wifi_bssid='02:10:20:30:40:50',
                 device_spoof=True,spoof_brand='ProbeBrand',spoof_model='ProbeModel',
                 version_spoof=False,ui_enabled=False,trans=False,download_restrictions=False)
    if profile=='disabled':value['enabled']=False
    if profile=='motion':value.update(route_mode='circle',gpsdrift=True)
    if profile=='version':value.update(version_spoof=True,spoof_release='15',spoof_sdk='35')
    if profile=='translation':value.update(trans=True)
    if profile=='ui':
        mappings={key:['sample_'+suffix] for key,suffix in {
            'purify_author':'author','purify_desc':'caption','purify_music':'music','comment_filter':'comment',
            'purify_location':'location','purify_topbar':'topbar','purify_channel':'channel','purify_tabbar':'tabbar',
            'purify_sidebtn':'sidebtn','purify_logo':'logo','purify_translate':'translate','purify_paywall':'paywall'}.items()}
        value.update(ui_enabled=True,show_tool_button=False,filter_keywords='forbidden',custom_selectors=json.dumps(mappings))
        value.update({key:True for key in mappings})
    return value

def run(profile):
    OUT.mkdir(exist_ok=True)
    config=fixture(profile)
    f=OUT/('probe-'+profile+'-config.json');f.write_text(json.dumps(config,ensure_ascii=False),encoding='utf-8')
    parent='/data/user/0/'+MODULE
    uid=root('stat -c %u '+parent).decode().strip();context=root('stat -c %C '+parent).decode().strip()
    assert uid.isdigit() and re.fullmatch(r'[A-Za-z0-9_:,]+',context)
    adb('push',str(f),'/data/local/tmp/codex-tiktools-probe.json')
    script=f'''set -e
mkdir -p {parent}/files
chown {uid}:{uid} {parent}/files
chcon {context} {parent}/files
cp /data/local/tmp/codex-tiktools-probe.json {parent}/files/probe-config.json
chown {uid}:{uid} {parent}/files/probe-config.json
chmod 600 {parent}/files/probe-config.json
chcon {context} {parent}/files/probe-config.json
exit 0
'''
    subprocess.run(ADB+['shell','su','-c','sh'],input=script.encode(),check=True,capture_output=True,timeout=25)
    for command in ['modules enable '+MODULE,'scope add '+MODULE+' '+PROBE+'/0']:
        result=json.loads(root('sh /data/adb/modules/zygisk_vector/cli --json '+command));assert result['success'],result
    adb('shell','am','force-stop',PROBE)
    root('rm -f /data/user/0/'+PROBE+'/files/probe.json /data/user/0/'+PROBE+'/files/translation.json')
    command=['shell','am','start','-n',PROBE+'/.ProbeActivity']
    if profile=='translation':command+=['--ez','translation_probe','true']
    adb(*command)
    time.sleep(8)
    data=json.loads(root('cat /data/user/0/'+PROBE+'/files/probe.json'))
    (OUT/('probe-'+profile+'-result.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
    if profile=='disabled':assert data['latitude']==1 and data['longitude']==2,data
    else:
        for k,want in {'sim':'jp','net':'jp','operator':'44010','operator_name':'TikTools Test Carrier',
                       'lac':77,'cid':12345,'ssid':'"TikTools Lab"','bssid':'02:10:20:30:40:50',
                       'locale':'ja-JP','timezone':'Asia/Tokyo','model':'ProbeModel','brand':'ProbeBrand'}.items():
            assert data.get(k)==want,(k,data.get(k),want,data)
        assert data['accuracy']==8 and data['altitude']==9,data
        if profile=='motion':
            assert abs(data['latitude']-35.681236)<0.001 and abs(data['longitude']-139.767125)<0.001
            assert abs(data['latitude_after']-data['latitude'])+abs(data['longitude_after']-data['longitude'])>0.000001,data
        else:assert abs(data['latitude']-35.681236)<1e-8 and abs(data['longitude']-139.767125)<1e-8,data
        if profile=='version':assert data['sdk']==35 and data['release']=='15',data
        if profile=='ui':assert all(v==4 for v in data['ui_visibility'].values()),data
        if profile=='translation':
            translated=json.loads(root('cat /data/user/0/'+PROBE+'/files/translation.json'))
            (OUT/'translation-result.json').write_text(json.dumps(translated,ensure_ascii=False,indent=2),encoding='utf-8')
            assert translated.get('ok') and re.search(r'[\u4e00-\u9fff]',translated.get('text','')),translated
    print(json.dumps({'profile':profile,'passed':True,'values':data},ensure_ascii=False),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('profile',choices=['disabled','core','motion','version','translation','ui']);args=parser.parse_args();run(args.profile)
