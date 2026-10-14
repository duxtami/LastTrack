package LasTrack.duxtami.app.ui.maps

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Locale

data class PlaceSearchResult(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
)

data class PlaceDetails(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

object PlaceSearchManager {

    private const val USER_AGENT = "LasTrack/0.1.0 (Android; LasTrack.duxtami.app)"

    suspend fun search(query: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&addressdetails=1&limit=6"
            val conn = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Language", "en")
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext emptyList()
            }
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val array = JSONArray(response)
            val list = mutableListOf<PlaceSearchResult>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val lat = obj.optDouble("lat", Double.NaN)
                val lon = obj.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue

                val displayName = obj.optString("display_name", "")
                val parts = displayName.split(", ")
                val title = obj.optString("name").ifBlank { parts.firstOrNull() ?: displayName }
                val subtitle = if (parts.size > 1) parts.drop(1).joinToString(", ") else displayName

                list.add(
                    PlaceSearchResult(
                        title = title,
                        subtitle = subtitle,
                        latitude = lat,
                        longitude = lon,
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): PlaceDetails = withContext(Dispatchers.IO) {
        val fallback = PlaceDetails(
            name = String.format(Locale.US, "Location (%.4f, %.4f)", latitude, longitude),
            address = String.format(Locale.US, "%.5f, %.5f", latitude, longitude),
            latitude = latitude,
            longitude = longitude,
        )
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&addressdetails=1"
            val conn = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Language", "en")
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext fallback
            }
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val obj = JSONObject(response)
            val displayName = obj.optString("display_name", "")
            val name = obj.optString("name").ifBlank {
                val addr = obj.optJSONObject("address")
                addr?.optString("road")?.ifBlank { null }
                    ?: addr?.optString("suburb")?.ifBlank { null }
                    ?: addr?.optString("city")?.ifBlank { null }
                    ?: displayName.split(", ").firstOrNull()
                    ?: fallback.name
            }
            PlaceDetails(
                name = name,
                address = displayName.ifBlank { fallback.address },
                latitude = latitude,
                longitude = longitude,
            )
        } catch (_: Exception) {
            fallback
        }
    }
}
