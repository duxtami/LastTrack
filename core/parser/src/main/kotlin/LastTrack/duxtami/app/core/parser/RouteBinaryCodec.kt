package LastTrack.duxtami.app.core.parser

import LastTrack.duxtami.app.core.model.GeoSegment
import LastTrack.duxtami.app.core.model.GeoWaypoint
import LastTrack.duxtami.app.core.model.RouteGeometry
import LastTrack.duxtami.app.core.model.SegmentKind
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.roundToInt

/**
 * Compact binary storage for coordinates (version 1).
 *
 * lat/lon are stored as Int in 1e-7 degree units (~1.1 cm), elevation as Float, timestamps as Long, and
 * elevation/time arrays are only written for segments that have them: ~8 bytes per bare point instead of one
 * database row each. The original file is kept separately, so this quantisation loses nothing for export.
 */
object RouteBinaryCodec {
    private const val MAGIC = 0x4C54524B // "LTRK"
    private const val VERSION: Short = 1
    private const val SCALE = 1e7
    private const val FLAG_ELE = 1
    private const val FLAG_TIME = 2
    private const val MAX_SEGMENTS = 5_000_000
    private const val MAX_POINTS = 100_000_000
    private const val MAX_STRING_BYTES = 1 shl 20

    fun write(output: OutputStream, segments: List<GeoSegment>, waypoints: List<GeoWaypoint>) {
        val out = DataOutputStream(output)
        out.writeInt(MAGIC)
        out.writeShort(VERSION.toInt())
        out.writeInt(segments.size)
        for (s in segments) {
            out.writeByte(s.kind.ordinal)
            writeString(out, s.name)
            out.writeInt(s.size)
            var flags = 0
            if (s.hasElevation) flags = flags or FLAG_ELE
            if (s.hasTimestamps) flags = flags or FLAG_TIME
            out.writeByte(flags)
            for (i in 0 until s.size) out.writeInt(quantize(s.latitude(i)))
            for (i in 0 until s.size) out.writeInt(quantize(s.longitude(i)))
            if (flags and FLAG_ELE != 0) for (i in 0 until s.size) out.writeFloat(s.elevationOrNaN(i).toFloat())
            if (flags and FLAG_TIME != 0) for (i in 0 until s.size) out.writeLong(s.timestampOrUnknown(i))
        }
        out.writeInt(waypoints.size)
        for (w in waypoints) {
            out.writeInt(quantize(w.latitude))
            out.writeInt(quantize(w.longitude))
            out.writeFloat((w.elevation ?: Double.NaN).toFloat())
            writeString(out, w.name)
            writeString(out, w.description)
        }
        out.flush()
    }

    fun read(input: InputStream): RouteGeometry {
        val inp = DataInputStream(input)
        if (inp.readInt() != MAGIC) throw IOException("Not a LastTrack coordinate file")
        val version = inp.readShort()
        if (version != VERSION) throw IOException("Unsupported coordinate file version $version")
        val segCount = inp.readInt()
        if (segCount !in 0..MAX_SEGMENTS) throw IOException("Corrupt segment count")
        val segments = ArrayList<GeoSegment>(minOf(segCount, 1024))
        var totalPoints = 0L
        repeat(segCount) {
            val kindOrdinal = inp.readUnsignedByte()
            val kind = SegmentKind.entries.getOrNull(kindOrdinal) ?: throw IOException("Corrupt segment kind")
            val name = readString(inp)
            val n = inp.readInt()
            totalPoints += n
            if (n < 0 || totalPoints > MAX_POINTS) throw IOException("Corrupt point count")
            val flags = inp.readUnsignedByte()
            val lat = DoubleArray(n)
            val lon = DoubleArray(n)
            for (i in 0 until n) lat[i] = inp.readInt() / SCALE
            for (i in 0 until n) lon[i] = inp.readInt() / SCALE
            val ele = if (flags and FLAG_ELE != 0) DoubleArray(n).also { a ->
                for (i in 0 until n) a[i] = inp.readFloat().toDouble()
            } else null
            val time = if (flags and FLAG_TIME != 0) LongArray(n).also { a ->
                for (i in 0 until n) a[i] = inp.readLong()
            } else null
            segments.add(GeoSegment(name, kind, lat, lon, ele, time))
        }
        val wCount = inp.readInt()
        if (wCount < 0 || wCount > MAX_POINTS) throw IOException("Corrupt waypoint count")
        val waypoints = ArrayList<GeoWaypoint>(minOf(wCount, 1024))
        repeat(wCount) {
            val lat = inp.readInt() / SCALE
            val lon = inp.readInt() / SCALE
            val ele = inp.readFloat()
            waypoints.add(
                GeoWaypoint(lat, lon, readString(inp), readString(inp), if (ele.isNaN()) null else ele.toDouble()),
            )
        }
        return RouteGeometry(segments, waypoints)
    }

    private fun quantize(v: Double): Int = (v * SCALE).roundToInt()

    private fun writeString(out: DataOutputStream, s: String?) {
        if (s == null) {
            out.writeInt(-1)
        } else {
            val bytes = s.toByteArray(Charsets.UTF_8)
            out.writeInt(bytes.size)
            out.write(bytes)
        }
    }

    private fun readString(inp: DataInputStream): String? {
        val len = inp.readInt()
        if (len == -1) return null
        if (len < 0 || len > MAX_STRING_BYTES) throw IOException("Corrupt string length")
        val bytes = ByteArray(len)
        inp.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
