# Changelog

## 0.2.0-dev

- Replaced the Google navigation variant with MapLibre Native and OpenFreeMap.
- Added a shared phone/car map panel and separate street/test-map controls.
- Added a local OpenSky OAuth2 bridge, with fixtures as the default source.
- Kept account secrets outside the Android project and APK.
- Added stale-position, malformed-response, cache, and rate-limit checks.
- Moved the namespace to `com.haytam.aerodrive` and reduced the app to one debug variant.
- Added setup notes, a head-unit checklist, and upstream license notices.

This is still source-only. Android compilation, phone/DHU testing, authorized live data, routing, and ATC remain open.
