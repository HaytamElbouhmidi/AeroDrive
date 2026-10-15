# AeroDrive

I'm building a driving app that shows what's flying nearby and, eventually, plays local ATC audio. Android and Android Auto come first.

The app is open source. Core features will stay free. Optional map themes and car icons are planned as one-time purchases, not subscriptions.

**Status:** Austin source prototype. The core and bridge tests pass, but the Android app still needs its first successful build and phone/head-unit test. There is no tested APK or working turn-by-turn navigation yet.

## What's here

- MapLibre maps for the phone and an Android Auto map surface, with pan, zoom, and recenter controls.
- Austin street-map configuration using OpenFreeMap, with no map API key.
- A bundled test layout for offline map experiments. It is a schematic, not a road map.
- Fictional aircraft, nearest-aircraft selection, show/hide, pause, and stale-data tests.
- An optional local OpenSky bridge with OAuth2, shared caching, and rate-limit handling. It starts with fictional fixtures, not live data.

The test origin is fixed in downtown Austin, not the phone's location. Aircraft are filtered to 25 nautical miles, with no more than five displayed. The car's aircraft actions are parked-only. Source changes and test settings stay on the phone.

Maps and aircraft data are independent: the street map can show simulated aircraft, and the bundled test layout can show data from the bridge.

## Stack

| Part | Choice |
| --- | --- |
| App | Kotlin and AndroidX Car App Library |
| Maps | MapLibre Native and OpenFreeMap |
| Aircraft | OpenSky Network, through a local Python bridge |
| Tests | Kotlin core checks and Python `unittest` |

MapLibre renders maps; routing is a separate job. Destination search, directions, GPS, live aircraft identification by model/operator, spoken callouts, ATC, and purchases are not implemented.

## Build

Use JDK 17, Android SDK Platform 36, and Build Tools 35.0.0. The minimum Android version is 9. The first build needs internet access to download tools and dependencies.

The Gradle wrapper binaries are not included in this source archive. Run the bootstrap script once before opening the project in Android Studio.

Windows PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\bootstrap-gradle.ps1
```

Linux or macOS:

```bash
bash tools/bootstrap-gradle.sh
```

The script downloads Gradle 8.13, verifies its SHA-256, and creates the official wrapper. Open the project in Android Studio, set the Gradle JDK to 17, and let it configure the Android SDK path. Select `debug`.

```bash
./gradlew :core:test :app:assembleDebug
./gradlew :app:installDebug
```

On Windows, use `.\gradlew.bat` instead of `./gradlew`. The expected output after a successful build is `app/build/outputs/apk/debug/app-debug.apk`.

Start with the built-in simulation. It needs no account or bridge. Use **Test map** to switch to the bundled schematic.

[Full setup](docs/setup.md) | [OpenSky bridge](bridge/README.md) | [Android Auto tests](docs/testing.md)

## OpenSky access

OpenSky is the selected aircraft provider, but its data is not covered by this repository's MIT license. Its [current terms](https://opensky-network.org/about/terms-of-use) require a written agreement for operational REST API use, including free apps.

Live mode stays off until that permission is in place. The bridge can be developed and tested entirely with fixtures. Credentials stay on the computer running the bridge, never in the APK or repository. This loopback-only bridge is for local development, not a public service.

## Checks

```bash
python -m unittest discover -s bridge -p 'test_*.py' -v
python tools/check-project.py
bash tools/check-core.sh
```

The standalone core script needs `kotlinc` and a JDK; Windows has `tools/check-core.ps1`. Gradle runs the same core cases through `:core:test` once the Android toolchain is configured.

Current results: **45 core checks and 29 bridge tests pass**. Android compilation, lint, map rendering, DHU behavior, and live OpenSky access remain unverified. [Validation notes](docs/validation.md) keep those separate.

## Roadmap

### 1. Feasibility and project setup

- [x] Choose MapLibre, OpenFreeMap, and OpenSky.
- [x] Set Austin as the first test region.
- [x] Add the project structure, simulated aircraft, and local bridge.
- [x] Review provider terms and record the Android Auto questions.
- [ ] Build the Android app and finish the phone/DHU checklist.
- [ ] Get platform feedback on aircraft overlays before treating the car UI as release-ready.

### 2. Navigation

- [ ] Evaluate MapLibre Navigation and a self-hosted routing service.
- [ ] Add destination search, route preview, ETA, directions, and rerouting.
- [ ] Keep phone and car navigation in sync and usable with aviation features off.

### 3. Aircraft identification

- [x] Add simulated positions, freshness filtering, and nearest-aircraft selection in the core.
- [ ] Verify the car controls and map limits on a head unit.
- [ ] Add spoken identification without interrupting directions.

### 4. Live aircraft

- [x] Add an OpenSky adapter with server-side credentials and fixture tests.
- [ ] Obtain written permission for the intended display, caching, and app use.
- [ ] Test live access and Austin coverage, then measure actual request usage.
- [ ] Add permitted aircraft metadata and opt-in announcements.

### 5. ATC playback

- [ ] Find an authorized source and confirm its integration terms.
- [ ] Add feed selection, playback, audio focus, and interruption handling.

### 6. Automatic ATC switching

- [ ] Add coverage-based switching, channel lock, and sensible cooldowns.
- [ ] Test unavailable feeds and transitions around navigation instructions.

### 7. Android release

- [ ] Test devices, screens, input modes, reconnects, battery, and data use.
- [ ] Finish routing, privacy disclosures, dependency notices, provider agreements, and car-app review.
- [ ] Replace the debug host validator before enabling release builds.

### 8. iPhone and CarPlay

- [ ] Review platform eligibility and reuse the backend where practical.
- [ ] Build and test the Apple interfaces separately.

## Contributing

Small changes are easier to review. Include the test you ran and update the relevant docs when behavior changes. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Support

[Ko-fi](https://ko-fi.com/haytamelbouhmidi) | [Patreon](https://www.patreon.com/c/HaytamElbouhmidi)

Support is optional and does not unlock core features.

## License and references

[MIT](LICENSE). Map tiles, aircraft data, and dependencies have their own terms. See [third-party notices](third_party/README.md).

The car renderer uses the approach from [MapLibre's Android Auto sample](https://github.com/maplibre/MapLibre-Android-Auto-Sample). Its license is retained. Other implementation and documentation references are listed in [docs/references.md](docs/references.md).
