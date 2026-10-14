package LastTrack.duxtami.app.core.parser

import LastTrack.duxtami.app.core.model.GeoSegmentBuilder
import LastTrack.duxtami.app.core.model.GeoWaypoint
import LastTrack.duxtami.app.core.model.ImportError
import LastTrack.duxtami.app.core.model.SegmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KmzReaderTest {
    private fun reader(dir: File, limits: ZipLimits = ZipLimits()) = KmzReader(dir, kmlParser(), limits)
    private fun kmz(vararg e: Pair<String, ByteArray>) = zipOf(*e).inputStream()
    private val kml = SAMPLE_KML.toByteArray()

    @Test fun parsesDocKmlAndCleansUp() {
        val dir = tempDir()
        val r = reader(dir).parse(kmz("doc.kml" to kml, "files/icon.png" to ByteArray(100)))
        assertEquals("KML Doc", r.name)
        assertEquals(3, r.segments.single().size)
        assertEquals(emptyList<String>(), dir.list()!!.toList())
    }

    @Test fun findsKmlInSubfolderAndPrefersDocKml() {
        val other = "<kml><Placemark><Point><coordinates>1,1</coordinates></Point></Placemark></kml>".toByteArray()
        val r = reader(tempDir()).parse(kmz("sub/other.kml" to other, "doc.kml" to kml))
        assertEquals("KML Doc", r.name)
        val r2 = reader(tempDir()).parse(kmz("nested/deep/route.kml" to kml))
        assertEquals("KML Doc", r2.name)
    }

    @Test fun missingKmlIsReported() = expectError(ImportError.NO_KML_IN_ARCHIVE) {
        reader(tempDir()).parse(kmz("readme.txt" to "hi".toByteArray()))
    }

    @Test fun notAZipIsInvalidArchive() = expectError(ImportError.INVALID_ARCHIVE) {
        reader(tempDir()).parse("plain text, not a zip".stream())
    }

    @Test fun truncatedZipIsInvalidArchive() = expectError(ImportError.INVALID_ARCHIVE) {
        val bytes = zipOf("doc.kml" to kml)
        reader(tempDir()).parse(bytes.copyOf(bytes.size / 2).inputStream())
    }

    @Test fun emptyInputIsEmptyFile() = expectError(ImportError.EMPTY_FILE) {
        reader(tempDir()).parse(ByteArray(0).inputStream())
    }

    @Test fun zipSlipEntryIsRejectedAndNothingEscapes() {
        val base = tempDir()
        val work = File(base, "work")
        expectError(ImportError.INVALID_ARCHIVE) {
            reader(work).parse(kmz("../evil.kml" to kml, "doc.kml" to kml))
        }
        assertFalse(File(base, "evil.kml").exists())
        assertEquals(emptyList<String>(), work.list()!!.toList())
    }

    @Test fun absoluteAndBackslashPathsAreRejected() {
        for (name in listOf("/etc/evil.kml", "a/../../b.kml", "..\\x.kml", "C:/x.kml")) {
            expectError(ImportError.INVALID_ARCHIVE) { reader(tempDir()).parse(kmz(name to kml)) }
        }
        assertTrue(ZipEntryNames.isSafe("a/b/doc.kml"))
        assertTrue(ZipEntryNames.isSafe("..hidden.kml"))
    }

    @Test fun zipBombIsRejectedByEntrySizeLimit() {
        val bomb = ByteArray(8 * 1024 * 1024) // compresses to a few KB
        val limits = ZipLimits(maxKmlEntryBytes = 1L shl 20)
        val work = tempDir()
        expectError(ImportError.FILE_TOO_LARGE) { reader(work, limits).parse(kmz("doc.kml" to bomb)) }
        assertEquals(emptyList<String>(), work.list()!!.toList())
    }

    @Test fun bombHiddenInNonKmlEntryIsStillCaughtByTotalLimit() {
        val limits = ZipLimits(maxTotalBytes = 2L shl 20)
        expectError(ImportError.FILE_TOO_LARGE) {
            reader(tempDir(), limits).parse(kmz("doc.kml" to kml, "big.bin" to ByteArray(8 * 1024 * 1024)))
        }
    }

    @Test fun compressionRatioLimitTriggers() {
        val limits = ZipLimits(ratioCheckThresholdBytes = 1L shl 20, maxCompressionRatio = 50)
        expectError(ImportError.FILE_TOO_LARGE) {
            reader(tempDir(), limits).parse(kmz("big.bin" to ByteArray(8 * 1024 * 1024), "doc.kml" to kml))
        }
    }

    @Test fun tooManyEntriesIsRejected() {
        val limits = ZipLimits(maxEntries = 10)
        val entries = (0 until 50).map { "f$it.txt" to ByteArray(1) }.toTypedArray()
        expectError(ImportError.FILE_TOO_LARGE) { reader(tempDir(), limits).parse(kmz(*entries, "doc.kml" to kml)) }
    }

    @Test fun kmzWhoseKmlHasNoCoordinatesFails() = expectError(ImportError.NO_COORDINATES) {
        reader(tempDir()).parse(kmz("doc.kml" to "<kml><Document/></kml>".toByteArray()))
    }
}

