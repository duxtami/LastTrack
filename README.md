# LastTrack

An offline Android app for importing, storing and inspecting GPX, KML and KMZ routes.
Kotlin · Jetpack Compose · Material 3 / Material You · Hilt · Room · DataStore.

**Status: Phase 1-2.** Import, parsing, statistics, storage and the full UI shell work. **No map SDK is included
yet** (Phase 3+), no API keys are required, and the app declares no INTERNET permission.

* Application ID / package: `LasTrack.duxtami.app`
* minSdk 26, compile/target SDK 35, Gradle Kotlin DSL, Java/Kotlin 17

## Build

Open the folder in a current Android Studio and sync (it downloads Gradle 8.9 from
`gradle/wrapper/gradle-wrapper.properties`; the wrapper JAR is not committed - run `gradle wrapper` once if you want
`./gradlew`). Run the `app` configuration, or:

```
./gradlew :app:assembleDebug
./gradlew test            # all JVM unit tests
```

Dependency versions are pinned in `gradle/libs.versions.toml`. They are deliberately conservative; bump them as you like.

## Architecture

```
:app            UI (Compose screens, ViewModels, navigation, theme) + Hilt wiring
:core:model     Pure Kotlin. GeoRoute/GeoSegment/GeoPoint/GeoWaypoint/RouteStats/SourceFormat,
                plus domain contracts: RouteRepository, PreferencesRepository, ImportProgress, RouteFilter
:core:parser    Pure Kotlin (JVM). RouteParser, GpxParser, KmlParser, KmzReader, SafeZipExtractor,
                RouteStatsCalculator, RouteBinaryCodec, FormatDetector
:core:data      Android library. Room, DataStore, file storage, RouteImporter, RouteRepositoryImpl
:core:map       Pure Kotlin. MapProvider, MapState, BasemapStyle, MapAvailability, MapProviderRegistry
:map:google     Placeholder MapProvider (reports NotConfigured). No SDK.
:map:mapbox     Placeholder MapProvider (reports NotConfigured). No SDK.
:map:arcgis     Placeholder MapProvider (reports NotConfigured). No SDK.
```

Dependency direction: `app -> data -> parser -> model`, `app -> map:* -> core:map -> model`.
`:core:model` knows nothing about Android, file formats or map SDKs. ViewModels call the `RouteRepository`
interface only; parsing lives in `:core:parser`; Room and files live in `:core:data`; Composables contain no
business logic.

### Import pipeline (`RouteImporter`, runs on `Dispatchers.IO`)

1. **Reading** - copy the picked file into app storage (size-capped at 512 MiB). This copy is kept as the original.
2. Format detection by content first (zip magic, `<gpx`, `<kml`), then by extension.
3. **Parsing** - streaming `XmlPullParser`; the XML is never fully in memory. Coordinates go into primitive
   `DoubleArray`s (`GeoSegment`), not per-point objects. Cancellation and progress are checked every 4096 points.
4. **Processing** - coordinates are written to a compact binary file (`RouteBinaryCodec`): ~8 bytes/point plus
   4 for elevation and 8 for timestamps only when present.
5. **Calculating Statistics**, 6. **Preparing** - metadata row inserted into Room.
7. **Completed** / **Failed(ImportError)**. On any failure or cancellation the partial files are deleted.

Progress is a `Flow<ImportProgress>`: Reading, Parsing, Processing, CalculatingStatistics, Preparing, Completed, Failed.

### Storage

`filesDir/routes/<id>/original.<ext>` (byte-for-byte original) and `coordinates.ltk` (binary coordinates).
Room (`routes` table) stores id, name, file name, format, file size, distance, duration, elevation stats, point /
segment / waypoint counts, import time and both file paths - never individual coordinates. Preferences live in
DataStore.

Export copies the original file, so nothing is lost; sharing copies it to `cache/share/` and exposes it through a
`FileProvider`.

### Statistics rules (never fabricated)

