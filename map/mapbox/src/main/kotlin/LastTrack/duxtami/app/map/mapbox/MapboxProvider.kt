package LastTrack.duxtami.app.map.mapbox

import LastTrack.duxtami.app.core.map.BasemapStyle
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.map.MapProvider
import LastTrack.duxtami.app.core.model.MapProviderId

class MapboxProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.MAPBOX

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Streets"),
        BasemapStyle("outdoors", "Outdoors"),
        BasemapStyle("light", "Light"),
        BasemapStyle("dark", "Dark"),
        BasemapStyle("satellite", "Satellite"),
    )
}
