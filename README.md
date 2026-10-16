# AeroDrive

I'm building a driving app that shows what's flying nearby and, eventually, plays local ATC audio. Android and Android Auto come first.

The app is open source. Core features will stay free. Optional map themes and car icons are planned as one-time purchases, not subscriptions.

**Status:** Austin source prototype. Android compilation, phone/DHU validation, and platform feedback are still open. There is no verified APK or working turn-by-turn navigation in this handoff.

> The build-validation add-on contains tools and documentation only. I merge it into the existing MapLibre/OpenSky source project. It is not a standalone app archive. See [START_VALIDATION.md](START_VALIDATION.md).

## Prototype scope

The previous source handoff describes MapLibre phone and Android Auto maps with pan, zoom, and recenter controls, an OpenFreeMap street style, and a bundled schematic for offline experiments. The schematic is not a road map.

The aircraft source starts with fictional traffic. Selection, visibility, pause, and freshness checks use a fixed downtown Austin origin, a 25 NM radius, and a maximum of five displayed aircraft. Aircraft actions in the car are designed to be parked-only. Those behaviors still require phone and DHU verification.

An optional local Python bridge provides OpenSky integration with server-side credentials, fixture mode, shared caching, and rate-limit handling. I keep live access disabled until the provider permission and live-integration checks are complete. This local bridge is not a public production service.

Maps and aircraft data are independent. The street map can show simulated aircraft, and the test layout can show bridge fixtures.

| Part | Choice |
| --- | --- |
| App | Kotlin and AndroidX Car App Library |
| Maps | MapLibre Native and OpenFreeMap |
| Aircraft | OpenSky Network through a local Python bridge |
| Validation | Kotlin core checks, Python tests, and recorded phone/DHU runs |

Map rendering does not implement routing. Destination search, turn-by-turn directions, GPS integration, aircraft model/operator enrichment, spoken callouts, ATC, and purchases remain outside this prototype.

## Build

The documented prototype baseline is JDK 17, Android SDK Platform 36, Build Tools 35.0.0, and Android 9 or newer. I keep those versions unchanged for the first reproducible build. Python 3.10 or newer runs the validation tools.

The source handoff omitted the Gradle wrapper binaries. I run the existing bootstrap script first when the wrapper is missing:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\bootstrap-gradle.ps1
```

On Linux or macOS:

```bash
bash tools/bootstrap-gradle.sh
```

The existing bootstrap is documented to prepare Gradle 8.13. It has not been re-executed or inspected in this validation session.

I open the full project in Android Studio, select JDK 17, configure the SDK path, and select `debug`. Direct build commands are:

```powershell
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:installDebug
```

On Linux or macOS I use `./gradlew`. A successful build should produce `app/build/outputs/apk/debug/app-debug.apk`.

### Build with evidence

```powershell
py -3 tools/validate_aerodrive.py --check-only
py -3 tools/validate_aerodrive.py
```

The runner uses the project's official wrapper JAR, runs the existing bridge/static checks and clean Gradle tasks, verifies the debug APK signature, and records a separate run directory. It does not download a substitute project, change dependencies, or enable release builds.

For phone installation and DHU connection setup:

```powershell
py -3 tools/validate_aerodrive.py --install --dhu
```

The phone must be connected and authorized, with the Android Auto head-unit server running. Add `--serial SERIAL` to select a device and `--bridge-tunnel` for the local fixture bridge. Installation success is not a visual test result.

[Validation setup](START_VALIDATION.md) | [Phone and DHU checklist](docs/testing.md) | [Recorded status](docs/validation.md)

## Checks

```powershell
py -3 -m unittest discover -s bridge -p "test_*.py" -v
py -3 tools/check-project.py
py -3 -m unittest discover -s tools/tests -p "test_*.py" -v
```

The existing standalone Kotlin check uses `tools/check-core.ps1` or `bash tools/check-core.sh`. Gradle runs the core suite with `:core:test` once the full toolchain is available.

The prior prototype report listed 45 core checks and 29 bridge tests passing. Those original suites were not rerun for this add-on because the source archive could not be accessed. The new validation-runner suite has 33 passing tests. Mock SDK and APK fixtures in that suite do not test Android compilation or rendering.

## Platform review

Aircraft-overlay eligibility is unresolved. I do not treat a map that renders, a parked-only button, or a successful testing-track upload as approval of the aviation layer.

I request written guidance on passive aircraft markers, parked-only alternatives, and marker updates before expanding the car interface. Google requires actual turn-by-turn functionality for the navigation category. The current prototype does not meet that requirement yet.

[Review route and release conditions](docs/platform-review.md) | [Draft question](docs/review/aircraft-overlay-question.md)

## Roadmap

### 1. Feasibility and project setup

- [x] Choose MapLibre, OpenFreeMap, and OpenSky.
- [x] Set Austin as the first test region.
- [x] Prepare the source prototype and local bridge in the earlier handoff.
- [x] Add a build-evidence runner, test profiles, and platform-review draft.
- [ ] Build the Android app and finish the phone/DHU checklist.
- [ ] Get platform feedback on aircraft overlays before treating the car UI as release-ready.

### 2. Navigation

- [ ] Evaluate an open-source routing engine and MapLibre-compatible navigation integration.
- [ ] Add search, route preview, ETA, directions, and rerouting.
- [ ] Keep phone and car sessions in sync and useful with aviation features off.

### 3. Aircraft identification

- [ ] Verify simulated positions, freshness, selection, and car controls on devices.
- [ ] Add spoken identification without interrupting navigation guidance.

### 4. Live aircraft

- [ ] Complete provider permission and live OpenSky checks.
- [ ] Measure Austin coverage, data freshness, and request usage.
- [ ] Add permitted metadata and opt-in announcements.

### 5. ATC and switching

- [ ] Confirm an authorized audio source and its integration terms.
- [ ] Add playback, audio focus, channel lock, and coverage-based switching.
- [ ] Test interruptions, unavailable feeds, and navigation priority.

### 6. Android release

- [ ] Finish phone, DHU, physical head-unit, reconnect, battery, and data tests.
- [ ] Complete real navigation, privacy disclosures, notices, and provider agreements.
- [ ] Replace any debug-only host trust with production host validation.
- [ ] Resolve aircraft-overlay feedback and obtain the required car-app review outcome.

### 7. iPhone and CarPlay

- [ ] Review eligibility and provider rights, then build and test separately.

## Contributing

I keep changes small and include the checks actually run. New behavior needs matching documentation and tests. A pending checklist item stays pending until there is evidence.

## Support

[Ko-fi](https://ko-fi.com/haytamelbouhmidi) | [Patreon](https://www.patreon.com/c/HaytamElbouhmidi)

Support is optional and does not unlock core features.

## License and references

The full source project uses [MIT](LICENSE). Map tiles, aircraft data, and dependencies have separate terms. I retain the existing third-party notices when merging this add-on.

The previous car renderer references [MapLibre's Android Auto sample](https://github.com/maplibre/MapLibre-Android-Auto-Sample). An implementation example is not approval of AeroDrive's use case. Current platform references are in [docs/platform-review.md](docs/platform-review.md).
