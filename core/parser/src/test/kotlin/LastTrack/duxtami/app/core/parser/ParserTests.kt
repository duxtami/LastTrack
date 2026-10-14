package LastTrack.duxtami.app.core.parser

import LastTrack.duxtami.app.core.model.ImportError
import LastTrack.duxtami.app.core.model.SegmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxParserTest {
    @Test fun parsesTracksSegmentsWaypointsElevationAndTime() {
        val r = gpxParser().parse(SAMPLE_GPX.stream())
        assertEquals("Sunday Loop", r.name)
        assertEquals("A test loop", r.description)
        assertEquals(2, r.segments.size)
        assertEquals(3, r.segments[0].size)
        assertEquals(2, r.segments[1].size)
        assertEquals(SegmentKind.TRACK, r.segments[0].kind)
        assertEquals("Track One", r.segments[0].name)
        assertEquals(46.001, r.segments[0].latitude(1), 1e-9)
        assertEquals(110.0, r.segments[0].elevationOrNaN(1), 1e-9)
        assertEquals(1773309660000L, r.segments[0].timestampOrUnknown(1))
        assertFalse(r.segments[1].hasElevation)
        assertFalse(r.segments[1].hasTimestamps)
        assertEquals(1, r.waypoints.size)
        assertEquals("Summit", r.waypoints[0].name)
        assertEquals("Top", r.waypoints[0].description)
        assertEquals(1500.5, r.waypoints[0].elevation!!, 1e-9)
    }

    @Test fun parsesRoutes() {
        val gpx = """<gpx version="1.1"><rte><name>Planned</name>
            <rtept lat="1.0" lon="2.0"/><rtept lat="1.1" lon="2.1"/></rte></gpx>"""
        val r = gpxParser().parse(gpx.stream())
        assertEquals("Planned", r.name)
        assertEquals(SegmentKind.ROUTE, r.segments.single().kind)
        assertEquals(2, r.segments.single().size)
    }

    @Test fun skipsInvalidCoordinatesWithoutCrashing() {
        val gpx = """<gpx><trk><trkseg>
            <trkpt lat="abc" lon="1"/><trkpt lat="95" lon="1"/><trkpt lon="1"/>
            <trkpt lat="10" lon="20"/></trkseg></trk></gpx>"""
        val r = gpxParser().parse(gpx.stream())
        assertEquals(1, r.segments.single().size)
        assertEquals(3, r.skippedPoints)
    }

    @Test fun emptyFileIsReported() = expectError(ImportError.EMPTY_FILE) { gpxParser().parse("".stream()) }

    @Test fun malformedXmlIsReported() = expectError(ImportError.MALFORMED_FILE) {
        gpxParser().parse("<gpx><trk><trkseg><trkpt lat=\"1\" lon=\"2\">".stream())
    }

    @Test fun notXmlIsReported() = expectError(ImportError.MALFORMED_FILE) {
        gpxParser().parse("this is definitely not xml".stream())
    }

    @Test fun fileWithoutCoordinatesIsReported() = expectError(ImportError.NO_COORDINATES) {
        gpxParser().parse("<gpx><metadata><name>x</name></metadata><trk><trkseg/></trk></gpx>".stream())
    }

    @Test fun largeFileStreamsAndReportsProgress() {
        val n = 120_000
        val sb = StringBuilder("<gpx><trk><trkseg>")
        for (i in 0 until n) sb.append("<trkpt lat=\"").append(10.0 + i * 1e-5).append("\" lon=\"20\"><ele>").append(i % 50).append("</ele></trkpt>")
        sb.append("</trkseg></trk></gpx>")
        var progress = 0L
        var checks = 0
        val ctx = object : ParseContext {
            override fun checkCancelled() { checks++ }
            override fun onPointsParsed(total: Long) { progress = total }
        }
        val r = gpxParser().parse(sb.toString().stream(), ctx)
        assertEquals(n, r.segments.single().size)
        assertTrue(progress > 100_000)
        assertTrue(checks > 20)
    }

    @Test fun cancellationPropagates() {
        val sb = StringBuilder("<gpx><trk><trkseg>")
        for (i in 0 until 10_000) sb.append("<trkpt lat=\"1\" lon=\"2\"/>")
        sb.append("</trkseg></trk></gpx>")
        val ctx = object : ParseContext {
            override fun checkCancelled() { throw java.util.concurrent.CancellationException("stop") }
        }
        try {
            gpxParser().parse(sb.toString().stream(), ctx)
            org.junit.Assert.fail("expected cancellation")
        } catch (_: java.util.concurrent.CancellationException) {
        }
    }
}

