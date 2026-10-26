"""Build signed local APKs and optionally install only after a successful build."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]


def locate_gradle(explicit):
    if explicit:
        candidate = Path(explicit)
        if not candidate.is_file():
            raise FileNotFoundError(candidate)
        return candidate
    wrapper=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
    if wrapper.is_file():return wrapper
    command = shutil.which('gradle') or shutil.which('gradle.bat')
    if command:
        return Path(command)
    cache = Path(os.environ.get('USERPROFILE', '')) / '.gradle/wrapper/dists'
    candidates = list(cache.glob('gradle-8.14.3-bin/*/gradle-8.14.3/bin/gradle.bat'))
    if not candidates:
        raise RuntimeError('Gradle 8.14.3 was not found; pass --gradle with its executable path.')
    return candidates[0]


def source_files():
    files = []
    for folder in ['app/src', 'probe/src', 'app/libs', 'gradle/wrapper']:
        files += [file for file in (ROOT / folder).rglob('*') if file.is_file()]
    files += [ROOT / name for name in ['settings.gradle', 'build.gradle', 'gradle.properties', 'app/build.gradle', 'probe/build.gradle']]
    return sorted(files,key=lambda path:path.relative_to(ROOT).as_posix())


def source_digest(reader=None):
    digest = hashlib.sha256()
    for file in source_files():
        digest.update(file.relative_to(ROOT).as_posix().encode())
        digest.update(b'\0')
        digest.update(file.read_bytes() if reader is None else reader(file))
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--install', action='store_true')
    parser.add_argument('--with-probe', action='store_true', help='Also install the diagnostic app.')
    parser.add_argument('--serial', default=None)
    parser.add_argument('--gradle')
    parser.add_argument('--proxy',default=os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY'))
    args = parser.parse_args()
    configured_key=os.environ.get('TIKTOOLS_KEYSTORE')
    signing_key=ROOT/Path(configured_key or 'local-signing.jks')
    if configured_key and not signing_key.is_file():
        raise FileNotFoundError('TIKTOOLS_KEYSTORE does not point to an existing key.')
    if not signing_key.is_file():
        java_home=Path(os.environ.get('JAVA_HOME',''))
        keytool=java_home/('bin/keytool.exe' if os.name=='nt' else 'bin/keytool')
        if not keytool.is_file():
            found=shutil.which('keytool')
            if not found:raise RuntimeError('Set JAVA_HOME to a JDK 17 installation before building.')
            keytool=Path(found)
        subprocess.run([str(keytool),'-genkeypair','-keystore',str(signing_key),
                        '-storepass','android','-keypass','android','-alias','trillkit','-keyalg','RSA',
                        '-keysize','3072','-validity','10000','-dname','CN=TikTools Local Development'],check=True)
    runner = locate_gradle(args.gradle)
    env = dict(os.environ)
    read_cache = Path(env.get('USERPROFILE', '')) / '.gradle/caches'
    if (read_cache / 'modules-2').is_dir():
        env['GRADLE_RO_DEP_CACHE'] = str(read_cache)
    before = source_digest()
    started = time.time()
    command = [str(runner), '--gradle-user-home', str(ROOT / '.gradle-home'), '--no-daemon']
    if args.proxy:
        proxy=urlparse(args.proxy)
        if proxy.scheme not in ('http','https') or not proxy.hostname or proxy.username:
            raise ValueError('Use an HTTP(S) proxy without embedded credentials, or configure the JVM separately.')
        for protocol in ('http','https'):
            command += [f'-D{protocol}.proxyHost={proxy.hostname}',f'-D{protocol}.proxyPort={proxy.port or 8080}']
    command += [':app:assembleRelease', ':probe:assembleRelease', '--console=plain', '--quiet']
    result = subprocess.run(command, cwd=ROOT, env=env, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT)
    output = result.stdout.decode('utf-8', errors='replace')
    (ROOT / 'build-output.log').write_text(output, encoding='utf-8')
    print(output, flush=True)
    if result.returncode:
        raise SystemExit(f'Build failed ({result.returncode}); no APK was installed.')
    if before != source_digest():
        raise SystemExit('Sources changed during the build; rebuild before installation.')
    paths = [ROOT / f'{name}/build/outputs/apk/release/{name}-release.apk' for name in ['app', 'probe']]
    report = {'source_sha256': before, 'elapsed_seconds': round(time.time() - started, 2),
              'apks': [{'file': path.relative_to(ROOT).as_posix(), 'bytes': path.stat().st_size,
                        'sha256': hashlib.sha256(path.read_bytes()).hexdigest()} for path in paths]}
    (ROOT / 'build-result.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    if args.install:
        adb=['adb']+(['-s',args.serial] if args.serial else [])
        for path in paths if args.with_probe else paths[:1]:
            subprocess.run(adb+['install', '-r', str(path)], check=True)
        subprocess.run(adb+['shell', 'am', 'start', '-n',
                        'io.github.sunisup666.tiktools/.MainActivity'], check=True)
    print(json.dumps(report, ensure_ascii=True), flush=True)


if __name__ == '__main__':
    main()
