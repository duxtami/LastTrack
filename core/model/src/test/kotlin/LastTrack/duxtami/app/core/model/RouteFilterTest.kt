package LastTrack.duxtami.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteFilterTest {
    private fun stats() = RouteStats(0.0, null, null, null, null, null, null, 0, 0, 0)

    private fun route(id: String, name: String, file: String, format: SourceFormat) =
        RouteSummary(id, name, file, format, 10, stats(), null, 0)

    private val routes = listOf(
        route("1", "Morning Ride", "ride-2026-03-12.gpx", SourceFormat.GPX),
        route("2", "Alps Hike", "alps.kml", SourceFormat.KML),
        route("3", "Coastal Walk", "coast_walk.kmz", SourceFormat.KMZ),
        route("4", "Evening Ride", "evening.gpx", SourceFormat.GPX),
    )

    private fun ids(list: List<RouteSummary>) = list.map { it.id }

    @Test fun emptyQueryAndNoFormatReturnsAll() {
        assertEquals(listOf("1", "2", "3", "4"), ids(RouteFilter.filter(routes, "", null)))
        assertEquals(listOf("1", "2", "3", "4"), ids(RouteFilter.filter(routes, "   ", null)))
    }

    @Test fun matchesNameCaseInsensitively() {
        assertEquals(listOf("1", "4"), ids(RouteFilter.filter(routes, "RIDE", null)))
    }

    @Test fun matchesFileName() {
        assertEquals(listOf("3"), ids(RouteFilter.filter(routes, "coast_walk", null)))
    }

    @Test fun matchesFormatName() {
        assertEquals(listOf("2"), ids(RouteFilter.filter(routes, "kml", null)))
    }

    @Test fun allTokensMustMatch() {
        assertEquals(listOf("4"), ids(RouteFilter.filter(routes, "evening ride", null)))
        assertEquals(emptyList<String>(), ids(RouteFilter.filter(routes, "evening hike", null)))
    }

    @Test fun formatFilterCombinesWithQuery() {
        assertEquals(listOf("1", "4"), ids(RouteFilter.filter(routes, "", SourceFormat.GPX)))
        assertEquals(listOf("4"), ids(RouteFilter.filter(routes, "evening", SourceFormat.GPX)))
        assertEquals(emptyList<String>(), ids(RouteFilter.filter(routes, "alps", SourceFormat.GPX)))
    }

    @Test fun sourceFormatLookup() {
        assertEquals(SourceFormat.GPX, SourceFormat.fromFileName("a.GPX"))
        assertEquals(SourceFormat.KMZ, SourceFormat.fromFileName("x.y.kmz"))
        assertEquals(null, SourceFormat.fromFileName("noextension"))
        assertEquals(null, SourceFormat.fromFileName("a.txt"))
    }
}
