package LasTrack.duxtami.app.core.model

import java.util.regex.Pattern

object CoordinateParser {

    data class ParsedCoordinates(
        val latitude: Double,
        val longitude: Double,
        val suggestedName: String? = null,
    )

    private val DECIMAL_REGEX = Pattern.compile(
        """(-?\d{1,2}(?:\.\d+)?)[,\s]+(-?\d{1,3}(?:\.\d+)?)"""
    )

    private val GOOGLE_MAPS_AT_REGEX = Pattern.compile(
        """@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)"""
    )

    private val QUERY_PARAM_REGEX = Pattern.compile(
        """[?&](?:q|query|ll)=(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)"""
    )

    private val GEO_URI_REGEX = Pattern.compile(
        """geo:(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)"""
    )

    private val DMS_REGEX = Pattern.compile(
        """(\d{1,2})[°\s]+(\d{1,2})['\s]+(\d{1,2}(?:\.\d+)?)["\s]*([NSns])[,\s]+(\d{1,3})[°\s]+(\d{1,2})['\s]+(\d{1,2}(?:\.\d+)?)["\s]*([EWew])"""
    )

    private val URL_REGEX = Pattern.compile("""https?://[^\s]+""")

    fun parse(input: String): ParsedCoordinates? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // If input contains a URL mixed with text, extract them
        val urlMatcher = URL_REGEX.matcher(trimmed)
        val extractedUrl = if (urlMatcher.find()) urlMatcher.group() else null
        val surroundingText = if (extractedUrl != null) {
            trimmed.replace(extractedUrl, "").trim().lines().firstOrNull()?.trim()?.ifEmpty { null }
        } else null

        val toParse = extractedUrl ?: trimmed

        // 1. Try Google Maps @lat,lng
        val atMatcher = GOOGLE_MAPS_AT_REGEX.matcher(toParse)
        if (atMatcher.find()) {
            val lat = atMatcher.group(1)?.toDoubleOrNull()
            val lon = atMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, extractNameFromUrl(toParse) ?: surroundingText)
            }
        }

        // 2. Try URL query params q=lat,lng
        val queryMatcher = QUERY_PARAM_REGEX.matcher(toParse)
        if (queryMatcher.find()) {
            val lat = queryMatcher.group(1)?.toDoubleOrNull()
            val lon = queryMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, extractNameFromUrl(toParse) ?: surroundingText)
            }
        }

        // 3. Try geo: URI
        val geoMatcher = GEO_URI_REGEX.matcher(toParse)
        if (geoMatcher.find()) {
            val lat = geoMatcher.group(1)?.toDoubleOrNull()
            val lon = geoMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, surroundingText)
            }
        }

        // 4. Try DMS
        val dmsMatcher = DMS_REGEX.matcher(toParse)
        if (dmsMatcher.find()) {
            val latDeg = dmsMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            val latMin = dmsMatcher.group(2)?.toDoubleOrNull() ?: 0.0
            val latSec = dmsMatcher.group(3)?.toDoubleOrNull() ?: 0.0
            val latHem = dmsMatcher.group(4) ?: "N"

            val lonDeg = dmsMatcher.group(5)?.toDoubleOrNull() ?: 0.0
            val lonMin = dmsMatcher.group(6)?.toDoubleOrNull() ?: 0.0
            val lonSec = dmsMatcher.group(7)?.toDoubleOrNull() ?: 0.0
            val lonHem = dmsMatcher.group(8) ?: "E"

            var lat = latDeg + (latMin / 60.0) + (latSec / 3600.0)
            if (latHem.equals("S", ignoreCase = true)) lat = -lat

            var lon = lonDeg + (lonMin / 60.0) + (lonSec / 3600.0)
            if (lonHem.equals("W", ignoreCase = true)) lon = -lon

            if (isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, surroundingText)
            }
        }

        // 5. Try Decimal regex
        val decMatcher = DECIMAL_REGEX.matcher(toParse)
        if (decMatcher.find()) {
            val lat = decMatcher.group(1)?.toDoubleOrNull()
            val lon = decMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, surroundingText)
            }
        }

        return null
    }

    /**
     * Resolves short URLs (like https://maps.app.goo.gl/... or https://goo.gl/maps/...)
     * by following HTTP redirects in background and extracting coordinates and place title.
     */
    fun resolveUrl(url: String): String {
        var current = url
        for (i in 0..4) {
            try {
                val conn = (java.net.URI(current).toURL().openConnection() as java.net.HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!loc.isNullOrBlank()) {
                        current = loc
                        continue
                    }
                }
                conn.disconnect()
                break
            } catch (_: Exception) {
                break
            }
        }
        return current
    }

    suspend fun resolveAndParse(input: String): ParsedCoordinates? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val direct = parse(input)
        if (direct != null) return@withContext direct

        val urlMatcher = URL_REGEX.matcher(input)
        if (urlMatcher.find()) {
            val url = urlMatcher.group()
            val surroundingText = input.replace(url, "").trim().lines().firstOrNull()?.trim()?.ifEmpty { null }
            val resolvedUrl = resolveUrl(url)
            val parsed = parse(resolvedUrl)
            if (parsed != null) {
                return@withContext if (parsed.suggestedName.isNullOrBlank() && !surroundingText.isNullOrBlank()) {
                    parsed.copy(suggestedName = surroundingText)
                } else {
                    parsed
                }
            }
        }
        null
    }

    private fun isValid(lat: Double, lon: Double): Boolean =
        lat in -90.0..90.0 && lon in -180.0..180.0

    private fun extractNameFromUrl(url: String): String? {
        val placeMatch = Pattern.compile("""/place/([^/@?]+)""").matcher(url)
        if (placeMatch.find()) {
            val raw = placeMatch.group(1) ?: return null
            return raw.replace('+', ' ').replace("%20", " ").trim().ifEmpty { null }
        }
        return null
    }
}
