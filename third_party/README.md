# Third-party notices

AeroDrive's code is under the [MIT license](../LICENSE). That does not replace the licenses of its dependencies or the terms attached to map and aircraft data.

| Component | License or terms | Use here |
| --- | --- | --- |
| MapLibre Native | [BSD 2-Clause](maplibre-native.txt) | Android map renderer |
| MapLibre Android Auto sample | [MIT](maplibre-android-auto-sample.txt) | Reference for the virtual-display and Presentation approach in `CarMap.kt` |
| AndroidX and Kotlin | [Apache 2.0](apache-2.0.txt) | App library, lifecycle, Android utilities, and Kotlin runtime |
| OpenFreeMap | [Project and hosting information](https://openfreemap.org/) | Default hosted street-map styles and tiles |
| OpenMapTiles | [Attribution and licensing](https://openmaptiles.org/copyright/) | Schema and cartography used by the hosted map |
| OpenStreetMap contributors | [Copyright and ODbL](https://www.openstreetmap.org/copyright) | Underlying street-map data |
| OpenSky Network | [Data terms](https://opensky-network.org/about/terms-of-use) | Optional live aircraft source, not included in this repository |

The bundled Austin schematic, aircraft icons, and test fixtures are original project assets. They are not downloaded street-map extracts or recorded OpenSky responses.

The map keeps visible attribution. The phone's About dialog includes the primary notices and data-source links. Custom map hosts need their own correct attribution in `developer.properties`.

Before distributing an APK, resolve the actual dependency graph and audit its transitive Java and native libraries, fonts, map styles, and any required NOTICE files. This source package is not a completed binary-license inventory. Keep any additional notices required by the resolved dependencies in the app and distribution.
