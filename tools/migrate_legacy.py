"""Back up the internal beta and migrate its module data using owner-authorized Root ADB."""
import json
import io
from pathlib import Path
import re
import shlex
import subprocess
import tarfile
import time
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts/legacy-migration'
OLD='dev.local.trillkit'
NEW='io.github.sunisup666.tiktools'
TARGET='com.ss.android.ugc.trill'
ADB=['adb']

def adb(*args):return subprocess.check_output(ADB+list(args),timeout=60)
def root(command):return adb('exec-out','su','-c',command)
def cli(command):
    result=json.loads(root('sh /data/adb/modules/zygisk_vector/cli --json '+command))
    if not result.get('success'):raise RuntimeError(result)
    return result
def get_config(raw):return json.loads(next(n.text for n in ET.fromstring(raw) if n.get('name')=='config'))
def exists(path):return root('if test -e '+shlex.quote(path)+'; then echo yes; fi').strip()==b'yes'
def context(path):
    value=root('stat -c %C '+shlex.quote(path)).decode().strip()
    if not re.fullmatch(r'[A-Za-z0-9_:,]+',value):raise RuntimeError('Invalid app data context')
    return value

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    old_de='/data/user_de/0/'+OLD;new_de='/data/user_de/0/'+NEW
    old_ce='/data/user/0/'+OLD;new_ce='/data/user/0/'+NEW
    if not exists(old_de+'/shared_prefs/trillkit.xml'):raise RuntimeError('Internal beta settings not found')
    if not exists(new_ce):raise RuntimeError('Install and open TikTools before migration')
    for package in [TARGET,OLD,NEW]:adb('shell','am','force-stop',package)
    original=root('cat '+old_de+'/shared_prefs/trillkit.xml');get_config(original)
    (OUT/'legacy-settings.xml').write_bytes(original)
    modules=cli('modules ls');(OUT/'modules-before.json').write_text(json.dumps(modules,indent=2),encoding='utf-8')
    old_apk=adb('shell','pm','path',OLD).decode().strip().removeprefix('package:')
    if '\n' in old_apk:raise RuntimeError('Unexpected split module APK')
    adb('pull',old_apk,str(OUT/'legacy-module.apk'))
    # This private backup is outside the public source file list and is ignored by Git.
    backup=root('tar -C '+old_ce+' -czf - files no_backup shared_prefs')
    with tarfile.open(fileobj=io.BytesIO(backup),mode='r:gz') as archive:
        if not archive.getmembers():raise RuntimeError('Empty private data backup')
    (OUT/'legacy-private-data.tar.gz').write_bytes(backup)
    uid=root('stat -c %u '+new_ce).decode().strip()
    if not uid.isdigit():raise RuntimeError('Invalid target UID')
    ce_context=context(new_ce);de_context=context(new_de)
    settings=OUT/'staged-settings.xml';settings.write_bytes(original);adb('push',str(settings),'/data/local/tmp/codex-tiktools-migrate.xml')
    commands=['set -e','mkdir -p '+new_de+'/shared_prefs','cp /data/local/tmp/codex-tiktools-migrate.xml '+new_de+'/shared_prefs/tiktools.xml']
    if exists(old_de+'/shared_prefs/status.xml'):commands.append('cp '+old_de+'/shared_prefs/status.xml '+new_de+'/shared_prefs/status.xml')
    commands += ['chown -R '+uid+':'+uid+' '+new_de+'/shared_prefs','chmod 700 '+new_de+'/shared_prefs','chmod 600 '+new_de+'/shared_prefs/*.xml','chcon -R '+de_context+' '+new_de+'/shared_prefs']
    models=old_ce+'/no_backup/com.google.mlkit.translate.models'
    if exists(models):
        links=root('find '+models+' -type l').strip()
        if links:raise RuntimeError('Model backup contains unexpected symbolic links')
        commands += ['mkdir -p '+new_ce+'/no_backup','cp -R '+models+' '+new_ce+'/no_backup/','chown -R '+uid+':'+uid+' '+new_ce+'/no_backup','chcon -R '+ce_context+' '+new_ce+'/no_backup']
    pref_names=root('ls -1 '+old_ce+'/shared_prefs').decode().splitlines()
    copied_prefs=False
    for name in pref_names:
        if not name.startswith('com.google.mlkit') or not re.fullmatch(r'[A-Za-z0-9_.-]+\.xml',name):continue
        commands += ['mkdir -p '+new_ce+'/shared_prefs','cp '+old_ce+'/shared_prefs/'+name+' '+new_ce+'/shared_prefs/'+name]
        copied_prefs=True
    if copied_prefs:commands += ['chown -R '+uid+':'+uid+' '+new_ce+'/shared_prefs','chcon -R '+ce_context+' '+new_ce+'/shared_prefs']
    commands += ['rm -f /data/local/tmp/codex-tiktools-migrate.xml','exit 0']
    copied=subprocess.run(ADB+['shell','su','-c','sh'],input=('\n'.join(commands)+'\n').encode(),capture_output=True,timeout=60)
    if copied.returncode:
        raise RuntimeError('Migration copy failed: '+copied.stderr.decode('utf-8',errors='replace'))
    actual=root('cat '+new_de+'/shared_prefs/tiktools.xml')
    if get_config(actual)!=get_config(original):raise RuntimeError('Configuration mismatch after copy')
    cli('modules disable '+OLD);cli('modules enable '+NEW);cli('scope set '+NEW+' '+TARGET+'/0')
    adb('shell','am','start','-n',NEW+'/.MainActivity')
    adb('shell','monkey','-p',TARGET,'-c','android.intent.category.LAUNCHER','1')
    time.sleep(12)
    status=ET.fromstring(root('cat '+new_de+'/shared_prefs/status.xml'))
    report=json.loads(next(n.text for n in status if n.get('name')=='report'))
    if report.get('package')!=TARGET or bool(report.get('enabled'))!=bool(get_config(original).get('enabled')) or not report.get('module','').startswith('TikTools '):raise RuntimeError('New module has not reported an active TikTok session')
    result={'configuration_copied':True,'language_models_copied':exists(models),'old_module_disabled':True,'new_module':report.get('module'),'target':TARGET,'new_package':NEW,'legacy_app_retained_for_rollback':True}
    (OUT/'result.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result),flush=True)

if __name__=='__main__':main()
