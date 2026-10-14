package LastTrack.duxtami.app.ui.maps

import LastTrack.duxtami.app.core.model.MapProviderId
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

/**
 * Provides live, high-performance map tiles for all map providers:
 * Google Maps, Mapbox, ArcGIS, and OpenStreetMap.
 */
object MapTileSources {

    // --- Google Maps Tile Sources ---
    private val GoogleRoadmap = object : OnlineTileSourceBase(
        "GoogleRoadmap", 0, 20, 256, ".png",
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
            return baseUrl + "lyrs=m&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    private val GoogleSatellite = object : OnlineTileSourceBase(
        "GoogleSatellite", 0, 20, 256, ".jpg",
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
            return baseUrl + "lyrs=s&x=" + x + "&y=" + y + "&z=" + z
        }
    }

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
            return baseUrl + "lyrs=y&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    private val GoogleTerrain = object : OnlineTileSourceBase(
        "GoogleTerrain", 0, 20, 256, ".png",
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
            return baseUrl + "lyrs=p&x=" + x + "&y=" + y + "&z=" + z
        }
    }

    // --- Mapbox Tile Sources ---
    private val MapboxStreets = XYTileSource(
        "MapboxStreets", 0, 20, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        ),
    )

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

    /**
     * Resolves the appropriate tile source for the given provider and basemap style.
     */
    fun resolve(provider: MapProviderId, basemapId: String?): ITileSource {
        return when (provider) {
            MapProviderId.GOOGLE_MAPS -> when (basemapId) {
                "satellite" -> GoogleSatellite
                "hybrid" -> GoogleHybrid
                "terrain" -> GoogleTerrain
                else -> GoogleRoadmap
            }
            MapProviderId.MAPBOX -> when (basemapId) {
                "outdoors" -> TileSourceFactory.OpenTopo
                "light" -> MapboxLight
                "dark" -> MapboxDark
                "satellite" -> ArcGisImagery
                else -> MapboxStreets
            }
            MapProviderId.ARCGIS -> when (basemapId) {
                "imagery" -> ArcGisImagery
                "topographic" -> ArcGisTopographic
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
