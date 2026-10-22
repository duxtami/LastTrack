package LasTrack.duxtami.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RouteEntity::class, SavedPlaceEntity::class], version = 3, exportSchema = false)
abstract class LastTrackDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun savedPlaceDao(): SavedPlaceDao
}
