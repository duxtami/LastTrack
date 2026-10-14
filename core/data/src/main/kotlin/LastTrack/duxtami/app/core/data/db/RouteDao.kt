package LastTrack.duxtami.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id")
    fun observe(id: String): Flow<RouteEntity?>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun get(id: String): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: RouteEntity)

    @Query("UPDATE routes SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String): Int

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun delete(id: String): Int
}
