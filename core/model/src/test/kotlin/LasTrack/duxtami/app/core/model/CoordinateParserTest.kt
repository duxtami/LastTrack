package LasTrack.duxtami.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CoordinateParserTest {

    @Test
    fun parsesDecimalPairWithComma() {
        val parsed = CoordinateParser.parse("28.6139, 77.2090")
        assertNotNull(parsed)
        assertEquals(28.6139, parsed!!.latitude, 0.0001)
        assertEquals(77.2090, parsed.longitude, 0.0001)
    }

    @Test
    fun parsesDecimalPairWithSpace() {
        val parsed = CoordinateParser.parse("-33.8688 151.2093")
        assertNotNull(parsed)
        assertEquals(-33.8688, parsed!!.latitude, 0.0001)
        assertEquals(151.2093, parsed.longitude, 0.0001)
    }

    @Test
    fun parsesGoogleMapsUrlWithAt() {
        val url = "https://www.google.com/maps/place/India+Gate/@28.612912,77.2295097,17z/data=!3m1!4b1"
        val parsed = CoordinateParser.parse(url)
        assertNotNull(parsed)
        assertEquals(28.612912, parsed!!.latitude, 0.0001)
        assertEquals(77.2295097, parsed.longitude, 0.0001)
        assertEquals("India Gate", parsed.suggestedName)
    }

    @Test
    fun parsesGoogleMapsQueryUrl() {
        val url = "https://maps.google.com/?q=19.0760,72.8777"
        val parsed = CoordinateParser.parse(url)
        assertNotNull(parsed)
        assertEquals(19.0760, parsed!!.latitude, 0.0001)
        assertEquals(72.8777, parsed.longitude, 0.0001)
    }

    @Test
    fun parsesGeoUri() {
        val uri = "geo:37.7749,-122.4194"
        val parsed = CoordinateParser.parse(uri)
        assertNotNull(parsed)
        assertEquals(37.7749, parsed!!.latitude, 0.0001)
        assertEquals(-122.4194, parsed.longitude, 0.0001)
    }

    @Test
    fun parsesDmsCoordinates() {
        val dms = "28°36'50.0\"N 77°12'32.4\"E"
        val parsed = CoordinateParser.parse(dms)
        assertNotNull(parsed)
        assertEquals(28.61388, parsed!!.latitude, 0.001)
        assertEquals(77.20900, parsed.longitude, 0.001)
    }

    @Test
    fun parsesSharedGoogleMapsText() {
        val shared = "Check out India Gate https://www.google.com/maps/place/India+Gate/@28.612912,77.2295097,17z"
        val parsed = CoordinateParser.parse(shared)
        assertNotNull(parsed)
        assertEquals(28.612912, parsed!!.latitude, 0.0001)
        assertEquals(77.2295097, parsed.longitude, 0.0001)
        assertEquals("India Gate", parsed.suggestedName)
    }

    @Test
    fun parsesGoogleMaps3d4dUrl() {
        val url = "https://www.google.com/maps/place/Taj+Mahal/data=!4m2!3m1!1s0x0:0x0!8m2!3d27.1751448!4d78.0421422"
        val parsed = CoordinateParser.parse(url)
        assertNotNull(parsed)
        assertEquals(27.1751448, parsed!!.latitude, 0.0001)
        assertEquals(78.0421422, parsed.longitude, 0.0001)
        assertEquals("Taj Mahal", parsed.suggestedName)
    }

    @Test
    fun parsesGoogleMapsDestinationUrl() {
        val url = "https://www.google.com/maps?daddr=28.6129,77.2295"
        val parsed = CoordinateParser.parse(url)
        assertNotNull(parsed)
        assertEquals(28.6129, parsed!!.latitude, 0.0001)
        assertEquals(77.2295, parsed.longitude, 0.0001)
    }
}
