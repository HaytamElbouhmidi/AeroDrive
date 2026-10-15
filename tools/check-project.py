"""Check source-package wiring without downloading the Android toolchain."""
from __future__ import annotations

import ast
import json
import re
import sys
from pathlib import Path
from urllib.parse import unquote, urlsplit
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def check_assets() -> None:
    for path in ROOT.rglob('*.xml'):
        ElementTree.parse(path)
    for path in ROOT.rglob('*.json'):
        if 'build' not in path.parts:
            json.loads(path.read_text(encoding='utf-8'))
    for name in ('austin-day.json', 'austin-night.json'):
        path = ROOT / 'app/src/main/assets' / name
        value = json.loads(path.read_text())
        require(value['version'] == 8, f'{name}: expected style version 8')
        require('http://' not in path.read_text() and 'https://' not in path.read_text(),
                f'{name}: bundled schematic must not fetch remote assets')


def check_manifest() -> None:
    manifest = ElementTree.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    permissions = {element.attrib[ANDROID + 'name'] for element in manifest.findall('uses-permission')}
    require('androidx.car.app.ACCESS_SURFACE' in permissions, 'Missing surface permission')
    require('androidx.car.app.NAVIGATION_TEMPLATES' in permissions, 'Missing template permission')
    require(not any('LOCATION' in name for name in permissions), 'The fixed-origin test must not ask for GPS')
    app = manifest.find('application')
    require(app is not None and app.attrib[ANDROID + 'usesCleartextTraffic'] == 'false',
            'Main application must deny cleartext')
    network = ElementTree.parse(ROOT / 'app/src/debug/res/xml/debug_network_security.xml').getroot()
    domains = network.findall('.//domain')
    require(len(domains) == 1 and domains[0].text == '127.0.0.1', 'Unexpected debug HTTP exception')
    require(network.find('base-config').attrib['cleartextTrafficPermitted'] == 'false',
            'Debug network base must deny cleartext')


def check_build() -> None:
    build = (ROOT / 'app/build.gradle.kts').read_text()
    require('org.maplibre.gl:android-sdk:12.0.1' in build, 'MapLibre dependency missing')
    require('com.haytam.aerodrive' in build, 'Wrong app namespace')
    require('productFlavors' not in build, 'Legacy build flavors remain')
    require('withBuildType("release")' in build and 'it.enable = false' in build,
            'Release builds must remain disabled with the test validator')
    dependencies = re.findall(r'implementation\("([^\"]+)"\)', build)
    require(not any('com.google.android.libraries.navigation' in name or name.startswith('com.mapbox')
                    for name in dependencies), 'Unexpected proprietary navigation dependency')
    for path in (ROOT / 'app/src/main/java').rglob('*.kt'):
        require(path.read_text().startswith('package com.haytam.aerodrive'), f'{path.name}: wrong package')


def check_bridge_boundary() -> None:
    example = (ROOT / 'bridge/.env.example').read_text()
    require('OPENSKY_PERMISSION_CONFIRMED=false' in example, 'Live permission must default to false')
    require('OPENSKY_CLIENT_SECRET=\n' in example, 'Credential example must be empty')
    for path in (ROOT / 'app').rglob('*'):
        if path.is_file() and path.suffix in ('.kt', '.kts', '.xml', '.json'):
            text = path.read_text()
            require('OPENSKY_CLIENT_SECRET' not in text and 'opensky-network.org/api/states' not in text,
                    f'{path.name}: upstream access belongs on the bridge')
    ignore = (ROOT / '.gitignore').read_text()
    for value in ('developer.properties', 'local.properties', '.env'):
        require(value in ignore, f'Missing ignore entry: {value}')


def check_python() -> None:
    for path in list((ROOT / 'bridge').glob('*.py')) + list((ROOT / 'tools').glob('*.py')):
        ast.parse(path.read_text(encoding='utf-8'), filename=str(path))


def check_docs() -> None:
    required = ('README.md', 'LICENSE', 'docs/setup.md', 'docs/testing.md', 'docs/decisions.md',
                'docs/validation.md', 'bridge/README.md', 'third_party/maplibre-android-auto-sample.txt',
                'third_party/maplibre-native.txt', 'app/src/main/assets/notices.txt')
    for name in required:
        require((ROOT / name).is_file(), f'Missing file: {name}')
    for path in ROOT.rglob('*.md'):
        for match in re.finditer(r'\[[^\]]*\]\(([^)\s]+)\)', path.read_text(encoding='utf-8')):
            target = urlsplit(match.group(1))
            if target.scheme or target.netloc or not target.path:
                continue
            linked = path.parent / unquote(target.path)
            require(linked.exists(), f'{path.relative_to(ROOT)}: broken local link to {target.path}')
    about = (ROOT / 'app/src/main/assets/notices.txt').read_text()
    require('Copyright (c) 2024 Flitsmeister' in about, 'Missing sample attribution in About')
    require('OpenStreetMap' in about and 'OpenSky' in about, 'Missing data-source notices')


def check_bootstrap() -> None:
    digest = '20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78'
    for suffix in ('sh', 'ps1'):
        text = (ROOT / f'tools/bootstrap-gradle.{suffix}').read_text()
        require(digest in text, f'{suffix}: missing pinned Gradle checksum')
        require('services.gradle.org/distributions/' in text, f'{suffix}: unexpected download source')


def main() -> int:
    checks = (check_assets, check_manifest, check_build, check_bridge_boundary,
              check_python, check_docs, check_bootstrap)
    for check in checks:
        try:
            check()
            print(f'PASS {check.__name__.removeprefix("check_")}')
        except (OSError, ValueError, SyntaxError, KeyError, ElementTree.ParseError) as error:
            print(f'FAIL {check.__name__}: {error}', file=sys.stderr)
            return 1
    print(f'{len(checks)} static checks passed. Android compilation and runtime tests are separate.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
