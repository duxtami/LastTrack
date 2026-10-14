package LastTrack.duxtami.app.map.arcgis

import LastTrack.duxtami.app.core.map.BasemapStyle
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.map.MapProvider
import LastTrack.duxtami.app.core.model.MapProviderId

class ArcGisProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.ARCGIS

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "World Street"),
        BasemapStyle("imagery", "World Imagery"),
        BasemapStyle("topographic", "World Topographic"),
    )
}
