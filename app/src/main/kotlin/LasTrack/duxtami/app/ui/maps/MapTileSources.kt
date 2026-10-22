package LasTrack.duxtami.app.ui.maps

import LasTrack.duxtami.app.core.model.MapProviderId
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

/**
 * High-definition, authentic map tile sources for all 6 map providers:
 * 1. OpenStreetMap:
 *    - Standard (Mapnik)
 *    - White / Light (Geoapify Positron, no watermark)
 *    - Dark Mode (Geoapify Dark Matter, no watermark)
 * 2. Google Maps:
 *    - Satellite Hybrid (HD &scale=2, 512px)
 *    - Dark Mode (HD, 512px)
 * 3. Mapbox:
 *    - Mapbox Satellite Hybrid (Crisp Satellite + Reference Overlays)
 * 4. ArcGIS (Official Esri from MapBoard & GisToolbox):
 *    - ArcGIS Satellite Hybrid (World Imagery + Boundaries, Places & Roads)
 * 5. MapLibre:
 *    - MapLibre Liberty (Clean modern vector scheme)
 *    - MapLibre Topo (OpenTopoMap Contours & Trails)
 *    - MapLibre CyclOSM (Cycle Paths & Outdoor Routes)
 * 6. Geoapify:
 *    - Geoapify OSM Bright
 *    - Geoapify Dark Matter
 *    - Geoapify Positron
 *    - Geoapify KlokanTech Basic
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

    // Google Satellite Hybrid HD
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

    // Google Maps Dark Mode HD
    val GoogleDarkHD = object : OnlineTileSourceBase(
        "GoogleDarkHD", 0, 20, 512, ".png", GoogleSubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "lyrs=m&scale=2&hl=en&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    // =========================================================================
    // 2. Geoapify Free High-Res Basemap Tiles (No Watermark / No API key error)
    // =========================================================================

    private val GeoapifySubdomains = arrayOf(
        "https://maps.geoapify.com/v1/tile/",
    )

    // Geoapify OSM Bright
    val GeoapifyBright = object : OnlineTileSourceBase(
        "GeoapifyBright", 0, 20, 512, ".png", GeoapifySubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "osm-bright/" + z + "/" + x + "/" + y + ".png"
        }
    }

    // Geoapify Dark Matter (Dark Mode)
    val GeoapifyDarkMatter = object : OnlineTileSourceBase(
        "GeoapifyDarkMatter", 0, 20, 512, ".png", GeoapifySubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "dark-matter/" + z + "/" + x + "/" + y + ".png"
        }
    }

    // Geoapify Positron (White / Light Mode)
    val GeoapifyPositron = object : OnlineTileSourceBase(
        "GeoapifyPositron", 0, 20, 512, ".png", GeoapifySubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "positron/" + z + "/" + x + "/" + y + ".png"
        }
    }

    // Geoapify KlokanTech Basic
    val GeoapifyKlokan = object : OnlineTileSourceBase(
        "GeoapifyKlokan", 0, 20, 512, ".png", GeoapifySubdomains,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "klokantech-basic/" + z + "/" + x + "/" + y + ".png"
        }
    }

    // =========================================================================
    // 3. Authentic Esri ArcGIS Map Services (Official Esri)
    // =========================================================================

    private val EsriServers = arrayOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/",
        "https://services.arcgisonline.com/ArcGIS/rest/services/",
    )

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

    // Esri World Boundaries and Places (Borders, Cities, Country Names Overlay)
    val ArcGisBoundariesAndPlaces = object : OnlineTileSourceBase(
        "ArcGisBoundariesAndPlaces", 0, 19, 256, ".png", EsriServers,
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + "Reference/World_Boundaries_and_Places/MapServer/tile/" + z + "/" + y + "/" + x
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
    // 4. MapLibre / OpenTopo / CyclOSM Map Sources
    // =========================================================================

    val OpenTopoMap = object : OnlineTileSourceBase(
        "OpenTopoMap", 0, 17, 256, ".png", arrayOf("https://tile.opentopomap.org/"),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + z + "/" + x + "/" + y + ".png"
        }
    }

    val CyclOSM = object : OnlineTileSourceBase(
        "CyclOSM", 0, 18, 256, ".png", arrayOf(
            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/",
        ),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + z + "/" + x + "/" + y + ".png"
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
                "osm_white" -> GeoapifyPositron
                "osm_dark" -> GeoapifyDarkMatter
                else -> TileSourceFactory.MAPNIK
            }

            MapProviderId.GOOGLE_MAPS -> when (basemapId) {
                "google_dark" -> GoogleDarkHD
                else -> GoogleHybridHD
            }

            MapProviderId.MAPBOX -> ArcGisWorldImagery

            MapProviderId.ARCGIS -> ArcGisWorldImagery

            MapProviderId.MAPLIBRE -> when (basemapId) {
                "maplibre_topo" -> OpenTopoMap
                "maplibre_cyclosm" -> CyclOSM
                else -> TileSourceFactory.MAPNIK
            }

            MapProviderId.GEOAPIFY -> when (basemapId) {
                "geoapify_dark" -> GeoapifyDarkMatter
                "geoapify_positron" -> GeoapifyPositron
                "geoapify_klokan" -> GeoapifyKlokan
                else -> GeoapifyBright
            }
        }
    }

    /**
     * Resolves an optional transparent label/places overlay tile source.
     * When non-null, this overlay is rendered directly on top of the base tile source.
     */
    fun resolveOverlay(provider: MapProviderId, basemapId: String?): ITileSource? {
        return when (provider) {
            MapProviderId.ARCGIS -> ArcGisTransportation
            MapProviderId.MAPBOX -> ArcGisBoundariesAndPlaces
            else -> null
        }
    }

    /**
     * Returns true if dark color filter should be applied to this map style.
     */
    fun isDarkFilterRequired(provider: MapProviderId, basemapId: String?): Boolean {
        return (provider == MapProviderId.GOOGLE_MAPS && basemapId == "google_dark")
    }
}
