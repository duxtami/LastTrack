package LasTrack.duxtami.app.ui.settings.backup

import LasTrack.duxtami.app.core.model.PlaceCategory
import LasTrack.duxtami.app.core.model.PreferencesRepository
import LasTrack.duxtami.app.core.model.RouteRepository
import LasTrack.duxtami.app.core.model.SavedPlace
import LasTrack.duxtami.app.core.model.SavedPlacesRepository
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class BackupResult(
    val placesCount: Int,
    val routesCount: Int,
    val jsonString: String,
    val fileName: String,
)

data class RestoreResult(
    val placesRestored: Int,
    val message: String,
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val routeRepository: RouteRepository,
    private val preferencesRepository: PreferencesRepository,
) {

    suspend fun createBackupJson(): BackupResult = withContext(Dispatchers.IO) {
        val places = savedPlacesRepository.observeAll().first()
        val routes = routeRepository.observeRoutes().first()

        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "LasTrack")
        root.put("timestamp", System.currentTimeMillis())

        val placesArray = JSONArray()
        places.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("latitude", p.latitude)
            obj.put("longitude", p.longitude)
            obj.put("category", p.category.name)
            obj.put("customLabel", p.customLabel ?: "")
            obj.put("description", p.description ?: "")
            obj.put("createdAt", p.createdAt)
            obj.put("iconKey", p.iconKey ?: "")
            placesArray.put(obj)
        }
        root.put("savedPlaces", placesArray)

        val routesArray = JSONArray()
        routes.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("fileName", r.fileName)
            obj.put("format", r.format.name)
            obj.put("description", r.description ?: "")
            routesArray.put(obj)
        }
        root.put("routes", routesArray)

        val timeFormatted = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "LasTrack_Backup_$timeFormatted.json"

        BackupResult(
            placesCount = places.size,
            routesCount = routes.size,
            jsonString = root.toString(2),
            fileName = fileName,
        )
    }

    suspend fun restoreFromJsonUri(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val jsonText = context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        } ?: return@withContext RestoreResult(0, "Unable to read backup file")

        val root = runCatching { JSONObject(jsonText) }.getOrNull()
            ?: return@withContext RestoreResult(0, "Invalid backup file format")

        var restoredPlaces = 0
        if (root.has("savedPlaces")) {
            val placesArray = root.getJSONArray("savedPlaces")
            for (i in 0 until placesArray.length()) {
                val obj = placesArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Restored Place")
                val lat = obj.optDouble("latitude", Double.NaN)
                val lon = obj.optDouble("longitude", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue

                val categoryStr = obj.optString("category", "CUSTOM")
                val category = PlaceCategory.fromString(categoryStr)
                val customLabel = obj.optString("customLabel").ifEmpty { null }
                val description = obj.optString("description").ifEmpty { null }
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val iconKey = obj.optString("iconKey").ifEmpty { null }

                val place = SavedPlace(
                    id = id,
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    category = category,
                    customLabel = customLabel,
                    description = description,
                    createdAt = createdAt,
                    iconKey = iconKey,
                )
                savedPlacesRepository.savePlace(place)
                restoredPlaces++
            }
        }

        RestoreResult(
            placesRestored = restoredPlaces,
            message = "Successfully restored $restoredPlaces saved places",
        )
    }
}
