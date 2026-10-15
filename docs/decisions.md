# Map and aircraft choices

Checked October 15, 2026.

## Direction

I'm using MapLibre for rendering and OpenSky Network for aircraft positions. OpenFreeMap supplies the initial street map. The old Google Navigation SDK variant is removed.

The application code stays open source under MIT. That does not make hosted map services, aircraft data, or the Android Auto host unrestricted. Android Auto is still a Google platform; this is not a replacement for its host software.

## Maps

| Option | Cost boundary | Decision |
| --- | --- | --- |
| MapLibre Native | Open-source renderer, no per-user renderer fee; map hosting is separate | Use now |
| OpenFreeMap public instance | Public hosting is free, with no API key or account; no SLA | Use for the first street-map tests |
| Self-hosted OpenFreeMap/compatible tiles | Software can be self-hosted; storage, compute, bandwidth, and updates cost money | Keep as a later hosting option |
| Google or commercial Mapbox navigation SDK | Proprietary service dependencies and usage terms | Removed from this implementation |

MapLibre Native uses a [BSD 2-Clause license](https://github.com/maplibre/maplibre-native/blob/main/LICENSE.md). [OpenFreeMap](https://openfreemap.org/) documents its public-service terms, self-hosting option, and attribution. Its [mobile guide](https://openfreemap.org/quick_start/) supports MapLibre Native styles. I am not using the public OpenStreetMap tile server as a free app backend.

The app uses Liberty by day and Dark at night, with a separate bundled schematic. The schematic is enough to test rendering and aircraft positioning without downloading a road network. It does not support driving directions. No offline street-map download feature is implemented.

MapLibre Native 12.0.1 is a published dependency, pinned rather than selected dynamically. The Android dependency graph and runtime rendering still need verification on the actual build toolchain.

## Routing is still separate

MapLibre Native does not calculate routes, search for destinations, or run a turn-by-turn session by itself. My next evaluation is [MapLibre Navigation for Android](https://github.com/maplibre/maplibre-navigation-android) with a routing service such as [Valhalla](https://github.com/valhalla/valhalla).

That is a direction to test, not a hidden dependency or completed integration. Map matching, guidance, rerouting, search, road-data updates, and hosting still need implementation and licensing review. I will not treat a public demo routing endpoint as the production backend.

## OpenSky

[OpenSky's terms](https://opensky-network.org/about/terms-of-use) require a previous written agreement for operational REST API use, even by nonprofit applications. A commercial entity also requires permission. The source being MIT and the core being free do not remove those requirements.

Before live use I need written scope covering app display, aircraft identifiers, caching, retention, multiple end users, redistribution, attribution, and any paid cosmetics or donation-supported operation. No agreement has been obtained in this project and no production-license price is confirmed. I am not budgeting this as guaranteed free production data.

The [REST documentation](https://openskynetwork.github.io/opensky-api/rest.html) lists 4,000 daily state credits for a standard account. Our approximately 1.09-square-degree Austin box fits the one-credit request tier. One successful fetch every 30 seconds is roughly 240 requests over two hours, or 2,880 over a full day, before retries or other account use. These are planning calculations, not a usage forecast or license grant.

The adapter uses OAuth2. Positions come from `/states/all`; that response is not a complete operator/model database. Missing position times are discarded rather than replaced with `last_contact`. Barometric altitude is not height above the ground. The app does not infer a model or airline from an incomplete callsign.

The bridge starts with original fixtures so development does not depend on live access. Authorized live use can be enabled later without changing the phone map renderer.

## ATC

ATC is still planned, not connected. No authorized feed, stream integration agreement, or integration price has been established. There are no audio stream URLs, proxy routes, or ATC player dependencies in this revision.

The next audio step is to obtain the rights for a specific source and feed before writing playback and handoff logic. An audible online stream is not automatically licensed for another app.

## Android Auto limits

The map-surface implementation follows the current code in [MapLibre's Android Auto sample](https://github.com/maplibre/MapLibre-Android-Auto-Sample/tree/main/car_common/src/main/java/nl/flitsmeister/car_common), using a virtual display and Presentation rather than repeatedly copying screenshots. Its license notice stays in the project.

The prototype uses a navigation template to test a map surface, not to claim it already qualifies as a navigation app. Google's [car-app quality rules](https://developer.android.com/docs/quality-guidelines/car-app-quality) require actual turn-by-turn navigation and restrict map content and distraction. Aircraft symbols and their movement still need specific review; parked-only aircraft actions do not settle whether the layer itself is allowed.

The map strip contains pan, zoom in, zoom out, and recenter. Aircraft selection and visibility changes are parked-only. Source controls and test settings stay on the phone. There is no audio session or navigation announcement behavior to validate yet.

## Release gates

I need an Android build, a phone/DHU test, routing that works without aviation features, permitted live-data access, a decision on the aircraft layer, dependency notices, and privacy disclosures before a release. Until then this stays debug-only.
