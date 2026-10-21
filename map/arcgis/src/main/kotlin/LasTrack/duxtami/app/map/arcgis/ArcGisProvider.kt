package LasTrack.duxtami.app.map.arcgis

import LasTrack.duxtami.app.core.map.BasemapStyle
import LasTrack.duxtami.app.core.map.MapAvailability
import LasTrack.duxtami.app.core.map.MapProvider
import LasTrack.duxtami.app.core.model.MapProviderId

/**
 * Authentic Esri ArcGIS Provider featuring official World Street Map, World Topographic Map,
 * and Satellite Hybrid with transportation and place names.
 */
class ArcGisProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.ARCGIS

    override val availability: MapAvailability = MapAvailability.Ready

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "ArcGIS World Streets"),
        BasemapStyle("arcgis_topo", "ArcGIS Topographic"),
        BasemapStyle("arcgis_hybrid", "ArcGIS Satellite Hybrid"),
    )
}
