package LastTrack.duxtami.app.map.google

import LastTrack.duxtami.app.core.map.BasemapStyle
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.map.MapProvider
import LastTrack.duxtami.app.core.model.MapProviderId

/**
 * Phase 1-2 placeholder: describes the provider but renders nothing and has no SDK dependency.
 * It always reports [MapAvailability.NotConfigured]; the real SDK integration replaces this class later.
 */
class GoogleMapProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.GOOGLE_MAPS

    override val availability: MapAvailability = MapAvailability.NotConfigured("Google Maps API key")

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Default"),
        BasemapStyle("satellite", "Satellite"),
        BasemapStyle("terrain", "Terrain"),
        BasemapStyle("hybrid", "Hybrid"),
    )
}
