package LasTrack.duxtami.app.core.model

/**
 * Statistics derived from route data. A null value means "not computable from this file" and must be hidden
 * by the UI rather than displayed as zero.
 */
data class RouteStats(
    val distanceMeters: Double,
    val durationMillis: Long?,
    val elevationGainMeters: Double?,
    val elevationLossMeters: Double?,
    val maxElevationMeters: Double?,
    val minElevationMeters: Double?,
    val averageSpeedMetersPerSecond: Double?,
    val pointCount: Int,
    val waypointCount: Int,
    val segmentCount: Int,
) {
    val hasElevation: Boolean
        get() = maxElevationMeters != null || elevationGainMeters != null
}
