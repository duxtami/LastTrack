package LastTrack.duxtami.app.ui

import LastTrack.duxtami.app.ui.common.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormattersTest {
    private val en = Locale.US

    @Test fun distanceUsesMetresBelowOneKilometre() {
        assertEquals("850 m", Formatters.distance(850.2, en))
        assertEquals("0 m", Formatters.distance(0.0, en))
    }

    @Test fun distanceUsesKilometresWithOneDecimalThenNone() {
        assertEquals("1.0 km", Formatters.distance(1000.0, en))
        assertEquals("38.4 km", Formatters.distance(38_432.0, en))
        assertEquals("142 km", Formatters.distance(141_900.0, en))
    }

    @Test fun durationFormats() {
        assertEquals("8s", Formatters.duration(8_000))
        assertEquals("42m 10s", Formatters.duration(2_530_000))
        assertEquals("1h 42m", Formatters.duration(6_120_000))
    }

    @Test fun elevationFormats() {
        assertEquals("+642 m", Formatters.signedElevation(642.4, true, en))
        assertEquals("−120 m", Formatters.signedElevation(-119.6, false, en))
        assertEquals("1,500 m", Formatters.elevation(1500.0, en))
    }

    @Test fun speedAndCount() {
        assertEquals("30.9 km/h", Formatters.speedKmh(8.58, en))
        assertEquals("120,000", Formatters.count(120_000, en))
    }
}
