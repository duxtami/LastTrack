package LastTrack.duxtami.app.core.model

enum class SegmentKind {
    /** A recorded path (GPX trkseg, KML LineString / gx:Track). */
    TRACK,

    /** A planned path (GPX rte). */
    ROUTE,
}

/**
 * An ordered run of coordinates stored in parallel primitive arrays (structure-of-arrays).
 *
 * Immutable by convention: the arrays are private and never exposed, so a segment can be shared freely
 * between the parser, the statistics calculator, the storage codec and (later) the map providers without
 * defensive copies.
 */
class GeoSegment(
    val name: String?,
    val kind: SegmentKind,
    private val latitudes: DoubleArray,
    private val longitudes: DoubleArray,
    private val elevations: DoubleArray? = null,
    private val timestamps: LongArray? = null,
) {
    init {
        require(latitudes.size == longitudes.size) { "latitude/longitude size mismatch" }
        require(elevations == null || elevations.size == latitudes.size) { "elevation size mismatch" }
        require(timestamps == null || timestamps.size == latitudes.size) { "timestamp size mismatch" }
    }

    val size: Int get() = latitudes.size
    val isEmpty: Boolean get() = latitudes.isEmpty()

    /** True when at least one point carries an elevation. */
    val hasElevation: Boolean get() = elevations != null

    /** True when at least one point carries a timestamp. */
    val hasTimestamps: Boolean get() = timestamps != null

    fun latitude(index: Int): Double = latitudes[index]
    fun longitude(index: Int): Double = longitudes[index]

    /** Elevation in metres, or [Double.NaN] when unknown. */
    fun elevationOrNaN(index: Int): Double = elevations?.get(index) ?: Double.NaN

    /** Epoch millis, or [TIME_UNKNOWN] when unknown. */
    fun timestampOrUnknown(index: Int): Long = timestamps?.get(index) ?: TIME_UNKNOWN

    operator fun get(index: Int): GeoPoint {
        val e = elevationOrNaN(index)
        val t = timestampOrUnknown(index)
        return GeoPoint(
            latitude = latitudes[index],
            longitude = longitudes[index],
            elevation = if (e.isNaN()) null else e,
            timestamp = if (t == TIME_UNKNOWN) null else t,
        )
    }

    fun points(): Sequence<GeoPoint> = (0 until size).asSequence().map { get(it) }

    companion object {
        const val TIME_UNKNOWN: Long = Long.MIN_VALUE
    }
}

/** Growable, allocation-friendly builder for [GeoSegment]. Elevation/time arrays are only allocated when used. */
class GeoSegmentBuilder(initialCapacity: Int = 256) {
    private var lat = DoubleArray(initialCapacity.coerceAtLeast(4))
    private var lon = DoubleArray(lat.size)
    private var ele: DoubleArray? = null
    private var time: LongArray? = null

    var size: Int = 0
        private set

    fun add(
        latitude: Double,
        longitude: Double,
        elevation: Double = Double.NaN,
        timestamp: Long = GeoSegment.TIME_UNKNOWN,
    ) {
        if (size == lat.size) grow()
        lat[size] = latitude
        lon[size] = longitude
        if (!elevation.isNaN()) {
            val e = ele ?: DoubleArray(lat.size).also { it.fill(Double.NaN); ele = it }
            e[size] = elevation
        }
        if (timestamp != GeoSegment.TIME_UNKNOWN) {
            val t = time ?: LongArray(lat.size).also { it.fill(GeoSegment.TIME_UNKNOWN); time = it }
            t[size] = timestamp
        }
        size++
    }

    /** Sets the timestamp of an already-added point (used by KML gx:Track, where times arrive separately). */
    fun setTimestamp(index: Int, timestamp: Long) {
        require(index in 0 until size) { "index out of range" }
        if (timestamp == GeoSegment.TIME_UNKNOWN) return
        val t = time ?: LongArray(lat.size).also { it.fill(GeoSegment.TIME_UNKNOWN); time = it }
        t[index] = timestamp
    }

    /** True when elevations exist and every one of them is exactly 0 (typical "clamp to ground" placeholder). */
    fun hasOnlyZeroElevations(): Boolean {
        val e = ele ?: return false
        var any = false
        for (i in 0 until size) {
            val v = e[i]
            if (!v.isNaN()) {
                if (v != 0.0) return false
                any = true
            }
        }
        return any
    }

    fun dropElevations() {
        ele = null
    }

    fun build(name: String?, kind: SegmentKind): GeoSegment = GeoSegment(
        name = name,
        kind = kind,
        latitudes = lat.copyOf(size),
        longitudes = lon.copyOf(size),
        elevations = ele?.copyOf(size),
        timestamps = time?.copyOf(size),
    )

    private fun grow() {
        val newCap = lat.size * 2
        lat = lat.copyOf(newCap)
        lon = lon.copyOf(newCap)
        ele = ele?.let { old -> old.copyOf(newCap).also { it.fill(Double.NaN, old.size, newCap) } }
        time = time?.let { old -> old.copyOf(newCap).also { it.fill(GeoSegment.TIME_UNKNOWN, old.size, newCap) } }
    }
}
