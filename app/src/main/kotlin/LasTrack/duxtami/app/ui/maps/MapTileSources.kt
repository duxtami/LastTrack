package LasTrack.duxtami.app.ui.maps

import LasTrack.duxtami.app.core.model.MapProviderId
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

/**
 * Provides live, authentic map tiles for all four select map providers:
 * 1. OpenStreetMap: Standard (Mapnik) vector-raster style.
 * 2. Google Maps: Official Google Satellite Hybrid (satellite imagery + roads + English place names).
 * 3. Mapbox: Natural satellite imagery (Esri World Imagery) with full place names & boundaries overlay.
 * 4. ArcGIS: Official Retina 2x Hybrid Imagery (With Places) from earlier build.
 */
object MapTileSources {

    // --- Official Google Maps Tile Sources ---
    // Google Hybrid (Satellite + Roads + English Place Names & Landmarks)
    private val GoogleHybrid = object : OnlineTileSourceBase(
        "GoogleHybrid", 0, 20, 256, ".jpg",
        arrayOf(
            "https://mt0.google.com/vt/",
            "https://mt1.google.com/vt/",
            "https://mt2.google.com/vt/",
            "https://mt3.google.com/vt/",
        ),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "lyrs=y&hl=en&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    // Google Hybrid HD (&scale=2 for sharp Retina display, as featured in earlier build)
    private val GoogleHybridHD = object : OnlineTileSourceBase(
        "GoogleHybridHD", 0, 20, 256, ".jpg",
        arrayOf(
            "https://mt0.google.com/vt/",
            "https://mt1.google.com/vt/",
            "https://mt2.google.com/vt/",
            "https://mt3.google.com/vt/",
        ),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "lyrs=y&scale=2&hl=en&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    // --- Official ArcGIS (Esri) Satellite Imagery ---
    // High-resolution natural greenish satellite photography from space
    private val ArcGisImagery = object : OnlineTileSourceBase(
        "ArcGisImagery", 0, 19, 256, ".jpg",
        arrayOf("https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + z + "/" + y + "/" + x
        }
    }

    // Official Esri Reference Overlay: Country borders, states, cities, towns, and place names
    private val ArcGisBoundariesAndPlaces = object : OnlineTileSourceBase(
        "ArcGisBoundariesAndPlaces", 0, 19, 256, ".png",
        arrayOf("https://services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/"),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + z + "/" + y + "/" + x
        }
    }

    /**
     * Resolves the primary base tile source for the given provider and basemap style.
     */
    fun resolve(provider: MapProviderId, basemapId: String?): ITileSource {
        return when (provider) {
            MapProviderId.OPEN_STREET_MAP -> TileSourceFactory.MAPNIK
            MapProviderId.GOOGLE_MAPS -> GoogleHybrid
            MapProviderId.MAPBOX -> ArcGisImagery
            MapProviderId.ARCGIS -> GoogleHybridHD
        }
    }

    /**
     * Resolves an optional transparent label/places overlay tile source.
     * When non-null, this overlay is rendered directly on top of the base tile source.
     */
    fun resolveOverlay(provider: MapProviderId, basemapId: String?): ITileSource? {
        return when (provider) {
            MapProviderId.MAPBOX -> ArcGisBoundariesAndPlaces
            else -> null
        }
    }
}
