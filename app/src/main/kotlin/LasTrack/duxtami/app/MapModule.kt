package LasTrack.duxtami.app

import LasTrack.duxtami.app.core.map.MapProviderRegistry
import LasTrack.duxtami.app.map.arcgis.ArcGisProvider
import LasTrack.duxtami.app.map.google.GoogleMapProvider
import LasTrack.duxtami.app.map.mapbox.MapboxProvider
import LasTrack.duxtami.app.map.osm.OpenStreetMapProvider
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
