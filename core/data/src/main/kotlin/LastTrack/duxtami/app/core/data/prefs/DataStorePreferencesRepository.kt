package LastTrack.duxtami.app.core.data.prefs

import LastTrack.duxtami.app.core.model.AppPreferences
import LastTrack.duxtami.app.core.model.MapProviderId
import LastTrack.duxtami.app.core.model.PreferencesRepository
import LastTrack.duxtami.app.core.model.RouteLineStyle
import LastTrack.duxtami.app.core.model.RouteLineWidth
import LastTrack.duxtami.app.core.model.ThemeMode
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lasttrack_preferences")

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreferencesRepository {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val amoled = booleanPreferencesKey("amoled")
        val mapProvider = stringPreferencesKey("map_provider")
        val basemap = stringPreferencesKey("basemap")
        val autoFit = booleanPreferencesKey("auto_fit")
        val startEnd = booleanPreferencesKey("start_end_markers")
        val waypoints = booleanPreferencesKey("show_waypoints")
        val compass = booleanPreferencesKey("show_compass")
        val scale = booleanPreferencesKey("show_scale")
        val lineWidth = stringPreferencesKey("route_line_width")
        val lineStyle = stringPreferencesKey("route_line_style")
        val autoOpen = booleanPreferencesKey("auto_open_imported")
    }

    override val preferences: Flow<AppPreferences> = context.dataStore.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { it.toAppPreferences() }

    override suspend fun update(transform: (AppPreferences) -> AppPreferences) {
        context.dataStore.edit { prefs ->
            val next = transform(prefs.toAppPreferences())
            prefs[Keys.theme] = next.themeMode.name
            prefs[Keys.dynamicColor] = next.dynamicColor
            prefs[Keys.amoled] = next.amoled
            prefs[Keys.mapProvider] = next.mapProvider.name
            prefs[Keys.basemap] = next.basemapId
            prefs[Keys.autoFit] = next.autoFitRoute
            prefs[Keys.startEnd] = next.showStartEndMarkers
            prefs[Keys.waypoints] = next.showWaypoints
            prefs[Keys.compass] = next.showCompass
            prefs[Keys.scale] = next.showScale
            prefs[Keys.lineWidth] = next.routeLineWidth.name
            prefs[Keys.lineStyle] = next.routeLineStyle.name
            prefs[Keys.autoOpen] = next.autoOpenImported
        }
    }

    private fun Preferences.toAppPreferences(): AppPreferences {
        val d = AppPreferences()
        return AppPreferences(
            themeMode = enumOrDefault(this[Keys.theme], d.themeMode),
            dynamicColor = this[Keys.dynamicColor] ?: d.dynamicColor,
            amoled = this[Keys.amoled] ?: d.amoled,
            mapProvider = enumOrDefault(this[Keys.mapProvider], d.mapProvider),
            basemapId = this[Keys.basemap] ?: d.basemapId,
            autoFitRoute = this[Keys.autoFit] ?: d.autoFitRoute,
            showStartEndMarkers = this[Keys.startEnd] ?: d.showStartEndMarkers,
            showWaypoints = this[Keys.waypoints] ?: d.showWaypoints,
            showCompass = this[Keys.compass] ?: d.showCompass,
            showScale = this[Keys.scale] ?: d.showScale,
            routeLineWidth = enumOrDefault(this[Keys.lineWidth], d.routeLineWidth),
            routeLineStyle = enumOrDefault(this[Keys.lineStyle], d.routeLineStyle),
            autoOpenImported = this[Keys.autoOpen] ?: d.autoOpenImported,
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
