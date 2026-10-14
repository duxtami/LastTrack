package LasTrack.duxtami.app.core.parser

import LasTrack.duxtami.app.core.model.GeoSegment
import LasTrack.duxtami.app.core.model.GeoWaypoint
import LasTrack.duxtami.app.core.model.ImportError
import LasTrack.duxtami.app.core.model.SourceFormat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Thrown for any problem with the *content* of a file. Carries a typed [error], never UI text. */
class RouteParseException(
    val error: ImportError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Provider-independent parse result, before ids / stats / storage are attached. */
class ParsedRoute(
    val name: String?,
    val description: String?,
    val segments: List<GeoSegment>,
    val waypoints: List<GeoWaypoint>,
    /** Coordinates that were skipped because they were missing, non-numeric or out of range. */
    val skippedPoints: Int = 0,
)

/** Hooks a caller can pass in to observe progress and support cooperative cancellation. */
interface ParseContext {
    /** Throw (e.g. CancellationException) to abort parsing. Called every few thousand points. */
    fun checkCancelled() {}

    /** Number of coordinates read so far. Called every few thousand points. */
    fun onPointsParsed(total: Long) {}

    object None : ParseContext
}

/** Blocking, streaming parser. Callers run it off the main thread. */
interface RouteParser {
    val format: SourceFormat
    fun parse(input: InputStream, context: ParseContext = ParseContext.None): ParsedRoute
}

/** Hard cap so a hostile file can't exhaust memory with coordinates. */
const val MAX_POINTS_PER_FILE: Long = 20_000_000

internal const val PROGRESS_INTERVAL = 4096

/** Creates an XmlPull parser. On Android this resolves to the platform's pull parser. */
typealias XmlParserFactory = () -> XmlPullParser

val DefaultXmlParserFactory: XmlParserFactory = { XmlPullParserFactory.newInstance().newPullParser() }

internal fun localName(qName: String?): String {
    if (qName == null) return ""
    val i = qName.lastIndexOf(':')
    return if (i < 0) qName else qName.substring(i + 1)
}

/** Fails fast on empty input without consuming it. */
internal fun requireNonEmpty(input: InputStream): InputStream {
    val stream = if (input.markSupported()) input else BufferedInputStream(input, 64 * 1024)
    stream.mark(1)
    if (stream.read() == -1) throw RouteParseException(ImportError.EMPTY_FILE, "File is empty")
    stream.reset()
    return stream
}

/**
 * Reads the text of the element the parser is currently positioned on (a START_TAG) and leaves the parser on
 * its matching END_TAG. Nested elements are skipped (their text is included, which is fine for the simple
 * leaf elements this is used for).
 */
internal fun XmlPullParser.readElementText(): String {
    val startDepth = depth
    val sb = StringBuilder()
    var ev = next()
    while (!(ev == XmlPullParser.END_TAG && depth == startDepth)) {
        when (ev) {
            XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(text)
            XmlPullParser.END_DOCUMENT -> throw org.xmlpull.v1.XmlPullParserException("Unexpected end of document")
        }
        ev = next()
    }
    return sb.toString()
}

internal fun isValidCoordinate(lat: Double, lon: Double): Boolean =
    lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0

/** Parses ISO-8601 timestamps as found in GPX `<time>` and KML `<when>`. Returns epoch millis or null. */
object IsoTime {
    fun parseMillis(text: String): Long? {
        val s = text.trim()
        if (s.isEmpty()) return null
        return try {
            OffsetDateTime.parse(s).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }
}
