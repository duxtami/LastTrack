package LasTrack.duxtami.app.map.geoapify

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.model.MapProviderId

class GeoapifyProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.GEOAPIFY

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Geoapify OSM Bright"),
        BasemapStyle("geoapify_dark", "Geoapify Dark Matter"),
        BasemapStyle("geoapify_positron", "Geoapify Positron (Light)"),
        BasemapStyle("geoapify_klokan", "Geoapify KlokanTech Basic"),
    )
}
