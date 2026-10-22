package LasTrack.duxtami.app.map.arcgis

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.model.MapProviderId

/**
 * Authentic Esri ArcGIS Provider featuring official high-resolution Satellite Hybrid imagery
 * with complete boundaries, place names, and transportation overlays.
 */
class ArcGisProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.ARCGIS

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "ArcGIS Satellite Hybrid"),
    )
}
