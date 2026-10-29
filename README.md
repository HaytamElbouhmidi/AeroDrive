# AeroDrive

**A driving-map project with aircraft information alongside the map.**

Version 0.2.1-dev · Early testing · Android 9 or later · Android Auto prototype

I'm building AeroDrive to help you see what's flying nearby without switching between a driving map and a separate flight-tracking app. Android phones and Android Auto come first. Android Auto is the system that displays compatible phone apps on a car's screen.

**The current version is a demonstration, not a finished navigation app.** It shows a map and made-up aircraft around Austin, Texas. It does not yet provide turn-by-turn directions, live flight tracking, or air traffic control radio.

The app is open source. Core features will stay free. Optional map themes and car icons are planned as one-time purchases, not subscriptions.

[Try the test version](#get-the-first-apk) · [What works now](#what-can-i-do-with-it-now) · [Future plans](#roadmap) · [Build instructions](#local-build)

## What can I do with it now?

The test version lets you explore the map and try the aircraft controls:

- **Nearest:** Select the nearest demonstration aircraft and view its details.
- **Hide aircraft / Show aircraft:** Remove or restore the aircraft symbols.
- **Pause:** Stop the made-up aircraft from moving.
- **Test map:** Open a simple built-in test layout without an internet connection.
- **Stale test / Restore data:** Check how the app handles aircraft information that has become too old to display.

The map starts at a fixed point in downtown Austin, not your phone's GPS location. It shows at most five fictional aircraft within 25 nautical miles of that point. The built-in test layout is not an offline street map.

The project includes a phone view and an Android Auto view. Aircraft controls in the car view are for use while parked. Whether aircraft can be displayed while driving still needs platform review.

## Is it ready for everyday use?

Not yet. This is for people who want to try an early version and report what works or fails.

The [October 16, 2026 automated build](https://github.com/HaytamElbouhmidi/AeroDrive/actions/runs/38916734207) passed its source checks, Android build, and code checks, then uploaded a test APK. An **APK** is the file used to install an Android app. A **debug APK** is a test build, not a finished public release.

That successful build does not prove the app works correctly on every phone or car display. Phone and car-display testing has not been recorded in the repository's validation notes. Do not rely on this version for driving directions or real aircraft information.

## Get the first APK

For the first test, you need an Android phone running Android 9 or later. You do not need a car, an OpenSky account, or live aircraft data. Test while stationary.

1. Open the [verified test-build page](https://github.com/HaytamElbouhmidi/AeroDrive/actions/runs/38916734207).
2. Find **Artifacts**, which is GitHub's name for files produced by a build. Download **aerodrive-debug-3**. GitHub may ask you to sign in.
3. Extract the downloaded ZIP and find **app-debug.apk**.
4. Follow the [phone installation and test instructions](docs/first-build.md#first-phone-test-only-after-the-build-passes). The current documented installation uses Android Studio or ADB, an Android command-line tool. It is not a one-click installer yet.
5. Open AeroDrive and try the controls listed above. Start with the built-in demonstration, not the optional live-data bridge.

Build downloads can expire. If that download is no longer available, the [full first-build guide](docs/first-build.md) explains how to create a new test build with GitHub Actions or on your own computer. GitHub Actions builds run on GitHub's computers, so that route does not require installing the Android development tools locally.

**Code > Download ZIP downloads the project source, not an installable phone app.** To install a test build, you need the APK inside a successful build's artifact.

If you report a problem, include your phone model, Android version, what you tapped, and the message or behavior you saw. Keep phone testing separate from testing a car display. The [full checklist](docs/testing.md) covers both.

## What is planned?

- Driving directions, destination search, estimated arrival time, and rerouting.
- Real nearby aircraft, once the data provider has authorized the intended use.
- Spoken aircraft identification.
- Air traffic control audio, once an authorized audio source is available.
- Further work on the Android Auto experience, followed by separate iPhone and CarPlay work.

These are plans, not features in the current test version. The detailed [roadmap](#roadmap) tracks the remaining work.

## Technical details

The sections below are for building, troubleshooting, or contributing. You do not need to understand the tools listed here just to understand the project.

## What's here

- MapLibre Native maps with OpenFreeMap street styles and an original offline test schematic.
- Fictional traffic around a fixed downtown Austin origin, filtered to 25 nautical miles and capped at five aircraft.
- A shared phone/car traffic state with nearest, show/hide, pause, stale-data, and source controls.
- An optional local OpenSky bridge with fixture mode, server-side credentials, caching, and rate-limit handling.
- Build validation, evidence collection, a phone/DHU checklist, and an unsubmitted platform-review request.

The origin is not the phone's GPS location. The schematic is not an offline road map. The car's aircraft actions are parked-only. Aircraft visibility while driving is still an unresolved platform question.

## CI setup correction history

I restored `.gitignore`, `.gitattributes`, `bridge/.env.example`, and `validation/.gitignore`. These are source-controlled templates and rules, not private configuration. The OpenSky example keeps live permission disabled and both credential fields empty.

Both workflows now request only `platform-tools` from `setup-android` and explicitly select command-line tools build `12266719`. The previous action default requested the unavailable `tools` package. The separate SDK installation step still installs Platform 36 and Build Tools 35.0.0. Gradle remains 8.13 and Java remains 17.

The correction subsequently passed on GitHub at commit `c5ae04c` on October 16, 2026. Both [Build and validate](https://github.com/HaytamElbouhmidi/AeroDrive/actions/runs/38916734207) and [Submit Gradle dependencies](https://github.com/HaytamElbouhmidi/AeroDrive/actions/runs/38916734204) succeeded. This corrects the earlier setup failure, not the outstanding phone and car-display testing.

[Failure details and patch steps](docs/ci-setup-fix.md)

## Gradle dependency-scan correction

The reported `submit-gradle` failure used Gradle 9.8.1 after finding no `gradlew`. I added a separate **Submit Gradle dependencies** workflow that explicitly selects Gradle 8.13, JDK 17, Android SDK Platform 36, and Build Tools 35.0.0 on Ubuntu 24.04. This is a workflow correction, not an Android compilation result. The app version remains 0.2.1-dev.

For a repository using this replacement workflow, set **Settings > Advanced Security > Dependency graph > Automatic dependency submission** to **Disabled**. Keep the dependency graph and Dependabot alerts enabled. The repository-owned workflow replaces the GitHub-managed scan. Adding the YAML alone does not turn the old scan off.

[Correction and setup](docs/gradle-dependency-fix.md) | [Dependency workflow](.github/workflows/dependency-submission.yml)

## Changes in 0.2.1

I replaced the removed Kotlin Gradle configuration with typed compiler options and kept the build on JDK 17. I corrected car Surface-handle cleanup, unknown safe-area handling, and zoom events without a usable focal point. Phone recreation no longer forces the map back to the initial camera, and landscape controls sit beside the map rather than consuming a fixed-height panel.

The app chrome, aircraft symbols, and bundled schematic use the green, navy, and neutral palette. Hosted road-map styles keep their upstream styling and attribution. No custom font files are bundled.

The earlier validation add-on is already merged here. No second ZIP needs to be merged into this project.

## Build downloads and workflow details

The repository includes **Build and validate**, a GitHub Actions workflow. It prepares JDK 17 and the Android SDK, bootstraps the checksum-verified Gradle wrapper, runs the checks, attempts a clean debug build and lint, and verifies the APK signature. Only a successful validation run uploads the debug-build artifact. Failure logs are retained when available.

The successful October 16, 2026 run uploaded `aerodrive-debug-3`, `aerodrive-gradle-wrapper-3`, and `aerodrive-build-evidence-3`. Future runs must be checked individually. The dependency scan is separate from **Build and validate**.

When importing the project into a new repository, put the project files at the repository root, including `.github`. Commit the files, not the ZIP itself. On GitHub, open **Actions > Build and validate**. A push starts the workflow, or it can be started with **Run workflow** after the workflow is on the default branch.

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
| Earlier workflow YAML and shell syntax checks | Two workflows parsed and embedded Bash checked locally |
| GitHub source check at `1e9d856` | Failed because `bridge/.env.example` was missing |
| GitHub dependency submission at `1e9d856` | Failed because SDK setup requested `tools` |
| GitHub source and Python checks at `c5ae04c` | Passed on October 16, 2026 |
| Android build and lint at `c5ae04c` | Passed, test APK uploaded |
| GitHub dependency submission at `c5ae04c` | Passed |
| Phone and DHU | Not run |
| Platform feedback | Not submitted |

The standalone Kotlin core and syntax counts above are retained from the earlier local validation record. They are separate from the later successful GitHub build. The standalone core runner does not resolve Android APIs or the Gradle DSL. Runner tests use mocked build commands and do not produce a real APK. An earlier local Android build attempt stopped before compilation because the SDK, JDK 17, and wrapper were missing, and the Gradle download failed DNS resolution. [Validation notes and logs](docs/validation.md) retain that historical result. The linked October 16 GitHub run provides the later successful build result.

The dependency-workflow correction has its own [validation record](docs/validation/gradle-workflow-fix/README.md). The build gate has now passed. The phone test and DHU test still need separate recorded results. DHU means Desktop Head Unit, Google's car-display test tool.

## OpenSky access

OpenSky is the selected provider, but its data is not licensed by this repository's MIT license. Its [terms](https://opensky-network.org/about/terms-of-use) require an agreement for operational REST API use, including free apps. Live mode stays off until the intended use is authorized.

The bridge starts with fixtures. Credentials belong on the bridge computer, not in the APK or public repository. The loopback-only bridge is a development tool, not a public service. No live OpenSky test or provider agreement has been recorded.

## Roadmap

### 1. Feasibility and project setup

- [x] Choose MapLibre, OpenFreeMap, and OpenSky.
- [x] Set Austin as the first test region.
- [x] Add the project structure, simulated aircraft, and local bridge.
- [x] Review provider terms and record the Android Auto questions.
- [x] Build the Android app and pass the automated build checks.
- [ ] Finish the phone/DHU checklist.
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
