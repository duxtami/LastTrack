package LastTrack.duxtami.app.map.arcgis

import LastTrack.duxtami.app.core.map.BasemapStyle
import LastTrack.duxtami.app.core.map.MapAvailability
import LastTrack.duxtami.app.core.map.MapProvider
import LastTrack.duxtami.app.core.model.MapProviderId

/**
 * Phase 1-2 placeholder: describes the provider but renders nothing and has no SDK dependency.
 * It always reports [MapAvailability.NotConfigured]; the real SDK integration replaces this class later.
 */
class ArcGisProvider : MapProvider {
    override val id: MapProviderId = MapProviderId.ARCGIS

    override val availability: MapAvailability = MapAvailability.NotConfigured("ArcGIS API key")

    override val basemaps: List<BasemapStyle> = listOf(
        BasemapStyle("default", "Default"),
        BasemapStyle("dark_gray_canvas", "Dark Gray Canvas"),
        BasemapStyle("imagery", "Imagery"),
        BasemapStyle("imagery_hybrid", "Imagery Hybrid"),
        BasemapStyle("light_gray_canvas", "Light Gray Canvas"),
        BasemapStyle("navigation", "Navigation"),
        BasemapStyle("oceans", "Oceans"),
        BasemapStyle("streets", "Streets"),
        BasemapStyle("streets_night", "Streets Night"),
        BasemapStyle("topographic", "Topographic"),
    )
}
