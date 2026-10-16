# Changelog

## Unreleased

### Build workflows

- Added a dependency-submission workflow with Gradle 8.13, JDK 17, the pinned Android SDK components, and Ubuntu 24.04.
- Added a wrapper artifact to the APK workflow so the generated official wrapper can be committed later.
- Added workflow configuration checks and documented replacement of GitHub's automatic dependency scan.
- Kept all Android and core source code unchanged. No successful APK or device test is recorded by this workflow patch.

## 0.2.1-dev

- Replace the removed Kotlin options DSL with typed compiler options and JDK 17 toolchains.
- Release received car Surface handles and continue cleanup if a display resource is already detached.
- Treat empty host safe-area rectangles as unknown and handle unavailable zoom focal points.
- Preserve the phone camera on recreation and use a side-by-side landscape layout.
- Apply the engineering palette to app chrome, aircraft symbols, and the bundled schematic.
- Merge the build-validation runner and add failure evidence to the GitHub workflow.
- Add 16 viewport and gesture core regression cases.

Source checks pass. Android compilation and runtime tests remain pending. No APK was produced in this source-review environment.

## 0.2.0-dev

- Replaced the Google navigation variant with MapLibre Native and OpenFreeMap.
- Added a shared phone/car map panel and separate street/test-map controls.
- Added a local OpenSky OAuth2 bridge, with fixtures as the default source.
- Kept account secrets outside the Android project and APK.
- Added stale-position, malformed-response, cache, and rate-limit checks.
- Moved the namespace to `com.haytam.aerodrive` and reduced the app to one debug variant.
- Added setup notes, a head-unit checklist, and upstream license notices.

This is still source-only. Android compilation, phone/DHU testing, authorized live data, routing, and ATC remain open.
