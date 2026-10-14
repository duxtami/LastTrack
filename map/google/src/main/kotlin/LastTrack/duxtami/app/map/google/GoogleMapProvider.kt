package LastTrack.duxtami.app.map.google

import LastTrack.duxtami.app.core.map.BasemapStyle
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.map.MapProvider
import LastTrack.duxtami.app.core.model.MapProviderId

class GoogleMapProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.GOOGLE_MAPS

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Roadmap"),
        BasemapStyle("satellite", "Satellite"),
        BasemapStyle("hybrid", "Hybrid"),
        BasemapStyle("terrain", "Terrain"),
    )
}
