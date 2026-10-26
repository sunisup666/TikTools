"""Remove the temporary diagnostic app and confirm the installed module using normal settings."""
import json
import argparse
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts'
ADB=['adb']
MODULE='io.github.sunisup666.tiktools'
TARGET='com.ss.android.ugc.trill'

def adb(*args):return subprocess.check_output(ADB+list(args),timeout=35)
def root(command):return adb('exec-out','su','-c',command)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--config-baseline',type=Path);args=parser.parse_args()
    cli='sh /data/adb/modules/zygisk_vector/cli --json '
    scope=json.loads(root(cli+'scope ls '+MODULE))
    if any(x['APP_PACKAGE']=='io.github.sunisup666.tiktools.probe' for x in scope['data']):
        result=json.loads(root(cli+'scope rm '+MODULE+' io.github.sunisup666.tiktools.probe/0'));assert result['success'],result
    if b'io.github.sunisup666.tiktools.probe' in adb('shell','pm','list','packages','io.github.sunisup666.tiktools.probe'):
        assert b'Success' in adb('uninstall','io.github.sunisup666.tiktools.probe')
    root('rm -f /data/user/0/io.github.sunisup666.tiktools/files/probe-config.json /data/local/tmp/codex-tiktools-probe.json /data/local/tmp/codex-tiktools-config.xml')
    for package in [TARGET,MODULE]:adb('shell','am','force-stop',package)
    adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard');adb('shell','cmd','statusbar','collapse')
    adb('shell','am','start','-n',MODULE+'/.MainActivity');adb('shell','monkey','-p',TARGET,'-c','android.intent.category.LAUNCHER','1')
    time.sleep(8);pid=adb('shell','pidof',TARGET).strip();assert pid
    (OUT/'final-playing-first.png').write_bytes(adb('exec-out','screencap','-p'))
    time.sleep(16);assert adb('shell','pidof',TARGET).strip()==pid
    (OUT/'final-playing-next.png').write_bytes(adb('exec-out','screencap','-p'))
    crash=adb('shell','logcat','-b','crash','-d','--pid='+pid.decode(),'-t','1000');assert b'FATAL EXCEPTION' not in crash and b'Fatal signal' not in crash
    (OUT/'final-crash.txt').write_bytes(crash)
    status=ET.fromstring(root('cat /data/user_de/0/'+MODULE+'/shared_prefs/status.xml'));report=json.loads(next(n.text for n in status if n.get('name')=='report'))
    assert report['package']==TARGET and report['enabled'];(OUT/'final-tiktok-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    states={m['PACKAGE']:m['STATUS'] for m in json.loads(root(cli+'modules ls'))['data']};assert states[MODULE]=='enabled'
    for conflicting in ['dev.local.trillkit']:
        if conflicting in states:assert states[conflicting]=='disabled',(conflicting,states[conflicting])
    scope=json.loads(root(cli+'scope ls '+MODULE))['data'];assert scope==[{'APP_PACKAGE':TARGET,'USER_ID':0}],scope
    path=adb('shell','pm','path',MODULE).decode().strip().removeprefix('package:');installed=adb('shell','sha256sum',path).decode().split()[0]
    build=json.loads((ROOT/'build-result.json').read_text(encoding='utf-8'));assert installed==build['apks'][0]['sha256']
    cfg_raw=root('cat /data/user_de/0/'+MODULE+'/shared_prefs/tiktools.xml');cfg=json.loads(next(n.text for n in ET.fromstring(cfg_raw) if n.get('name')=='config'))
    (OUT/'final-user-config.xml').write_bytes(cfg_raw)
    saved=args.config_baseline
    if saved is not None:
        before=json.loads(next(n.text for n in ET.fromstring(saved.read_bytes()) if n.get('name')=='config'))
        assert all(cfg.get(k)==v for k,v in before.items() if isinstance(v,bool))
    launcher=adb('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.LAUNCHER','-p',MODULE).decode();launcher_visible='LauncherAlias' in launcher;assert launcher_visible!=cfg.get('hide_icon',False)
    stay_awake=adb('shell','settings','get','global','stay_on_while_plugged_in').decode().strip()
    result={'module':report['module'],'installed_apk_sha256':installed,'normal_config_cold_start_seconds':24,'same_process':True,'new_crash':False,'feature_switches_preserved':True if saved is not None else None,'probe_removed':True,'scope':scope,'modules':states,'launcher_visible':launcher_visible,'stay_awake':stay_awake}
    (OUT/'final-device-state.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(result,ensure_ascii=False),flush=True)

if __name__=='__main__':main()