class KmlParserTest {
    @Test fun parsesPlacemarksPointsAndLineStrings() {
        val r = kmlParser().parse(SAMPLE_KML.stream())
        assertEquals("KML Doc", r.name)
        assertEquals(1, r.waypoints.size)
        assertEquals("Start", r.waypoints[0].name)
        assertEquals("Begin here", r.waypoints[0].description)
        assertEquals(500.0, r.waypoints[0].elevation!!, 1e-9)
        val seg = r.segments.single()
        assertEquals("Path", seg.name)
        assertEquals(3, seg.size)
        assertEquals(7.001, seg.longitude(1), 1e-9)
        assertEquals(46.0, seg.latitude(1), 1e-9)
        assertEquals(110.0, seg.elevationOrNaN(1), 1e-9)
    }

    @Test fun parsesGxTrackWithTimes() {
        val kml = """<kml xmlns:gx="x"><Document><Placemark><name>T</name><gx:Track>
            <when>2026-03-12T10:00:00Z</when><when>2026-03-12T10:00:10Z</when>
            <gx:coord>7.0 46.0 100</gx:coord><gx:coord>7.001 46.0 101</gx:coord>
            </gx:Track></Placemark></Document></kml>"""
        val seg = kmlParser().parse(kml.stream()).segments.single()
        assertEquals(2, seg.size)
        assertEquals(10_000L, seg.timestampOrUnknown(1) - seg.timestampOrUnknown(0))
        assertEquals(101.0, seg.elevationOrNaN(1), 1e-9)
    }

    @Test fun allZeroAltitudeIsTreatedAsUnknown() {
        val kml = """<kml><Placemark><LineString><coordinates>1,2,0 1.1,2.1,0</coordinates></LineString></Placemark></kml>"""
        val seg = kmlParser().parse(kml.stream()).segments.single()
        assertFalse(seg.hasElevation)
    }

    @Test fun polygonsAreSkippedAndNotFatal() {
        val kml = """<kml><Document>
            <Placemark><Polygon><outerBoundaryIs><LinearRing><coordinates>1,1 2,2 3,1 1,1</coordinates></LinearRing></outerBoundaryIs></Polygon></Placemark>
            <Placemark><Point><coordinates>5,6</coordinates></Point></Placemark></Document></kml>"""
        val r = kmlParser().parse(kml.stream())
        assertEquals(0, r.segments.size)
        assertEquals(1, r.waypoints.size)
    }

    @Test fun onlyUnsupportedGeometryReportsNoCoordinates() = expectError(ImportError.NO_COORDINATES) {
        kmlParser().parse("<kml><Placemark><Polygon><LinearRing><coordinates>1,1 2,2</coordinates></LinearRing></Polygon></Placemark></kml>".stream())
    }

    @Test fun malformedKmlIsReported() = expectError(ImportError.MALFORMED_FILE) {
        kmlParser().parse("<kml><Document><Placemark>".stream())
    }

    @Test fun emptyKmlIsReported() = expectError(ImportError.EMPTY_FILE) { kmlParser().parse("".stream()) }

    @Test fun skipsMalformedTuples() {
        val kml = """<kml><Placemark><LineString><coordinates>1,2 foo 3,4,5 ,, 6,7</coordinates></LineString></Placemark></kml>"""
        assertEquals(3, kmlParser().parse(kml.stream()).segments.single().size)
    }
}

class FormatDetectorTest {
    @Test fun detectsByContentFirst() {
        assertEquals(LastTrack.duxtami.app.core.model.SourceFormat.GPX, FormatDetector.detect("x.kml", "<?xml?><gpx>".toByteArray()))
        assertEquals(LastTrack.duxtami.app.core.model.SourceFormat.KML, FormatDetector.detect("x.gpx", "<kml>".toByteArray()))
        assertEquals(LastTrack.duxtami.app.core.model.SourceFormat.KMZ, FormatDetector.detect("x.kml", byteArrayOf(0x50, 0x4B, 3, 4)))
    }

    @Test fun fallsBackToExtensionThenNull() {
        assertEquals(LastTrack.duxtami.app.core.model.SourceFormat.KMZ, FormatDetector.detect("a.kmz", "garbage".toByteArray()))
        assertNull(FormatDetector.detect("a.txt", "garbage".toByteArray()))
        assertNull(FormatDetector.detect(null, ByteArray(0)))
    }
}
