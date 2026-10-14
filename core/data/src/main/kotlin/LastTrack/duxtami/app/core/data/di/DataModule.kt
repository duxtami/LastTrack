package LastTrack.duxtami.app.core.data.di

import LastTrack.duxtami.app.core.data.db.LastTrackDatabase
import LastTrack.duxtami.app.core.data.db.RouteDao
import LastTrack.duxtami.app.core.data.db.SavedPlaceDao
import LastTrack.duxtami.app.core.data.prefs.DataStorePreferencesRepository
import LastTrack.duxtami.app.core.data.repository.RouteRepositoryImpl
import LastTrack.duxtami.app.core.data.repository.SavedPlacesRepositoryImpl
import LastTrack.duxtami.app.core.model.PreferencesRepository
import LastTrack.duxtami.app.core.model.RouteRepository
import LastTrack.duxtami.app.core.model.SavedPlacesRepository
import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds abstract fun routeRepository(impl: RouteRepositoryImpl): RouteRepository
    @Binds abstract fun preferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository
    @Binds abstract fun savedPlacesRepository(impl: SavedPlacesRepositoryImpl): SavedPlacesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): LastTrackDatabase =
        Room.databaseBuilder(context, LastTrackDatabase::class.java, "lasttrack.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun routeDao(db: LastTrackDatabase): RouteDao = db.routeDao()

    @Provides
    fun savedPlaceDao(db: LastTrackDatabase): SavedPlaceDao = db.savedPlaceDao()

    @Provides @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
