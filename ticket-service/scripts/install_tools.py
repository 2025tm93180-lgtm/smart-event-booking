#!/usr/bin/env python3
"""Install Java 21 and Maven under this project's .tools folder (macOS/Linux)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tempfile
import urllib.parse

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / '.tools'
MAVEN_VERSION = '3.9.11'
MAVEN_URL = ('https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/'
             + MAVEN_VERSION + '/apache-maven-' + MAVEN_VERSION + '-bin.tar.gz')


def target(system, machine):
    systems = {'Darwin': 'mac', 'Linux': 'linux'}
    architectures = {'arm64': 'aarch64', 'aarch64': 'aarch64', 'x86_64': 'x64', 'amd64': 'x64'}
    if system not in systems or machine.lower() not in architectures:
        raise RuntimeError('Supported: macOS/Linux on Apple Silicon/ARM64 or Intel/AMD64.')
    return systems[system], architectures[machine.lower()]


def fetch(url, destination=None):
    command = ['curl', '--fail', '--location', '--show-error', '--retry', '3',
               '--connect-timeout', '30', '--max-time', '1800',
               '--proto', '=https', '--proto-redir', '=https']
    if destination:
        subprocess.run(command + ['--output', str(destination), url], check=True)
        return None
    return subprocess.run(command + ['--silent', url], check=True,
                          capture_output=True, text=True).stdout


def check_hash(path, expected, algorithm):
    digest = hashlib.new(algorithm)
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    if digest.hexdigest().lower() != expected.strip().lower():
        raise RuntimeError('Checksum mismatch for ' + path.name + '; nothing will be installed from it.')


def extract(archive, destination):
    destination.mkdir()
    subprocess.run(['tar', '-xzf', str(archive), '-C', str(destination)], check=True)
    entries = list(destination.iterdir())
    if len(entries) != 1 or not entries[0].is_dir():
        raise RuntimeError('Unexpected archive layout: ' + archive.name)
    return entries[0]


def java_home(bundle):
    return bundle / 'Contents' / 'Home' if (bundle / 'Contents' / 'Home').is_dir() else bundle


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dry-run', action='store_true', help='Show detected platform and locations without downloading.')
    args = parser.parse_args()
    os_name, arch = target(platform.system(), platform.machine())
    print('Detected: ' + os_name + ' / ' + arch, flush=True)
    print('Tools directory: ' + str(TOOLS), flush=True)
    print('Java: Eclipse Temurin 21; Maven: ' + MAVEN_VERSION, flush=True)
    if args.dry_run:
        return
    if not shutil.which('curl') or not shutil.which('tar'):
        raise RuntimeError('curl and tar are required; both are included with macOS.')
    TOOLS.mkdir(exist_ok=True)
    # The lock prevents two installer processes from sharing installation destinations.
    lock = TOOLS / 'install.lock'
    try:
        descriptor = os.open(str(lock), os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        os.close(descriptor)
    except FileExistsError:
        raise RuntimeError('Another installation is active. If it was interrupted, remove .tools/install.lock and retry.')
    try:
        bundle = TOOLS / 'jdk21'
        maven = TOOLS / ('apache-maven-' + MAVEN_VERSION)
        with tempfile.TemporaryDirectory(prefix='install-', dir=str(TOOLS)) as temporary:
            stage = Path(temporary)
            if not bundle.exists():
                query = urllib.parse.urlencode({'architecture': arch, 'image_type': 'jdk', 'os': os_name, 'vendor': 'eclipse'})
                assets = json.loads(fetch('https://api.adoptium.net/v3/assets/latest/21/hotspot?' + query))
                if not assets:
                    raise RuntimeError('No Java 21 package is available for this platform.')
                package = assets[0]['binary']['package']
                print('Downloading ' + package['name'], flush=True)
                archive = stage / 'java.tar.gz'
                fetch(package['link'], archive)
                check_hash(archive, package['checksum'], 'sha256')
                extracted = extract(archive, stage / 'java')
                if not (java_home(extracted) / 'bin' / 'javac').is_file():
                    raise RuntimeError('Downloaded Java archive is not a JDK.')
                extracted.rename(bundle)
            if not maven.exists():
                archive = stage / 'maven.tar.gz'
                expected = fetch(MAVEN_URL + '.sha512').strip().split()[0]
                print('Downloading Apache Maven ' + MAVEN_VERSION, flush=True)
                fetch(MAVEN_URL, archive)
                check_hash(archive, expected, 'sha512')
                extracted = extract(archive, stage / 'maven')
                if not (extracted / 'bin' / 'mvn').is_file():
                    raise RuntimeError('Downloaded Maven archive is invalid.')
                extracted.rename(maven)
        home = java_home(bundle)
        env = os.environ.copy()
        env['JAVA_HOME'] = str(home)
        env['PATH'] = str(home / 'bin') + os.pathsep + env.get('PATH', '')
        subprocess.run([str(home / 'bin' / 'java'), '-version'], env=env, check=True)
        subprocess.run([str(maven / 'bin' / 'mvn'), '-version'], env=env, check=True)
        (TOOLS / 'toolchain.json').write_text(json.dumps({'os': os_name, 'architecture': arch, 'javaHome': str(home), 'mavenVersion': MAVEN_VERSION}, indent=2) + '\n')
        print('\nReady. Run: bash mvn-local test', flush=True)
        print('Your system Java, Maven, and shell profile were not changed.', flush=True)
    finally:
        lock.unlink()


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        raise SystemExit('Installation failed: ' + str(error))
