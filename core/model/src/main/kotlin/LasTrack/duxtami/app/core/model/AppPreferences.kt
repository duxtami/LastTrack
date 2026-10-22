package LasTrack.duxtami.app.core.model

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class MapProviderId { OPEN_STREET_MAP, GOOGLE_MAPS, MAPBOX, ARCGIS, MAPLIBRE, GEOAPIFY }

enum class RouteLineStyle { SOLID, DASHED }

enum class RouteLineWidth(val dp: Float) { THIN(3f), MEDIUM(5f), THICK(8f) }

const val DEFAULT_BASEMAP_ID = "default"

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
    val mapProvider: MapProviderId = MapProviderId.OPEN_STREET_MAP,
    val basemapId: String = DEFAULT_BASEMAP_ID,
    val autoFitRoute: Boolean = true,
    val showStartEndMarkers: Boolean = true,
    val showWaypoints: Boolean = true,
    val showCompass: Boolean = true,
    val showScale: Boolean = true,
    val routeLineWidth: RouteLineWidth = RouteLineWidth.MEDIUM,
    val routeLineStyle: RouteLineStyle = RouteLineStyle.SOLID,
    val autoOpenImported: Boolean = true,
)

interface PreferencesRepository {
    val preferences: Flow<AppPreferences>

    /** Atomically reads the current preferences, applies [transform] and persists the result. */
    suspend fun update(transform: (AppPreferences) -> AppPreferences)
}
