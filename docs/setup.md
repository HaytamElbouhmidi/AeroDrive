# Development setup

The first target is the phone simulation, then Android Auto through the Desktop Head Unit. This is a source prototype, the Android build has not been verified yet.

## Tools

Use JDK 17, Android SDK Platform 36, Build Tools 35.0.0, and an Android Studio version supporting Android Gradle Plugin 8.13.2. Add Platform Tools for `adb`. Python 3.10 or newer is only needed for the bridge and its tests.

Pinned build versions are AGP 8.13.2, Gradle 8.13, and Kotlin 2.3.0. MapLibre Native is pinned to 12.0.1 and the Car App Library to 1.7.0. These are explicit starting versions, not a claim that every dependency is the latest or that the full combination has already compiled. [AGP compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes).

## First build

From the repository root, run one bootstrap command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\bootstrap-gradle.ps1
```

```bash
bash tools/bootstrap-gradle.sh
```

The script downloads the official Gradle binary distribution and verifies it against the [published checksum](https://gradle.org/release-checksums/). It generates `gradlew`, `gradlew.bat`, and `gradle/wrapper/` from that distribution. Those files are absent until bootstrap succeeds. Commit the resulting official wrapper when setting up the repository.

Open this folder in Android Studio. Install missing SDK components, choose JDK 17 for Gradle, and allow Studio to write `local.properties`. Do not commit that file. For command-line-only use, set `ANDROID_HOME` to the SDK directory or create `local.properties` with `sdk.dir` pointing to it.

Select the `debug` build variant, then run:

```bash
./gradlew :core:test :app:assembleDebug :app:lintDebug
./gradlew :app:installDebug
```

Windows uses `.\gradlew.bat` for the same tasks. Expected APK path: `app/build/outputs/apk/debug/app-debug.apk`. Debug application ID: `com.haytam.aerodrive.debug`.

Launch AeroDrive on the phone. The default is an OpenFreeMap street map with fictional aircraft. No provider credentials are needed for this configuration. The map still uses an internet connection.

If a style fails to load or exceeds the ten-second initial style timeout, the map panel falls back to the bundled schematic. A later tile failure is not a complete offline-map feature. Use **Test map** explicitly for offline testing. Returning to **Street map** reloads the hosted style.

## Replacing the earlier prototype

Use this as the project root rather than copying individual Kotlin files over the Google version. The namespace is now `com.haytam.aerodrive`, and the `googleDebug` and `offlineDebug` flavors are gone. The new build has just `debug`.

Move any old signing files or local settings aside before replacing the project. Do not carry over Google SDK dependencies, API-key resource files, or old `com/aerodrive` source directories. Remove the previous app from the test phone manually if both icons remain after installation. Nothing here deletes the old package automatically.

Keep the existing Git history and remote when merging these files. Review `git diff` before committing.

## Map settings

`developer.properties` is optional and ignored by Git. Copy `developer.properties.example` to that name to use a different HTTPS style endpoint and matching attribution. The defaults are:

```properties
MAP_STYLE_URL=https://tiles.openfreemap.org/styles/liberty
MAP_NIGHT_STYLE_URL=https://tiles.openfreemap.org/styles/dark
MAP_ATTRIBUTION=OpenFreeMap | (c) OpenMapTiles | (c) OpenStreetMap contributors
```

These values go into the APK. They are not a place for secret tokens. Both hosted styles must allow this use, and the attribution must cover their actual content. The offline assets use a separate original schematic with no remote fonts, sprites, or tiles.

Phone and car map cameras are independent, traffic state and the selected map mode are shared. The fixed blue origin marker is downtown Austin, not GPS.

## Local bridge

Follow [bridge/README.md](../bridge/README.md). Use a USB-connected phone or emulator and `adb reverse tcp:8765 tcp:8765`. Select **Use OpenSky bridge** on the phone after the server starts. With the default server configuration, the app must show a fixture/simulation label, never claim the fixture is live OpenSky data.

The app's HTTP exception is debug-only and limited to `127.0.0.1`. There is no LAN URL, public endpoint configuration, or background service. The bridge is not needed for the built-in simulation.

## Android Auto

Follow the [head-unit test checklist](testing.md). This is an Android Auto projected app, not an Android Automotive OS build. The code uses a permissive debug host validator for development. Release variants are intentionally disabled.

## Common failures

| Symptom | Check |
| --- | --- |
| No `gradlew` file | Run the bootstrap script first. A checksum/download failure must be fixed, not bypassed. |
| SDK location missing | Let Android Studio set `local.properties`, or set `ANDROID_HOME`. |
| Java toolchain error | Install JDK 17 and select it as the Gradle JDK. |
| Map shows the schematic | Verify network access and the style URL, then toggle Test map / Street map. |
| Bridge unavailable | Check `adb devices`, the reverse tunnel, server process, and its mode. |
| Old aircraft disappear | Positions expire after 60 seconds, the cache does not make an old position fresh. |
| No aircraft in live mode | Check permission, OAuth credentials, provider responses, coverage, and the fixed Austin bounds. |
| Car app missing or rejected | Check the current official DHU/developer-mode instructions, installed package, and host logs. Do not assume Play approval. |

The [validation log](validation.md) distinguishes checks already run from Android work still pending.
