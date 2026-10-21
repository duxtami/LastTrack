package LasTrack.duxtami.app.ui.maps

import LasTrack.duxtami.app.core.model.MapProviderId
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

/**
 * High-definition, authentic map tile sources for all map providers:
 * 1. OpenStreetMap:
 *    - Standard (Mapnik)
 *    - White / Light (Carto Positron @2x, 512px)
 *    - Dark Mode (Carto Dark Matter @2x, 512px)
 * 2. Google Maps:
 *    - Satellite Hybrid (HD &scale=2, 512px)
 *    - Streets / Roadmap (HD &scale=2, 512px)
 *    - Terrain (HD &scale=2, 512px)
 * 3. ArcGIS (Official Esri):
 *    - ArcGIS World Street Map (Authentic Esri street cartography with all place names & streets)
 *    - ArcGIS Topographic Map (Elevation, contours, trails & landmarks)
 *    - ArcGIS Satellite Hybrid (World Imagery + Transportation & Boundaries overlay)
 * 4. Mapbox:
 *    - Mapbox Outdoors & Streets (Carto Voyager @2x, 512px, rich Google Maps-like POIs & labels)
 *    - Mapbox Satellite Hybrid (High-res satellite + transportation overlay)
 */
object MapTileSources {

    // =========================================================================
    // 1. Google Maps HD (512x512 Retina Tiles with &scale=2)
    // =========================================================================

    private val GoogleSubdomains = arrayOf(
        "https://mt0.google.com/vt/",
        "https://mt1.google.com/vt/",
        "https://mt2.google.com/vt/",
        "https://mt3.google.com/vt/",
    )

    // Google Satellite Hybrid (Satellite Imagery + Roads + English Place Names & Landmarks)
    val GoogleHybridHD = object : OnlineTileSourceBase(
        "GoogleHybridHD", 0, 20, 512, ".jpg", GoogleSubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "lyrs=y&scale=2&hl=en&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    // Google Streets / Roadmap HD
    val GoogleStreetsHD = object : OnlineTileSourceBase(
        "GoogleStreetsHD", 0, 20, 512, ".png", GoogleSubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "lyrs=m&scale=2&hl=en&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    // Google Terrain HD
    val GoogleTerrainHD = object : OnlineTileSourceBase(
        "GoogleTerrainHD", 0, 20, 512, ".jpg", GoogleSubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "lyrs=p&scale=2&hl=en&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    // =========================================================================
    // 2. OpenStreetMap Styles (Mapnik, Carto Positron White, Carto Dark Matter)
    // =========================================================================

    private val CartoSubdomains = arrayOf(
        "https://a.basemaps.cartocdn.com/",
        "https://b.basemaps.cartocdn.com/",
        "https://c.basemaps.cartocdn.com/",
        "https://d.basemaps.cartocdn.com/",
    )

    // Carto Positron @2x: Ultra-crisp Clean White/Light OpenStreetMap style
    val CartoPositronHD = object : OnlineTileSourceBase(
        "CartoPositronHD", 0, 20, 512, ".png", CartoSubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "rastertiles/light_all/" + z + "/" + x + "/" + y + "@2x.png"
        }
    }

    // Carto Dark Matter @2x: Ultra-crisp Dark Mode OpenStreetMap style
    val CartoDarkMatterHD = object : OnlineTileSourceBase(
        "CartoDarkMatterHD", 0, 20, 512, ".png", CartoSubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "rastertiles/dark_all/" + z + "/" + x + "/" + y + "@2x.png"
        }
    }

    // =========================================================================
    // 3. Mapbox / Geo Tracker Style: Carto Voyager @2x (Rich POIs, Streets & Places)
    // =========================================================================

    // Carto Voyager @2x: Features full Mapbox-derived vector scheme with rich POIs, shop & street names
    val CartoVoyagerHD = object : OnlineTileSourceBase(
        "CartoVoyagerHD", 0, 20, 512, ".png", CartoSubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "rastertiles/voyager/" + z + "/" + x + "/" + y + "@2x.png"
        }
    }

    // =========================================================================
    // 4. Authentic Esri ArcGIS Map Services
    // =========================================================================

    private val EsriServers = arrayOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/",
        "https://services.arcgisonline.com/ArcGIS/rest/services/",
    )

    // Esri World Street Map: Authentic official ArcGIS street map with all cities, streets, landmarks & POIs
    val ArcGisWorldStreets = object : OnlineTileSourceBase(
        "ArcGisWorldStreets", 0, 19, 256, ".jpg", EsriServers,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "World_Street_Map/MapServer/tile/" + z + "/" + y + "/" + x
        }
    }

    // Esri World Topographic Map: Topo contours, trails, mountains, parks and places
    val ArcGisWorldTopo = object : OnlineTileSourceBase(
        "ArcGisWorldTopo", 0, 19, 256, ".jpg", EsriServers,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "World_Topo_Map/MapServer/tile/" + z + "/" + y + "/" + x
        }
    }

    // Esri World Imagery (Satellite)
    val ArcGisWorldImagery = object : OnlineTileSourceBase(
        "ArcGisWorldImagery", 0, 19, 256, ".jpg", EsriServers,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "World_Imagery/MapServer/tile/" + z + "/" + y + "/" + x
        }
    }

    // Esri World Transportation (Roads, Highways & Street Names Overlay)
    val ArcGisTransportation = object : OnlineTileSourceBase(
        "ArcGisTransportation", 0, 19, 256, ".png", EsriServers,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "Reference/World_Transportation/MapServer/tile/" + z + "/" + y + "/" + x
        }
    }

    // =========================================================================
    // Tile Source Resolvers
    // =========================================================================

    /**
     * Resolves the primary base tile source for the given provider and basemap style.
     */
    fun resolve(provider: MapProviderId, basemapId: String?): ITileSource {
        return when (provider) {
            MapProviderId.OPEN_STREET_MAP -> when (basemapId) {
                "osm_white" -> CartoPositronHD
                "osm_dark" -> CartoDarkMatterHD
                else -> TileSourceFactory.MAPNIK
            }

            MapProviderId.GOOGLE_MAPS -> when (basemapId) {
                "google_streets" -> GoogleStreetsHD
                "google_terrain" -> GoogleTerrainHD
                else -> GoogleHybridHD
            }

            MapProviderId.MAPBOX -> when (basemapId) {
                "mapbox_satellite" -> ArcGisWorldImagery
                else -> CartoVoyagerHD
            }

            MapProviderId.ARCGIS -> when (basemapId) {
                "arcgis_topo" -> ArcGisWorldTopo
                "arcgis_hybrid" -> ArcGisWorldImagery
                else -> ArcGisWorldStreets
            }
        }
    }

    /**
     * Resolves an optional transparent label/places overlay tile source.
     * When non-null, this overlay is rendered directly on top of the base tile source.
     */
    fun resolveOverlay(provider: MapProviderId, basemapId: String?): ITileSource? {
        return when (provider) {
            MapProviderId.ARCGIS -> if (basemapId == "arcgis_hybrid") ArcGisTransportation else null
            MapProviderId.MAPBOX -> if (basemapId == "mapbox_satellite") ArcGisTransportation else null
            else -> null
        }
    }
}
