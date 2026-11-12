package space.o4bit.projectasteria.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
@Dao
interface LaunchDao {
    @Query("SELECT * FROM launches ORDER BY CASE WHEN netMillis <= 0 THEN 1 ELSE 0 END, netMillis ASC")
    fun getOldestLaunches(): Flow<List<LaunchEntity>>
    @Query("SELECT * FROM launches ORDER BY CASE WHEN netMillis <= 0 THEN 1 ELSE 0 END, netMillis DESC")
    fun getNewestLaunches(): Flow<List<LaunchEntity>>
    @Query("SELECT * FROM launches")
    fun getAllLaunches(): Flow<List<LaunchEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaunches(launches: List<LaunchEntity>)
    @Query("DELETE FROM launches")
    suspend fun clearLaunches()
    @Query("DELETE FROM launches WHERE id NOT IN (:preservedIds)")
    suspend fun clearUnpreservedLaunches(preservedIds: List<String>)
    @Query("SELECT * FROM launches WHERE id = :id LIMIT 1")
    suspend fun getLaunchById(id: String): LaunchEntity?
    @Query("SELECT * FROM launches WHERE id = :id LIMIT 1")
    fun getLaunchByIdFlow(id: String): Flow<LaunchEntity?>
    @Query("SELECT * FROM launches WHERE id IN (:ids)")
    suspend fun getLaunchesByIds(ids: List<String>): List<LaunchEntity>
    @Query("SELECT * FROM launches WHERE id IN (:ids)")
    fun getLaunchesByIdsFlow(ids: List<String>): Flow<List<LaunchEntity>>
    @Transaction
    suspend fun replaceAll(launches: List<LaunchEntity>, preservedIds: List<String> = emptyList()) {
        if (preservedIds.isEmpty()) {
            clearLaunches()
        } else {
            clearUnpreservedLaunches(preservedIds)
        }
        insertLaunches(launches)
    }
}