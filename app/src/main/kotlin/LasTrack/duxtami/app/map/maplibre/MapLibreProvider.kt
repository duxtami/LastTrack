package LasTrack.duxtami.app.map.maplibre

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.model.MapProviderId

class MapLibreProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.MAPLIBRE

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "MapLibre Liberty"),
        BasemapStyle("maplibre_topo", "MapLibre Topo (OpenTopoMap)"),
        BasemapStyle("maplibre_cyclosm", "MapLibre CyclOSM (Cycle Trails)"),
    )
}
