package LastTrack.duxtami.app.core.parser

import LastTrack.duxtami.app.core.model.ImportError
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.kxml2.io.KXmlParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** JVM tests use kxml2 directly; on Android the platform pull parser is used via DefaultXmlParserFactory. */
val testXml: XmlParserFactory = { KXmlParser() }

fun gpxParser() = GpxParser(testXml)
fun kmlParser() = KmlParser(testXml)

fun String.stream() = ByteArrayInputStream(toByteArray(Charsets.UTF_8))

fun tempDir(): File = Files.createTempDirectory("lasttrack-test").toFile().also { it.deleteOnExit() }

fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
    val bos = ByteArrayOutputStream()
    ZipOutputStream(bos).use { z ->
        for ((name, data) in entries) {
            z.putNextEntry(ZipEntry(name))
            z.write(data)
            z.closeEntry()
        }
    }
    return bos.toByteArray()
}

inline fun expectError(expected: ImportError, block: () -> Unit) {
    try {
        block()
        fail("Expected RouteParseException($expected)")
    } catch (e: RouteParseException) {
        assertEquals(expected, e.error)
    }
}

const val SAMPLE_GPX = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata><name>Sunday Loop</name><desc>A test loop</desc></metadata>
  <wpt lat="46.0" lon="7.0"><ele>1500.5</ele><name>Summit</name><desc>Top</desc></wpt>
  <trk>
    <name>Track One</name>
    <trkseg>
      <trkpt lat="46.0" lon="7.0"><ele>100</ele><time>2026-03-12T10:00:00Z</time></trkpt>
      <trkpt lat="46.001" lon="7.0"><ele>110</ele><time>2026-03-12T10:01:00Z</time></trkpt>
      <trkpt lat="46.002" lon="7.0"><ele>105</ele><time>2026-03-12T10:02:00Z</time></trkpt>
    </trkseg>
    <trkseg>
      <trkpt lat="46.01" lon="7.01"/>
      <trkpt lat="46.011" lon="7.01"/>
    </trkseg>
  </trk>
</gpx>"""

const val SAMPLE_KML = """<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
 <Document>
  <name>KML Doc</name>
  <Placemark><name>Start</name><description>Begin here</description>
    <Point><coordinates>7.0,46.0,500</coordinates></Point></Placemark>
  <Folder><name>Folder A</name>
   <Placemark><name>Path</name>
     <LineString><coordinates>
        7.0,46.0,100 7.001,46.0,110
        7.002,46.0,105
     </coordinates></LineString></Placemark>
  </Folder>
 </Document>
</kml>"""