class StatsTest {
    private fun seg(vararg p: DoubleArray, kind: SegmentKind = SegmentKind.TRACK) = GeoSegmentBuilder().also { b ->
        for (a in p) b.add(a[0], a[1], if (a.size > 2) a[2] else Double.NaN, if (a.size > 3) a[3].toLong() else Long.MIN_VALUE)
    }.build(null, kind)

    @Test fun haversineKnownDistances() {
        // One degree of latitude is ~111.195 km on the mean-radius sphere.
        assertEquals(111_195.0, GeoMath.haversineMeters(0.0, 0.0, 1.0, 0.0), 5.0)
        assertEquals(111_195.0, GeoMath.haversineMeters(0.0, 0.0, 0.0, 1.0), 5.0)
        assertEquals(0.0, GeoMath.haversineMeters(10.0, 20.0, 10.0, 20.0), 1e-9)
        // Paris -> London is ~343.5 km.
        assertEquals(343_500.0, GeoMath.haversineMeters(48.8566, 2.3522, 51.5074, -0.1278), 1500.0)
    }

    @Test fun distanceSumsWithinSegmentsOnly() {
        val a = seg(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 0.0))
        val b = seg(doubleArrayOf(50.0, 50.0), doubleArrayOf(50.0, 51.0))
        val s = RouteStatsCalculator.calculate(listOf(a, b), 0)
        val expected = GeoMath.haversineMeters(0.0, 0.0, 1.0, 0.0) + GeoMath.haversineMeters(50.0, 50.0, 50.0, 51.0)
        assertEquals(expected, s.distanceMeters, 1e-6)
        assertEquals(4, s.pointCount)
        assertEquals(2, s.segmentCount)
    }

    @Test fun elevationRawGainLoss() {
        val s = RouteStatsCalculator.calculate(
            listOf(seg(doubleArrayOf(0.0, 0.0, 100.0), doubleArrayOf(0.001, 0.0, 110.0), doubleArrayOf(0.002, 0.0, 105.0), doubleArrayOf(0.003, 0.0, 120.0))),
            0, elevationThresholdMeters = 0.0,
        )
        assertEquals(25.0, s.elevationGainMeters!!, 1e-9)
        assertEquals(5.0, s.elevationLossMeters!!, 1e-9)
        assertEquals(120.0, s.maxElevationMeters!!, 1e-9)
        assertEquals(100.0, s.minElevationMeters!!, 1e-9)
    }

    @Test fun elevationHysteresisIgnoresNoise() {
        val ele = doubleArrayOf(100.0, 101.0, 103.0, 110.0, 108.0, 100.0)
        val seg = seg(*ele.mapIndexed { i, e -> doubleArrayOf(i * 0.001, 0.0, e) }.toTypedArray())
        val s = RouteStatsCalculator.calculate(listOf(seg), 0, elevationThresholdMeters = 3.0)
        assertEquals(10.0, s.elevationGainMeters!!, 1e-9)
        assertEquals(10.0, s.elevationLossMeters!!, 1e-9)
    }

    @Test fun noElevationMeansNullNotZero() {
        val s = RouteStatsCalculator.calculate(listOf(seg(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 0.0))), 0)
        assertNull(s.elevationGainMeters); assertNull(s.elevationLossMeters)
        assertNull(s.maxElevationMeters); assertNull(s.minElevationMeters)
        assertFalse(s.hasElevation)
    }

    @Test fun singleElevationGivesMinMaxButNoGainLoss() {
        val s = RouteStatsCalculator.calculate(listOf(seg(doubleArrayOf(0.0, 0.0, 50.0), doubleArrayOf(1.0, 0.0))), 0)
        assertEquals(50.0, s.maxElevationMeters!!, 1e-9)
        assertNull(s.elevationGainMeters)
    }

    @Test fun durationAndAverageSpeedFromTimestamps() {
        val t0 = 1_700_000_000_000.0
        val s = RouteStatsCalculator.calculate(
            listOf(seg(doubleArrayOf(0.0, 0.0, Double.NaN, t0), doubleArrayOf(1.0, 0.0, Double.NaN, t0 + 3_600_000))), 0,
        )
        assertEquals(3_600_000L, s.durationMillis)
        assertEquals(111_195.0 / 3600.0, s.averageSpeedMetersPerSecond!!, 0.01)
    }

    @Test fun noTimestampsMeansNullDurationAndSpeed() {
        val s = RouteStatsCalculator.calculate(listOf(seg(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 0.0))), 0)
        assertNull(s.durationMillis); assertNull(s.averageSpeedMetersPerSecond)
    }

    @Test fun plannedRoutesAreIgnoredWhenTracksExist() {
        val track = seg(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 0.0))
        val route = seg(doubleArrayOf(0.0, 0.0), doubleArrayOf(1.0, 0.0), kind = SegmentKind.ROUTE)
        val both = RouteStatsCalculator.calculate(listOf(track, route), 0)
        val trackOnly = RouteStatsCalculator.calculate(listOf(track), 0)
        assertEquals(trackOnly.distanceMeters, both.distanceMeters, 1e-9)
        val routeOnly = RouteStatsCalculator.calculate(listOf(route), 0)
        assertTrue(routeOnly.distanceMeters > 100_000)
    }

    @Test fun endToEndFromGpx() {
        val r = gpxParser().parse(SAMPLE_GPX.stream())
        val s = RouteStatsCalculator.calculate(r.segments, r.waypoints.size, 0.0)
        assertEquals(60_000L * 2, s.durationMillis)
        assertEquals(10.0, s.elevationGainMeters!!, 1e-9)
        assertEquals(5.0, s.elevationLossMeters!!, 1e-9)
        assertEquals(1, s.waypointCount)
        assertEquals(5, s.pointCount)
    }
}

