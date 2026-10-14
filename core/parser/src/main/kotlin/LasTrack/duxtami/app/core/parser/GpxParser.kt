package LasTrack.duxtami.app.core.parser

import LasTrack.duxtami.app.core.model.GeoSegment
import LasTrack.duxtami.app.core.model.GeoSegmentBuilder
import LasTrack.duxtami.app.core.model.GeoWaypoint
import LasTrack.duxtami.app.core.model.ImportError
import LasTrack.duxtami.app.core.model.SegmentKind
import LasTrack.duxtami.app.core.model.SourceFormat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream
import java.util.concurrent.CancellationException

/**
 * Streaming GPX 1.0/1.1 parser (tracks, track segments, routes, waypoints, elevation, time).
 * Only one XML event is held at a time; coordinates go straight into primitive arrays.
 */
class GpxParser(
    private val newXmlParser: XmlParserFactory = DefaultXmlParserFactory,
) : RouteParser {
    override val format: SourceFormat get() = SourceFormat.GPX

    override fun parse(input: InputStream, context: ParseContext): ParsedRoute {
        val stream = requireNonEmpty(input)
        try {
            val parser = newXmlParser()
            parser.setInput(stream, null)
            return GpxReader(parser, context).read()
        } catch (e: RouteParseException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: XmlPullParserException) {
            throw RouteParseException(ImportError.MALFORMED_FILE, "Malformed GPX", e)
        } catch (e: RuntimeException) {
            // Some pull-parser implementations surface bad input as unchecked exceptions.
            throw RouteParseException(ImportError.MALFORMED_FILE, "Malformed GPX", e)
        }
    }
}

private class GpxReader(private val p: XmlPullParser, private val ctx: ParseContext) {
    private val segments = ArrayList<GeoSegment>()
    private val waypoints = ArrayList<GeoWaypoint>()
    private val stack = ArrayList<String>()

    private var docName: String? = null
    private var docDesc: String? = null
    private var firstTrackName: String? = null
    private var firstRouteName: String? = null

    private var trackName: String? = null
    private val trackBuilders = ArrayList<GeoSegmentBuilder>()
    private var segBuilder: GeoSegmentBuilder? = null
    private var routeName: String? = null
    private var routeBuilder: GeoSegmentBuilder? = null

    private var ptValid = false
    private var ptLat = 0.0
    private var ptLon = 0.0
    private var ptEle = Double.NaN
    private var ptTime = GeoSegment.TIME_UNKNOWN
    private var ptName: String? = null
    private var ptDesc: String? = null
    private var ptCmt: String? = null

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
            throw RouteParseException(ImportError.NO_COORDINATES, "No coordinates found in GPX")
        }
        return ParsedRoute(
            name = (docName ?: firstTrackName ?: firstRouteName)?.takeIf { it.isNotBlank() },
            description = docDesc?.takeIf { it.isNotBlank() },
            segments = segments,
            waypoints = waypoints,
            skippedPoints = skipped,
        )
    }

    private fun onStart() {
        val name = localName(p.name)
        when (name) {
            "name", "desc", "cmt", "ele", "time" -> {
                val parent = stack.lastOrNull()
                val text = p.readElementText().trim() // consumes the END_TAG too
                assignText(name, parent, text)
                return
            }
            "trk" -> {
                trackName = null
                trackBuilders.clear()
            }
            "trkseg" -> segBuilder = GeoSegmentBuilder()
            "rte" -> {
                routeName = null
                routeBuilder = GeoSegmentBuilder()
            }
            "trkpt", "rtept", "wpt" -> beginPoint()
        }
        stack.add(name)
    }

    private fun onEnd() {
        val name = localName(p.name)
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        when (name) {
            "trkpt" -> endTrackPoint(segBuilder)
            "rtept" -> endTrackPoint(routeBuilder)
            "wpt" -> endWaypoint()
            "trkseg" -> {
                segBuilder?.let { if (it.size > 0) trackBuilders.add(it) }
                segBuilder = null
            }
            "trk" -> {
                if (firstTrackName == null) firstTrackName = trackName
                for (b in trackBuilders) segments.add(b.build(trackName, SegmentKind.TRACK))
                trackBuilders.clear()
            }
            "rte" -> {
                val b = routeBuilder
                if (b != null && b.size > 0) {
                    if (firstRouteName == null) firstRouteName = routeName
                    segments.add(b.build(routeName, SegmentKind.ROUTE))
                }
                routeBuilder = null
            }
        }
    }

    private fun beginPoint() {
        val lat = p.getAttributeValue(null, "lat")?.trim()?.toDoubleOrNull()
        val lon = p.getAttributeValue(null, "lon")?.trim()?.toDoubleOrNull()
        ptValid = lat != null && lon != null && isValidCoordinate(lat, lon)
        ptLat = lat ?: 0.0
        ptLon = lon ?: 0.0
        ptEle = Double.NaN
        ptTime = GeoSegment.TIME_UNKNOWN
        ptName = null
        ptDesc = null
        ptCmt = null
    }

    private fun assignText(element: String, parent: String?, text: String) {
        when (parent) {
            "trkpt", "rtept", "wpt" -> when (element) {
                "ele" -> ptEle = text.toDoubleOrNull()?.takeIf { it.isFinite() } ?: Double.NaN
                "time" -> ptTime = IsoTime.parseMillis(text) ?: GeoSegment.TIME_UNKNOWN
                "name" -> ptName = text
                "desc" -> ptDesc = text
                "cmt" -> ptCmt = text
            }
            "trk" -> if (element == "name") trackName = text
            "rte" -> if (element == "name") routeName = text
            "metadata", "gpx" -> when (element) {
                "name" -> if (docName == null) docName = text
                "desc" -> if (docDesc == null) docDesc = text
            }
        }
    }

    private fun endTrackPoint(builder: GeoSegmentBuilder?) {
        if (ptValid && builder != null) {
            builder.add(ptLat, ptLon, ptEle, ptTime)
            countPoint()
        } else {
            skipped++
        }
    }

    private fun endWaypoint() {
        if (ptValid) {
            waypoints.add(
                GeoWaypoint(
                    latitude = ptLat,
                    longitude = ptLon,
                    name = ptName?.takeIf { it.isNotBlank() },
                    description = (ptDesc ?: ptCmt)?.takeIf { it.isNotBlank() },
                    elevation = ptEle.takeIf { !it.isNaN() },
                ),
            )
        } else {
            skipped++
        }
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
}
