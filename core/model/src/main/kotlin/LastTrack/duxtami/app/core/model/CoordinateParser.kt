package LastTrack.duxtami.app.core.model

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

    fun parse(input: String): ParsedCoordinates? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // 1. Try Google Maps @lat,lng
        val atMatcher = GOOGLE_MAPS_AT_REGEX.matcher(trimmed)
        if (atMatcher.find()) {
            val lat = atMatcher.group(1)?.toDoubleOrNull()
            val lon = atMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, extractNameFromUrl(trimmed))
            }
        }

        // 2. Try URL query params q=lat,lng
        val queryMatcher = QUERY_PARAM_REGEX.matcher(trimmed)
        if (queryMatcher.find()) {
            val lat = queryMatcher.group(1)?.toDoubleOrNull()
            val lon = queryMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon, extractNameFromUrl(trimmed))
            }
        }

        // 3. Try geo: URI
        val geoMatcher = GEO_URI_REGEX.matcher(trimmed)
        if (geoMatcher.find()) {
            val lat = geoMatcher.group(1)?.toDoubleOrNull()
            val lon = geoMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon)
            }
        }

        // 4. Try DMS
        val dmsMatcher = DMS_REGEX.matcher(trimmed)
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
                return ParsedCoordinates(lat, lon)
            }
        }

        // 5. Try Decimal regex
        val decMatcher = DECIMAL_REGEX.matcher(trimmed)
        if (decMatcher.find()) {
            val lat = decMatcher.group(1)?.toDoubleOrNull()
            val lon = decMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lon != null && isValid(lat, lon)) {
                return ParsedCoordinates(lat, lon)
            }
        }

        return null
    }

    private fun isValid(lat: Double, lon: Double): Boolean =
        lat in -90.0..90.0 && lon in -180.0..180.0

    private fun extractNameFromUrl(url: String): String? {
        val placeMatch = Pattern.compile("""/place/([^/@]+)""").matcher(url)
        if (placeMatch.find()) {
            val raw = placeMatch.group(1) ?: return null
            return raw.replace('+', ' ').replace("%20", " ").trim().ifEmpty { null }
        }
        return null
    }
}
