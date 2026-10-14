package LasTrack.duxtami.app.map.osm

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.model.MapProviderId

/**
 * OpenStreetMap provider powered by osmdroid.
 * Fully functional out of the box with zero API keys required.
 */
class OpenStreetMapProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.OPEN_STREET_MAP

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Standard (Mapnik)"),
        BasemapStyle("opentopo", "OpenTopoMap"),
        BasemapStyle("usgs_topo", "USGS Topo"),
    )
}
