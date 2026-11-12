package space.o4bit.projectasteria.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
@Dao
interface AsteroidDao {
    @Query("SELECT * FROM asteroids ORDER BY CASE WHEN missDistanceKm < 0 THEN 1 ELSE 0 END, missDistanceKm ASC")
    fun getClosestAsteroids(): Flow<List<AsteroidEntity>>
    @Query("SELECT * FROM asteroids ORDER BY CASE WHEN missDistanceKm < 0 THEN 1 ELSE 0 END, missDistanceKm DESC")
    fun getFarthestAsteroids(): Flow<List<AsteroidEntity>>
    @Query("SELECT * FROM asteroids")
    fun getAllAsteroids(): Flow<List<AsteroidEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsteroids(asteroids: List<AsteroidEntity>)
    @Query("DELETE FROM asteroids")
    suspend fun clearAsteroids()
    @Transaction
    suspend fun replaceAll(asteroids: List<AsteroidEntity>) {
        clearAsteroids()
        insertAsteroids(asteroids)
    }
}