class RouteBinaryCodecTest {
    @Test fun roundTripPreservesDataWithinQuantisation() {
        val r = gpxParser().parse(SAMPLE_GPX.stream())
        val bos = java.io.ByteArrayOutputStream()
        RouteBinaryCodec.write(bos, r.segments, r.waypoints)
        val g = RouteBinaryCodec.read(bos.toByteArray().inputStream())
        assertEquals(r.segments.size, g.segments.size)
        for (i in r.segments.indices) {
            val a = r.segments[i]; val b = g.segments[i]
            assertEquals(a.name, b.name); assertEquals(a.kind, b.kind); assertEquals(a.size, b.size)
            assertEquals(a.hasElevation, b.hasElevation); assertEquals(a.hasTimestamps, b.hasTimestamps)
            for (k in 0 until a.size) {
                assertEquals(a.latitude(k), b.latitude(k), 1e-7)
                assertEquals(a.longitude(k), b.longitude(k), 1e-7)
                assertEquals(a.timestampOrUnknown(k), b.timestampOrUnknown(k))
                if (a.hasElevation) assertEquals(a.elevationOrNaN(k), b.elevationOrNaN(k), 1e-3)
            }
        }
        assertEquals(r.waypoints.single().name, g.waypoints.single().name)
        assertEquals(r.waypoints.single().elevation!!, g.waypoints.single().elevation!!, 1e-3)
    }

    @Test fun largeRouteIsCompact() {
        val b = GeoSegmentBuilder()
        for (i in 0 until 100_000) b.add(10.0 + i * 1e-5, 20.0 + i * 1e-5)
        val bos = java.io.ByteArrayOutputStream()
        RouteBinaryCodec.write(bos, listOf(b.build("big", SegmentKind.TRACK)), emptyList<GeoWaypoint>())
        assertTrue("size=${bos.size()}", bos.size() < 100_000 * 8 + 200)
        assertEquals(100_000, RouteBinaryCodec.read(bos.toByteArray().inputStream()).pointCount)
    }

    @Test fun corruptDataFailsWithIoException() {
        for (bytes in listOf(ByteArray(0), byteArrayOf(1, 2, 3, 4, 5, 6, 7), "LTRK-not-really".toByteArray())) {
            try {
                RouteBinaryCodec.read(bytes.inputStream())
                org.junit.Assert.fail("expected failure")
            } catch (_: java.io.IOException) {
            }
        }
    }
}
