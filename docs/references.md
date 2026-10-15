# References

Checked October 15, 2026. These are the upstream projects and primary docs behind this revision.

## Code and architecture

- [MapLibre Native](https://github.com/maplibre/maplibre-native): Android map renderer and API examples.
- [MapLibre Android Auto sample](https://github.com/maplibre/MapLibre-Android-Auto-Sample): virtual-display and Presentation approach used by the car map. The sample's MIT notice is retained in [third_party](../third_party/maplibre-android-auto-sample.txt).
- [OpenFreeMap](https://github.com/hyperknot/openfreemap): hosted map source, attribution, and possible self-hosting.
- [OpenSky API](https://github.com/openskynetwork/opensky-api) and [REST docs](https://openskynetwork.github.io/opensky-api/rest.html): authentication and state-vector format. The bridge here is a separate implementation, not a copy of its Python client.
- [MapLibre Navigation](https://github.com/maplibre/maplibre-navigation-android) and [Valhalla](https://github.com/valhalla/valhalla): candidates for the next routing step, not current dependencies.

## Repository layout

I used [StreetComplete](https://github.com/streetcomplete/StreetComplete) and [Organic Maps](https://github.com/organicmaps/organicmaps) as references for keeping the README focused on the app, with build details and contributor notes in separate files. No code, artwork, or README text from those projects is included.

## Platform and data terms

- [Android car-app quality](https://developer.android.com/docs/quality-guidelines/car-app-quality)
- [Drawing maps in car apps](https://developer.android.com/training/cars/apps/library/draw-maps)
- [Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu)
- [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Gradle checksums](https://gradle.org/release-checksums/)
- [OpenFreeMap hosting information](https://openfreemap.org/) and [mobile setup](https://openfreemap.org/quick_start/)
- [OpenSky data terms](https://opensky-network.org/about/terms-of-use)
- [OpenStreetMap copyright](https://www.openstreetmap.org/copyright)

External licenses and services can change. Confirm the actual versions and agreements before a release.
