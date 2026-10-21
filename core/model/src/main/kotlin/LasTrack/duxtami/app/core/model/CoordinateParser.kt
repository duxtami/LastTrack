package LasTrack.duxtami.app.core.model

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
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

    private val GOOGLE_MAPS_3D4D_REGEX = Pattern.compile(
        """!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)"""
    )

    private val QUERY_PARAM_REGEX = Pattern.compile(
        """[?&](?:q|query|ll|sll|daddr|destination|center)=(-?\d{1,2}(?:\.\d+)?)(?:,|%2C|\+)+(-?\d{1,3}(?:\.\d+)?)"""
    )

    private val GEO_URI_REGEX = Pattern.compile(
        """geo:(-?\d{1,2}(?:\.\d+)?),(-?\d{1,3}(?:\.\d+)?)"""
    )

    private val DMS_REGEX = Pattern.compile(
        """(\d{1,2})[°\s]+(\d{1,2})['\s]+(\d{1,2}(?:\.\d+)?)["\s]*([NSns])[,\s]+(\d{1,3})[°\s]+(\d{1,2})['\s]+(\d{1,2}(?:\.\d+)?)["\s]*([EWew])"""
    )

    private val URL_REGEX = Pattern.compile("""https?://[^\s<>"']+""")

    fun parse(input: String): ParsedCoordinates? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // If input contains a URL mixed with text, extract them
        val urlMatcher = URL_REGEX.matcher(trimmed)
        val extractedUrl = if (urlMatcher.find()) urlMatcher.group() else null
        val surroundingText = if (extractedUrl != null) {
            val textWithoutUrl = trimmed.replace(extractedUrl, "").trim()
            textWithoutUrl.lines().firstOrNull()?.trim()?.ifEmpty { null }
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

        // 2. Try Google Maps !3dlat!4dlng (standard in Google Maps URLs)
        val g3dMatcher = GOOGLE_MAPS_3D4D_REGEX.matcher(toParse)
        if (g3dMatcher.find()) {
            val lat = g3dMatcher.group(1)?.toDoubleOrNull()
            val lon = g3dMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, extractNameFromUrl(toParse) ?: surroundingText)
            }
        }

        // 3. Try URL query params q=lat,lng / query / ll / daddr / center
        val queryMatcher = QUERY_PARAM_REGEX.matcher(toParse)
        if (queryMatcher.find()) {
            val lat = queryMatcher.group(1)?.toDoubleOrNull()
            val lon = queryMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, extractNameFromUrl(toParse) ?: surroundingText)
            }
        }

        // 4. Try geo: URI
        val geoMatcher = GEO_URI_REGEX.matcher(toParse)
        if (geoMatcher.find()) {
            val lat = geoMatcher.group(1)?.toDoubleOrNull()
            val lon = geoMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, surroundingText)
            }
        }

        // 5. Try DMS
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

        // 6. Try Decimal regex anywhere in text
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
     * by following HTTP redirects in background and inspecting response body if HTML is returned.
     */
    fun resolveUrl(url: String): Pair<String, String?> {
        var current = url
        var extractedTitle: String? = null

        for (i in 0..5) {
            try {
                val conn = (URI(current).toURL().openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!loc.isNullOrBlank()) {
                        current = if (loc.startsWith("http://") || loc.startsWith("https://")) {
                            loc
                        } else {
                            URI(current).resolve(loc).toString()
                        }
                        continue
                    }
                } else if (code == 200) {
                    // Check HTML content for redirect meta or og:url / og:image
                    val bodySnippet = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                        val buffer = CharArray(32768)
                        val read = reader.read(buffer)
                        if (read > 0) String(buffer, 0, read) else ""
                    }
                    conn.disconnect()

                    // Try meta refresh
                    val refreshMatch = Pattern.compile("""<meta[^>]*?http-equiv=["']refresh["'][^>]*?content=["'][^"']*?url=([^"'>\s]+)""", Pattern.CASE_INSENSITIVE).matcher(bodySnippet)
                    if (refreshMatch.find()) {
                        val newUrl = refreshMatch.group(1)
                        if (!newUrl.isNullOrBlank()) {
                            current = newUrl
                            continue
                        }
                    }

                    // Try og:url
                    val ogUrlMatch = Pattern.compile("""<meta[^>]*?property=["']og:url["'][^>]*?content=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(bodySnippet)
                    if (ogUrlMatch.find()) {
                        val ogUrl = ogUrlMatch.group(1)
                        if (!ogUrl.isNullOrBlank() && (ogUrl.contains("@") || ogUrl.contains("!3d") || ogUrl.contains("q="))) {
                            current = ogUrl
                        }
                    }

                    // Try canonical link
                    val canonMatch = Pattern.compile("""<link[^>]*?rel=["']canonical["'][^>]*?href=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(bodySnippet)
                    if (canonMatch.find()) {
                        val canonUrl = canonMatch.group(1)
                        if (!canonUrl.isNullOrBlank() && (canonUrl.contains("@") || canonUrl.contains("!3d") || canonUrl.contains("q="))) {
                            current = canonUrl
                        }
                    }

                    // Extract title from <title> if available
                    val titleMatch = Pattern.compile("""<title>(.*?)</title>""", Pattern.CASE_INSENSITIVE).matcher(bodySnippet)
                    if (titleMatch.find()) {
                        val rawTitle = titleMatch.group(1).orEmpty()
                            .replace(" - Google Maps", "")
                            .replace(" · Google Maps", "")
                            .trim()
                        if (rawTitle.isNotBlank()) {
                            extractedTitle = rawTitle
                        }
                    }

                    // Check if coordinates exist directly in the body (e.g. og:image static map center)
                    val centerMatch = Pattern.compile("""center=(-?\d{1,2}(?:\.\d+)?)(?:%2C|,)(-?\d{1,3}(?:\.\d+)?)""").matcher(bodySnippet)
                    if (centerMatch.find()) {
                        val lat = centerMatch.group(1)
                        val lon = centerMatch.group(2)
                        current = "$current?center=$lat,$lon"
                    }

                    break
                }
                conn.disconnect()
                break
            } catch (_: Exception) {
                break
            }
        }
        return current to extractedTitle
    }

    suspend fun resolveAndParse(input: String): ParsedCoordinates? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val direct = parse(input)
        if (direct != null) return@withContext direct

        val urlMatcher = URL_REGEX.matcher(input)
        if (urlMatcher.find()) {
            val url = urlMatcher.group()
            val surroundingText = input.replace(url, "").trim().lines().firstOrNull()?.trim()?.ifEmpty { null }
            val (resolvedUrl, extractedTitle) = resolveUrl(url)
            val parsed = parse(resolvedUrl)
            if (parsed != null) {
                val candidateName = parsed.suggestedName ?: extractedTitle ?: surroundingText
                return@withContext parsed.copy(suggestedName = candidateName)
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
            return runCatching { URLDecoder.decode(raw.replace('+', ' '), "UTF-8") }.getOrDefault(raw).trim().ifEmpty { null }
        }
        return null
    }
}
