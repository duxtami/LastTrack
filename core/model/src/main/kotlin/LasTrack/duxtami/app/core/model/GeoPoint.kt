package LasTrack.duxtami.app.core.model

/**
 * A single coordinate. Convenience view only: bulk data lives in [GeoSegment]'s primitive arrays so that
 * 100k+ point routes never allocate one object per point unless a caller explicitly asks for it.
 *
 * @property elevation metres above sea level, or null when the source did not provide it.
 * @property timestamp epoch milliseconds (UTC), or null when the source did not provide it.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val timestamp: Long? = null,
)

data class GeoWaypoint(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val description: String? = null,
    val elevation: Double? = null,
)
