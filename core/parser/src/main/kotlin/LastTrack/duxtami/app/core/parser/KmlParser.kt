package LastTrack.duxtami.app.core.parser

import LastTrack.duxtami.app.core.model.GeoSegment
import LastTrack.duxtami.app.core.model.GeoSegmentBuilder
import LastTrack.duxtami.app.core.model.GeoWaypoint
import LastTrack.duxtami.app.core.model.ImportError
import LastTrack.duxtami.app.core.model.SegmentKind
import LastTrack.duxtami.app.core.model.SourceFormat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream
import java.util.concurrent.CancellationException

/**
 * Streaming KML parser.
 *
 * Supported: Placemark with Point (-> waypoint), LineString (-> track segment, also inside MultiGeometry) and
 * Google Earth's gx:Track (when + gx:coord). Folder/Document names are used as the route name.
 * Unsupported elements (Polygon, Model, NetworkLink, overlays...) are skipped without failing the import.
 *
 * KML stores unknown altitude as 0. If every altitude of a line is exactly 0 the elevations are discarded
 * rather than reported as a flat 0 m profile.
 */
class KmlParser(
    private val newXmlParser: XmlParserFactory = DefaultXmlParserFactory,
) : RouteParser {
    override val format: SourceFormat get() = SourceFormat.KML

    override fun parse(input: InputStream, context: ParseContext): ParsedRoute {
        val stream = requireNonEmpty(input)
        try {
            val parser = newXmlParser()
            parser.setInput(stream, null)
            return KmlReader(parser, context).read()
        } catch (e: RouteParseException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: XmlPullParserException) {
            throw RouteParseException(ImportError.MALFORMED_FILE, "Malformed KML", e)
        } catch (e: RuntimeException) {
            throw RouteParseException(ImportError.MALFORMED_FILE, "Malformed KML", e)
        }
    }
}

private class Pt(val lat: Double, val lon: Double, val ele: Double)

private class KmlReader(private val p: XmlPullParser, private val ctx: ParseContext) {
    private val segments = ArrayList<GeoSegment>()
    private val waypoints = ArrayList<GeoWaypoint>()
    private val stack = ArrayList<String>()

    private var docName: String? = null
    private var folderName: String? = null
    private var docDesc: String? = null
    private var firstPlacemarkName: String? = null

    private var inPlacemark = false
    private var pmName: String? = null
    private var pmDesc: String? = null
    private val pmLines = ArrayList<GeoSegmentBuilder>()
    private val pmPoints = ArrayList<Pt>()

    private var gxBuilder: GeoSegmentBuilder? = null
    private var gxTimes = ArrayList<Long>()

    private var totalPoints = 0L
    private var skipped = 0

    private var sawElement = false

