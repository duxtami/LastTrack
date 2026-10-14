package LasTrack.duxtami.app.ui.common

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Pure formatting helpers (metric units). Kept free of Android classes so they are unit-testable. */
object Formatters {
    fun distance(meters: Double, locale: Locale = Locale.getDefault()): String = when {
        meters < 1000.0 -> "${NumberFormat.getIntegerInstance(locale).format(meters.roundToLong())} m"
        meters < 100_000.0 -> String.format(locale, "%.1f km", meters / 1000.0)
        else -> "${NumberFormat.getIntegerInstance(locale).format((meters / 1000.0).roundToLong())} km"
    }

    fun duration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    fun elevation(meters: Double, locale: Locale = Locale.getDefault()): String =
        "${NumberFormat.getIntegerInstance(locale).format(meters.roundToLong())} m"

    fun signedElevation(meters: Double, positive: Boolean, locale: Locale = Locale.getDefault()): String =
        (if (positive) "+" else "−") + NumberFormat.getIntegerInstance(locale).format(abs(meters).roundToLong()) + " m"

    fun speedKmh(metersPerSecond: Double, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.1f km/h", metersPerSecond * 3.6)

    fun count(n: Int, locale: Locale = Locale.getDefault()): String = NumberFormat.getIntegerInstance(locale).format(n)

    fun date(epochMillis: Long, locale: Locale = Locale.getDefault()): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(epochMillis))

    fun dateTime(epochMillis: Long, locale: Locale = Locale.getDefault()): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(epochMillis))
}
