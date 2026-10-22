package LasTrack.duxtami.app.map.osm

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.model.MapProviderId

/**
 * OpenStreetMap provider powered by osmdroid.
 * Features Standard (Mapnik), White / Light, and Dark Mode styles.
 */
class OpenStreetMapProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.OPEN_STREET_MAP

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Standard (Mapnik)"),
        BasemapStyle("osm_white", "White / Light (Positron)"),
        BasemapStyle("osm_dark", "Dark Mode (Dark Matter)"),
    )
}