    fun read(): ParsedRoute {
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> { sawElement = true; onStart() }
                XmlPullParser.END_TAG -> onEnd()
            }
            ev = p.next()
        }
        if (!sawElement) throw RouteParseException(ImportError.MALFORMED_FILE, "Not an XML document")
        if (stack.isNotEmpty()) throw XmlPullParserException("Unbalanced document")
        if (segments.isEmpty() && waypoints.isEmpty()) {
            throw RouteParseException(ImportError.NO_COORDINATES, "No coordinates found in KML")
        }
        return ParsedRoute(
            name = (docName ?: folderName ?: firstPlacemarkName)?.takeIf { it.isNotBlank() },
            description = docDesc?.takeIf { it.isNotBlank() },
            segments = segments,
            waypoints = waypoints,
            skippedPoints = skipped,
        )
    }

    private fun onStart() {
        val name = localName(p.name)
        when (name) {
            "name", "description" -> {
                val parent = stack.lastOrNull()
                val text = p.readElementText().trim()
                when (parent) {
                    "Placemark" -> if (name == "name") pmName = text else pmDesc = text.take(MAX_DESCRIPTION)
                    "Document" -> if (name == "name") { if (docName == null) docName = text } else if (docDesc == null) docDesc = text.take(MAX_DESCRIPTION)
                    "Folder" -> if (name == "name" && folderName == null) folderName = text
                }
                return
            }
            "coordinates" -> {
                val parent = stack.lastOrNull()
                val text = p.readElementText()
                if (inPlacemark) handleCoordinates(parent, text)
                return
            }
            "when" -> {
                val text = p.readElementText()
                if (gxBuilder != null) gxTimes.add(IsoTime.parseMillis(text) ?: GeoSegment.TIME_UNKNOWN)
                return
            }
            "coord" -> {
                val text = p.readElementText()
                gxBuilder?.let { b -> handleGxCoord(b, text) }
                return
            }
            "Placemark" -> {
                inPlacemark = true
                pmName = null
                pmDesc = null
                pmLines.clear()
                pmPoints.clear()
            }
            "Track" -> {
                gxBuilder = GeoSegmentBuilder()
                gxTimes = ArrayList()
            }
        }
        stack.add(name)
    }

    private fun onEnd() {
        val name = localName(p.name)
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        when (name) {
            "Track" -> finishGxTrack()
            "Placemark" -> finishPlacemark()
        }
    }

    private fun handleCoordinates(parent: String?, text: String) {
        when (parent) {
            "Point" -> {
                parseKmlCoordinates(text) { lon, lat, alt ->
                    if (isValidCoordinate(lat, lon)) pmPoints.add(Pt(lat, lon, alt)) else skipped++
                }
            }
            "LineString" -> {
                val b = GeoSegmentBuilder()
                parseKmlCoordinates(text) { lon, lat, alt ->
                    if (isValidCoordinate(lat, lon)) {
                        b.add(lat, lon, alt)
                        countPoint()
                    } else skipped++
                }
                if (b.size > 0) pmLines.add(b)
            }
            // LinearRing (polygon boundaries) and anything else are intentionally unsupported.
        }
    }

    private fun handleGxCoord(b: GeoSegmentBuilder, text: String) {
        val parts = text.trim().split(Regex("\\s+"))
        val lon = parts.getOrNull(0)?.toDoubleOrNull()
        val lat = parts.getOrNull(1)?.toDoubleOrNull()
        val alt = parts.getOrNull(2)?.toDoubleOrNull() ?: Double.NaN
        if (lat != null && lon != null && isValidCoordinate(lat, lon)) {
            b.add(lat, lon, alt)
            countPoint()
        } else {
            // keep index alignment with <when> entries by recording an invalid slot
            skipped++
            invalidGxSlots.add(b.size + invalidGxSlots.size)
        }
    }

    private val invalidGxSlots = ArrayList<Int>()

    private fun finishGxTrack() {
        val b = gxBuilder ?: return
        gxBuilder = null
        if (b.size > 0) {
            // Pair times with coordinates by position, skipping slots whose coordinate was invalid.
            var coordIndex = 0
            for (slot in gxTimes.indices) {
                if (slot in invalidGxSlots) continue
                if (coordIndex >= b.size) break
                b.setTimestamp(coordIndex, gxTimes[slot])
                coordIndex++
            }
            pmLines.add(b)
        }
        invalidGxSlots.clear()
        gxTimes = ArrayList()
    }

    private fun finishPlacemark() {
        if (firstPlacemarkName == null) firstPlacemarkName = pmName
        for (b in pmLines) {
            if (b.hasOnlyZeroElevations()) b.dropElevations()
            if (b.size > 0) segments.add(b.build(pmName?.takeIf { it.isNotBlank() }, SegmentKind.TRACK))
        }
        for (pt in pmPoints) {
            waypoints.add(
                GeoWaypoint(
                    latitude = pt.lat,
                    longitude = pt.lon,
                    name = pmName?.takeIf { it.isNotBlank() },
                    description = pmDesc?.takeIf { it.isNotBlank() },
                    elevation = pt.ele.takeIf { !it.isNaN() && it != 0.0 },
                ),
            )
        }
        pmLines.clear()
        pmPoints.clear()
        inPlacemark = false
    }

    private fun countPoint() {
        totalPoints++
        if (totalPoints > MAX_POINTS_PER_FILE) {
            throw RouteParseException(ImportError.FILE_TOO_LARGE, "Too many coordinates")
        }
        if (totalPoints % PROGRESS_INTERVAL == 0L) {
            ctx.checkCancelled()
            ctx.onPointsParsed(totalPoints)
        }
    }

    private companion object {
        const val MAX_DESCRIPTION = 4000
    }
}

/**
 * Scans a KML `<coordinates>` text ("lon,lat[,alt]" tuples separated by whitespace) without allocating a
 * list of tokens. Malformed tuples are skipped. Altitude is NaN when absent.
 */
internal fun parseKmlCoordinates(text: String, onCoordinate: (lon: Double, lat: Double, alt: Double) -> Unit) {
    val n = text.length
    var i = 0
    while (i < n) {
        while (i < n && text[i].isWhitespace()) i++
        if (i >= n) break
        val start = i
        while (i < n && !text[i].isWhitespace()) i++
        val end = i
        val c1 = text.indexOf(',', start).let { if (it in start until end) it else -1 }
        if (c1 < 0) continue
        val c2 = text.indexOf(',', c1 + 1).let { if (it in (c1 + 1) until end) it else -1 }
        val lon = text.substring(start, c1).toDoubleOrNull()
        val lat = text.substring(c1 + 1, if (c2 >= 0) c2 else end).toDoubleOrNull()
        val alt = if (c2 >= 0) {
            val altEnd = text.indexOf(',', c2 + 1).let { if (it in (c2 + 1) until end) it else end }
            text.substring(c2 + 1, altEnd).toDoubleOrNull() ?: Double.NaN
        } else Double.NaN
        if (lon != null && lat != null) onCoordinate(lon, lat, alt)
    }
}
