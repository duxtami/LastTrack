package LasTrack.duxtami.app.map.mapbox

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.model.MapProviderId

class MapboxProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.MAPBOX

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Mapbox Satellite Hybrid"),
    )
}
