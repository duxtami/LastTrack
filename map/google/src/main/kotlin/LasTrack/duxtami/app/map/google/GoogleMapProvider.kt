package LasTrack.duxtami.app.map.google

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.model.MapProviderId

class GoogleMapProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.GOOGLE_MAPS

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Satellite Hybrid (HD)"),
        BasemapStyle("google_streets", "Streets / Roadmap (HD)"),
        BasemapStyle("google_terrain", "Terrain (HD)"),
    )
}
