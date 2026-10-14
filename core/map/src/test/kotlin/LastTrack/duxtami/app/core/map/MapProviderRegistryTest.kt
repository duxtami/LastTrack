package LastTrack.duxtami.app.core.map

import LastTrack.duxtami.app.core.model.MapProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapProviderRegistryTest {
    private class Fake(override val id: MapProviderId, override val basemaps: List<BasemapStyle>) : MapProvider {
        override val availability: MapAvailability = MapAvailability.NotConfigured("key")
    }

    private val registry = MapProviderRegistry(
        listOf(
            Fake(MapProviderId.GOOGLE_MAPS, listOf(BasemapStyle("default", "Default"), BasemapStyle("sat", "Satellite"))),
            Fake(MapProviderId.MAPBOX, listOf(BasemapStyle("default", "Default"))),
        ),
    )

    @Test fun resolvesKnownBasemap() {
        assertEquals("sat", registry.resolveBasemap(MapProviderId.GOOGLE_MAPS, "sat")?.id)
    }

    @Test fun fallsBackToProviderDefaultWhenBasemapUnknown() {
        assertEquals("default", registry.resolveBasemap(MapProviderId.MAPBOX, "sat")?.id)
    }

    @Test fun unknownProviderHasNoBasemaps() {
        assertNull(registry.resolveBasemap(MapProviderId.ARCGIS, "default"))
        assertEquals(emptyList<BasemapStyle>(), registry.basemapsFor(MapProviderId.ARCGIS))
    }
}
