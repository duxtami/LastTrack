package LastTrack.duxtami.app.core.model

data class RouteMetadata(
    val description: String? = null,
)

/** Coordinates only (no bookkeeping); what the storage codec persists and what map providers will consume. */
class RouteGeometry(
    val segments: List<GeoSegment>,
    val waypoints: List<GeoWaypoint>,
) {
    val pointCount: Int get() = segments.sumOf { it.size }
}

/**
 * Provider-independent route model. GPX, KML and KMZ are converted into this by parsers; map providers consume it.
 * Nothing here references a file format's structure or any map SDK.
 */
class GeoRoute(
    val id: String,
    val name: String,
    /** Name of the file the route was imported from, if known. */
    val sourceFile: String?,
    val sourceFormat: SourceFormat,
    val segments: List<GeoSegment>,
    val waypoints: List<GeoWaypoint>,
    val metadata: RouteMetadata,
    val stats: RouteStats,
    /** Epoch millis of import. */
    val createdAt: Long,
) {
    val pointCount: Int get() = segments.sumOf { it.size }
    val distanceMeters: Double get() = stats.distanceMeters
    val durationMillis: Long? get() = stats.durationMillis
    val elevationGainMeters: Double? get() = stats.elevationGainMeters
    val elevationLossMeters: Double? get() = stats.elevationLossMeters

    fun points(): Sequence<GeoPoint> = segments.asSequence().flatMap { it.points() }
}