* Distance: haversine sum within segments (gaps between segments are not counted).
* If recorded tracks exist, planned GPX routes (`rte`) are ignored for statistics so a file that stores the same
  path twice is not double counted; routes-only files use the routes.
* Elevation gain/loss uses a 3 m hysteresis to suppress GPS noise (`RouteStatsCalculator.DEFAULT_ELEVATION_THRESHOLD_METERS`).
  Highest/lowest are exact.
* Duration: latest minus earliest timestamp per timestamped segment, summed. Average speed uses the same segments.
* Anything not computable from the file is `null` and is hidden in the UI, not shown as 0.
* KML altitude `0` on every point of a line is treated as "no elevation", since it is KML's placeholder value.

### KMZ safety (`SafeZipExtractor`)

Entry names containing `..` segments, absolute paths, backslashes, drive letters or NUL are rejected (zip slip).
Files are extracted only if they end in `.kml`, under random names inside a controlled cache directory (never the
entry name), with the canonical path re-checked. Limits: 5,000 entries, 512 MiB total inflated, 256 MiB per KML,
and a 200x compression-ratio check (after 16 MiB). Non-KML entries are inflated into the void and counted so a bomb
cannot hide there. Invalid/truncated archives and archives without a KML produce typed errors.

## Supported formats

| Format | Supported |
|---|---|
| GPX 1.0 / 1.1 | tracks, track segments, routes, waypoints, elevation, time |
| KML | Placemarks with Point, LineString (also in MultiGeometry), Google Earth `gx:Track`; Document/Folder names |
| KMZ | zip with `doc.kml` or any `.kml` (`doc.kml` preferred) |

Unsupported KML geometry (polygons, models, network links, overlays) is skipped, not fatal.

## What works in Phase 1-2

Bottom navigation (rail on wide screens): Home, Routes, Maps, Settings, plus Route Details. System file picker import
with a progress dialog and Material 3 error dialogs. Recent routes, route library with search, format chips,
rename / delete / share / export. Route details with statistics. Theme (system/light/dark), dynamic color, AMOLED,
edge-to-edge. The other map settings are stored in DataStore and shown in Settings. The Maps screen lets you choose
Google Maps / Mapbox / ArcGIS and honestly shows "Not configured"; there is no fake map.

## Intentionally postponed

Map SDKs and rendering, offline maps, route editing, elevation chart, GPX/KML/KMZ conversion on export (export
currently writes the original format), cloud sync, accounts, social features, analytics, ads.

## Phase 3: adding Google Maps

1. `:map:google` gains the Maps Compose dependency and a Secrets Gradle Plugin entry for `MAPS_API_KEY`
   (read from `local.properties`, never committed). Add `INTERNET` to that module's manifest.
2. Extend `MapProvider` in `:core:map` with a `@Composable fun Content(state: MapState, route: RouteGeometry?, ...)`
   (making `:core:map` an Android library with only `compose.runtime`, still SDK-free).
3. `GoogleMapProvider.availability` becomes `Ready` when a key is present, otherwise `NotConfigured`.
4. The Maps screen asks the repository for `getRoutePoints(id)`, simplifies geometry for display
   (Ramer-Douglas-Peucker; export still uses the original file), and passes it to the provider.
5. Route statistics already exist, so Phase 3 only needs rendering, camera fit and markers.

## Later: Mapbox and ArcGIS

Same pattern: each provider module adds only its own SDK and credentials (Mapbox secret download token and access
token; ArcGIS Maven repo and API key), implements `MapProvider`, and is registered in `MapModule`. No other module
changes. Basemap ids stay provider-specific; switching provider resets to that provider's default.

## Tests

* `:core:model` - search/filter logic, format lookup
* `:core:parser` - GPX, KML, gx:Track, KMZ extraction, invalid/empty/malformed files, zip slip, zip bombs, entry /
  size / ratio limits, distance, elevation, duration/speed, statistics, codec round-trip and corruption,
  120,000-point streaming + progress + cancellation
* `:core:map` - provider registry / basemap fallback
* `:app` - formatters

Parser tests use kxml2 on the JVM; on Android the platform pull parser is used.
