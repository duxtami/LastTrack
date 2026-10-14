package LastTrack.duxtami.app.map.mapbox

import LastTrack.duxtami.app.core.map.BasemapStyle
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.map.MapProvider
import LastTrack.duxtami.app.core.model.MapProviderId

/**
 * Phase 1-2 placeholder: describes the provider but renders nothing and has no SDK dependency.
 * It always reports [MapAvailability.NotConfigured]; the real SDK integration replaces this class later.
 */
class MapboxProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.MAPBOX

    override val availability: MapAvailability = MapAvailability.NotConfigured("Mapbox access token")

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Default"),
        BasemapStyle("outdoors", "Outdoors"),
        BasemapStyle("light", "Light"),
        BasemapStyle("dark", "Dark"),
        BasemapStyle("satellite", "Satellite"),
        BasemapStyle("satellite_streets", "Satellite Streets"),
    )
}
