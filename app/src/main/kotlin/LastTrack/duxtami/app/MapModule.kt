package LastTrack.duxtami.app

import LastTrack.duxtami.app.core.map.MapProviderRegistry
import LastTrack.duxtami.app.map.arcgis.ArcGisProvider
import LastTrack.duxtami.app.map.google.GoogleMapProvider
import LastTrack.duxtami.app.map.mapbox.MapboxProvider
import LastTrack.duxtami.app.map.osm.OpenStreetMapProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapModule {
    @Provides @Singleton
    fun mapProviderRegistry(): MapProviderRegistry =
        MapProviderRegistry(
            listOf(
                OpenStreetMapProvider(),
                GoogleMapProvider(),
                MapboxProvider(),
                ArcGisProvider(),
            )
        )
}
