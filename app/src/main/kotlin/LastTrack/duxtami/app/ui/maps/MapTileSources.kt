package LastTrack.duxtami.app.ui.maps

import LastTrack.duxtami.app.core.model.MapProviderId
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

/**
 * Provides live, high-performance, crystal-clear map tiles for all map providers:
 * Google Maps (High-Res 2x), Mapbox, ArcGIS, and OpenStreetMap.
 */
object MapTileSources {

    // --- High-Resolution Google Maps Tile Sources (&scale=2 for sharp retina display) ---
    private val GoogleRoadmapHD = object : OnlineTileSourceBase(
        "GoogleRoadmapHD", 0, 20, 256, ".png",
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
            return baseUrl + "lyrs=m&scale=2&hl=en&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    private val GoogleSatelliteHD = object : OnlineTileSourceBase(
        "GoogleSatelliteHD", 0, 20, 256, ".jpg",
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
            return baseUrl + "lyrs=s&scale=2&x=" + x + "&y=" + y + "&z=" + z
        }
    }

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

    private val GoogleTerrainHD = object : OnlineTileSourceBase(
        "GoogleTerrainHD", 0, 20, 256, ".png",
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
            return baseUrl + "lyrs=p&scale=2&hl=en&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    // --- Mapbox Tile Sources (Rich place names & high-detail POIs) ---
    private val MapboxStreets = TileSourceFactory.MAPNIK

    private val MapboxLight = XYTileSource(
        "MapboxLight", 0, 20, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/light_all/",
            "https://b.basemaps.cartocdn.com/light_all/",
            "https://c.basemaps.cartocdn.com/light_all/",
        ),
    )

    private val MapboxDark = XYTileSource(
        "MapboxDark", 0, 20, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/",
        ),
    )

    // --- ArcGIS Tile Sources ---
    private val ArcGisStreets = object : OnlineTileSourceBase(
        "ArcGisStreets", 0, 19, 256, ".jpg",
        arrayOf("https://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + z + "/" + y + "/" + x
        }
    }

    private val ArcGisTopographic = object : OnlineTileSourceBase(
        "ArcGisTopo", 0, 19, 256, ".jpg",
        arrayOf("https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/"),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + z + "/" + y + "/" + x
        }
    }

    private val ArcGisNatGeo = object : OnlineTileSourceBase(
        "ArcGisNatGeo", 0, 16, 256, ".jpg",
        arrayOf("https://services.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/"),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return baseUrl + z + "/" + y + "/" + x
        }
    }

    /**
     * Resolves the appropriate tile source for the given provider and basemap style.
     */
    fun resolve(provider: MapProviderId, basemapId: String?): ITileSource {
        return when (provider) {
            MapProviderId.GOOGLE_MAPS -> when (basemapId) {
                "satellite" -> GoogleSatelliteHD
                "hybrid" -> GoogleHybridHD
                "terrain" -> GoogleTerrainHD
                else -> GoogleRoadmapHD
            }
            MapProviderId.MAPBOX -> when (basemapId) {
                "outdoors" -> TileSourceFactory.OpenTopo
                "light" -> MapboxLight
                "dark" -> MapboxDark
                "satellite" -> GoogleHybridHD
                else -> MapboxStreets
            }
            MapProviderId.ARCGIS -> when (basemapId) {
                "imagery" -> GoogleHybridHD
                "topographic" -> ArcGisTopographic
                "natgeo" -> ArcGisNatGeo
                else -> ArcGisStreets
            }
            MapProviderId.OPEN_STREET_MAP -> when (basemapId) {
                "opentopo" -> TileSourceFactory.OpenTopo
                "usgs_topo" -> TileSourceFactory.USGS_TOPO
                else -> TileSourceFactory.MAPNIK
            }
        }
    }
}
