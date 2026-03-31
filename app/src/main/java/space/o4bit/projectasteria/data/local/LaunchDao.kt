package space.o4bit.projectasteria.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LaunchDao {
    @Query("SELECT * FROM launches ORDER BY netMillis ASC")
    fun getOldestLaunches(): Flow<List<LaunchEntity>>

    @Query("SELECT * FROM launches ORDER BY netMillis DESC")
    fun getNewestLaunches(): Flow<List<LaunchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaunches(launches: List<LaunchEntity>)

    @Query("DELETE FROM launches")
    suspend fun clearLaunches()
    
    @Query("SELECT * FROM launches WHERE id = :id LIMIT 1")
    suspend fun getLaunchById(id: String): LaunchEntity?
    
    @Query("SELECT * FROM launches WHERE id = :id LIMIT 1")
    fun getLaunchByIdFlow(id: String): Flow<LaunchEntity?>
}
