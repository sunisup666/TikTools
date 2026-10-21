"""Package one committed source tree and its verified APK, without private artifacts."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile
from build import source_digest

ROOT=Path(__file__).resolve().parents[1]

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git(*args):
    return subprocess.check_output(['git',*args],cwd=ROOT)


def main():
    if git('status','--porcelain').strip():
        raise RuntimeError('Commit all public source files before packaging a release.')
    build=json.loads((ROOT/'build-result.json').read_text(encoding='utf-8'))
    if source_digest()!=build['source_sha256']:
        raise RuntimeError('Build inputs changed; rebuild before packaging.')
    validation=json.loads((ROOT/'validation.json').read_text(encoding='utf-8'))
    version=validation['version'];code=validation['version_code']
    if not isinstance(code,int) or code<=0 or not version or not all(c in '0123456789.' for c in version):
        raise ValueError('Invalid release version')
    apk=ROOT/build['apks'][0]['file']
    if sha256(apk)!=build['apks'][0]['sha256'] or sha256(apk)!=validation['apk_sha256']:
        raise RuntimeError('APK does not match its build and validation records.')
    target=ROOT/'releases';target.mkdir(exist_ok=True)
    release_apk=target/f'TikTools-{version}.apk';shutil.copy2(apk,release_apk)
    archive=target/f'TikTools-{version}-source.zip'
    git('archive','--format=zip',f'--prefix=TikTools-{version}/',f'--output={archive}','HEAD')
    forbidden={'test-artifacts','.gradle','.gradle-home','.git','build','releases',
               'local.properties','build-result.json','validation.json'}
    with zipfile.ZipFile(archive) as zipped:
        for name in zipped.namelist():
            path=Path(name)
            if forbidden.intersection(path.parts) or path.suffix in ('.jks','.keystore','.apk','.pyc'):
                raise RuntimeError('Private or generated file in source archive: '+name)
    commit=git('rev-parse','HEAD').decode().strip()
    # validation.json is curated; do not copy raw device reports into it.
    report={'name':'TikTools','version':version,'version_code':code,
            'application_id':'io.github.sunisup666.tiktools','git_commit':commit,
            'build_inputs_sha256':build['source_sha256'],'apk_sha256':sha256(release_apk),
            'certificate_sha256':validation['certificate_sha256'],
            'toolchain':{'jdk':'17','gradle':'8.14.3','compile_sdk':36,'build_tools':'36.1.0'},
            'validation':validation['checks'],'limitations':validation['limitations']}
    info=target/'build-info.json'
    info.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    assets=[release_apk,archive,info]
    (target/'SHA256SUMS.txt').write_text(''.join(f'{sha256(p)}  {p.name}\n' for p in assets),encoding='ascii')
    print(json.dumps({'commit':commit,'tag':f'{code}-{version}',
                      'assets':[{'file':p.name,'bytes':p.stat().st_size,'sha256':sha256(p)}
                                for p in assets+[target/'SHA256SUMS.txt']]},indent=2))

if __name__=='__main__':main()
