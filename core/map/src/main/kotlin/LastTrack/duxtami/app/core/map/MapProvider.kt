package LastTrack.duxtami.app.core.map

import LastTrack.duxtami.app.core.model.DEFAULT_BASEMAP_ID
import LastTrack.duxtami.app.core.model.MapProviderId

/** A selectable map style. [id] is stable and persisted; [name] is the provider's own display name. */
data class BasemapStyle(val id: String, val name: String)

sealed interface MapAvailability {
    /** The provider can render maps right now. */
    data object Ready : MapAvailability

    /** The provider SDK or its credentials are missing. [missing] names what to add (e.g. "API key"). */
    data class NotConfigured(val missing: String) : MapAvailability

    /** The provider exists but can't be used right now. */
    data class Unavailable(val reason: String) : MapAvailability
}

/**
 * Provider-independent map contract. Phase 1-2 only describes providers (identity, availability, basemaps).
 * Rendering (a Composable per provider) is added in Phase 3 without touching route parsing or storage.
 */
interface MapProvider {
    val id: MapProviderId
    val availability: MapAvailability
    val basemaps: List<BasemapStyle>
}

data class MapOptions(
    val autoFitRoute: Boolean = true,
    val showStartEndMarkers: Boolean = true,
    val showWaypoints: Boolean = true,
    val showCompass: Boolean = true,
    val showScale: Boolean = true,
)

/** Everything a provider needs to render, expressed without any provider type. */
data class MapState(
    val provider: MapProviderId,
    val basemapId: String = DEFAULT_BASEMAP_ID,
    val routeId: String? = null,
    val options: MapOptions = MapOptions(),
)

class MapProviderRegistry(providers: List<MapProvider>) {
    private val byId = providers.associateBy { it.id }
    val all: List<MapProvider> = providers

    fun get(id: MapProviderId): MapProvider? = byId[id]

    fun basemapsFor(id: MapProviderId): List<BasemapStyle> = byId[id]?.basemaps.orEmpty()

    /** Returns the requested basemap if the provider offers it, else the provider's first (default) one. */
    fun resolveBasemap(id: MapProviderId, basemapId: String): BasemapStyle? {
        val list = basemapsFor(id)
        return list.firstOrNull { it.id == basemapId } ?: list.firstOrNull()
    }
}
