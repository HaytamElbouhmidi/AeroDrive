# AeroDrive

I'm building a driving app that shows what's flying nearby and, eventually, plays local ATC audio. Android and Android Auto come first.

The app is open source. Core features will stay free. Optional map themes and car icons are planned as one-time purchases, not subscriptions.

**Status: 0.2.1-dev, CI setup under repair.** Both repository workflows ran on October 16, 2026, at commit `1e9d856`. The source check stopped on a missing configuration example, and the dependency workflow stopped during SDK setup. Android compilation and lint have not passed. There is no verified APK or phone/DHU result. Turn-by-turn navigation is not implemented.

## What's here

- MapLibre Native maps with OpenFreeMap street styles and an original offline test schematic.
- Fictional traffic around a fixed downtown Austin origin, filtered to 25 nautical miles and capped at five aircraft.
- A shared phone/car traffic state with nearest, show/hide, pause, stale-data, and source controls.
- An optional local OpenSky bridge with fixture mode, server-side credentials, caching, and rate-limit handling.
- Build validation, evidence collection, a phone/DHU checklist, and an unsubmitted platform-review request.

The origin is not the phone's GPS location. The schematic is not an offline road map. The car's aircraft actions are parked-only. Aircraft visibility while driving is still an unresolved platform question.

## Current CI setup correction

I restored `.gitignore`, `.gitattributes`, `bridge/.env.example`, and `validation/.gitignore`. These are source-controlled templates and rules, not private configuration. The OpenSky example keeps live permission disabled and both credential fields empty.

Both workflows now request only `platform-tools` from `setup-android` and explicitly select command-line tools build `12266719`. The previous action default requested the unavailable `tools` package. The separate SDK installation step still installs Platform 36 and Build Tools 35.0.0. Gradle remains 8.13 and Java remains 17.

This correction has passed local checks against the previous source package with the patches applied. It has not run on GitHub yet. I do not treat local source checks as Android compilation or device testing.

[Failure details and patch steps](docs/ci-setup-fix.md)

## Gradle dependency-scan correction

The reported `submit-gradle` failure used Gradle 9.8.1 after finding no `gradlew`. I added a separate **Submit Gradle dependencies** workflow that explicitly selects Gradle 8.13, JDK 17, Android SDK Platform 36, and Build Tools 35.0.0 on Ubuntu 24.04. This is a workflow correction, not an Android compilation result. The app version remains 0.2.1-dev.

After committing the patch, set **Settings > Advanced Security > Dependency graph > Automatic dependency submission** to **Disabled**. Keep the dependency graph and Dependabot alerts enabled. The repository-owned workflow replaces the GitHub-managed scan. Adding the YAML alone does not turn the old scan off.

[Correction and setup](docs/gradle-dependency-fix.md) | [Dependency workflow](.github/workflows/dependency-submission.yml)

## Changes in 0.2.1

I replaced the removed Kotlin Gradle configuration with typed compiler options and kept the build on JDK 17. I corrected car Surface-handle cleanup, unknown safe-area handling, and zoom events without a usable focal point. Phone recreation no longer forces the map back to the initial camera, and landscape controls sit beside the map rather than consuming a fixed-height panel.

The app chrome, aircraft symbols, and bundled schematic use the green, navy, and neutral palette. Hosted road-map styles keep their upstream styling and attribution. No custom font files are bundled.

The earlier validation add-on is already merged here. No second ZIP needs to be merged into this project.

## Get the first APK

[First build and phone test](docs/first-build.md) has the handoff steps.

The repository includes **Build and validate**, a GitHub Actions workflow. It prepares JDK 17 and the Android SDK, bootstraps the checksum-verified Gradle wrapper, runs the checks, attempts a clean debug build and lint, and verifies the APK signature. Only a successful validation run uploads the debug-build artifact. Failure logs are retained when available.

No successful APK-build run has been recorded in these notes. The dependency-scan failure is separate from **Build and validate**. An uploaded workflow file is not a successful build.

Put the contents of the extracted `aerodrive` directory at the repository root, including `.github`. Commit the files, not the ZIP itself. On GitHub, open **Actions > Build and validate**. A push starts the workflow, or it can be started with **Run workflow** after the workflow is on the default branch.

The workflow also saves `aerodrive-gradle-wrapper-<run number>` after wrapper generation, before the app build. Its four files can be committed later using [the wrapper instructions](docs/gradle-dependency-fix.md). A wrapper artifact is not proof that the app built.

After a successful run, the `aerodrive-debug-<run number>` artifact contains `app-debug.apk`, its SHA-256 in `run.json`, and the signature log. The separate `aerodrive-build-evidence-<run number>` artifact contains the build evidence. I do not treat an APK artifact as a phone/DHU pass.

## Local build

Use JDK 17, Android SDK Platform 36, Build Tools 35.0.0, and Python 3.10 or later for the validation runner. The Android project is pinned to AGP 8.13.2, Kotlin 2.3.0, and Gradle 8.13. The minimum Android version is 9.

The wrapper binaries are not bundled. From the extracted project root, run the bootstrap once before opening the project in Android Studio.

Windows PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\bootstrap-gradle.ps1
```

Linux or macOS:

```bash
bash tools/bootstrap-gradle.sh
```

The first build needs network access for the tools and dependencies. Open the project in Android Studio, select JDK 17 for Gradle, and let it write the SDK location to `local.properties`. Select the `debug` variant.

```powershell
py -3 tools/validate_aerodrive.py --check-only
py -3 tools/validate_aerodrive.py
```

On Linux/macOS use `python3` in place of `py -3`. The runner writes each result under `validation/runs/`. It never fills in visual checks automatically.

For a direct Gradle build:

```powershell
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug
```

Use `./gradlew` on Linux/macOS. After a successful build, the APK is at `app/build/outputs/apk/debug/app-debug.apk`. Use `--install` with the validation runner only when ready for the phone test, and `--dhu` only when starting the car checklist.

[Full setup](docs/setup.md) | [OpenSky bridge](bridge/README.md) | [Phone/DHU checklist](docs/testing.md)

## Checks and current results

```bash
python -m unittest discover -s bridge -p 'test_*.py' -v
python -m unittest discover -s tools/tests -p 'test_*.py' -v
python tools/check-project.py
bash tools/check-core.sh
```

| Check | Recorded result |
| --- | --- |
| Standalone Kotlin core cases | 61 passed using Kotlin CLI 1.9.0 and OpenJDK 21, targeting JVM 17 bytecode |
| Python bridge tests | 29 passed |
| Python tooling tests | 51 passed locally, including 33 runner tests, 12 workflow checks, and 6 CI setup checks |
| Source-package checks | 9 passed |
| Kotlin/Gradle-script syntax | 20 files parsed, syntax only |
| Workflow YAML and shell syntax | Two workflows parsed, embedded Bash checked, not executed as CI |
| GitHub source check at `1e9d856` | Failed because `bridge/.env.example` was missing |
| GitHub dependency submission at `1e9d856` | Failed because SDK setup requested `tools` |
| Android build and lint | Skipped after the source check failed |
| Phone and DHU | Not run |
| Platform feedback | Not submitted |

The Kotlin core and syntax results above are retained from the earlier validation record and were not rerun for this CI-only patch. The standalone core runner does not resolve Android APIs or the Gradle DSL. Runner tests use mocked build commands and do not produce a real APK. The Android build attempt stopped before compilation because the SDK, JDK 17, and wrapper were missing, and the Gradle download failed DNS resolution. [Validation notes and logs](docs/validation.md) record the exact boundary.

The dependency-workflow correction has its own [validation record](docs/validation/gradle-workflow-fix/README.md). No current phone or DHU test is needed until the APK build result has been checked.

## OpenSky access

OpenSky is the selected provider, but its data is not licensed by this repository's MIT license. Its [terms](https://opensky-network.org/about/terms-of-use) require an agreement for operational REST API use, including free apps. Live mode stays off until the intended use is authorized.

The bridge starts with fixtures. Credentials belong on the bridge computer, not in the APK or public repository. The loopback-only bridge is a development tool, not a public service. No live OpenSky test or provider agreement has been recorded.

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

Keep changes focused, record the checks that actually ran, and update the relevant documentation. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Support

[Ko-fi](https://ko-fi.com/haytamelbouhmidi) | [Patreon](https://www.patreon.com/c/HaytamElbouhmidi)

Support is optional and does not unlock core features.

## License and references

[MIT](LICENSE). Map tiles, aircraft data, and dependencies have separate terms. See [third-party notices](third_party/README.md).

[MapLibre's Android Auto sample](https://github.com/maplibre/MapLibre-Android-Auto-Sample) is a structure and texture-rendering reference. Its sample uses texture-to-canvas rendering. AeroDrive uses a separate virtual-display and Presentation implementation. The upstream notices are retained. Primary documentation and implementation references are in [docs/references.md](docs/references.md).
