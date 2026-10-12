package space.o4bit.projectasteria.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CelestialBodyDao {
    @Query("SELECT * FROM celestial_bodies ORDER BY isPlanet DESC, semimajorAxis ASC, englishName ASC")
    fun getAllBodies(): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE isPlanet = 1 OR bodyType = 'Planet' COLLATE NOCASE ORDER BY semimajorAxis ASC")
    fun getPlanets(): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE bodyType = :bodyType COLLATE NOCASE ORDER BY englishName ASC")
    fun getBodiesByType(bodyType: String): Flow<List<CelestialBodyEntity>>

    @Query("SELECT * FROM celestial_bodies WHERE id = :id LIMIT 1")
    suspend fun getBodyById(id: String): CelestialBodyEntity?

    @Query("SELECT * FROM celestial_bodies WHERE englishName LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' OR bodyType LIKE '%' || :query || '%' OR alternativeName LIKE '%' || :query || '%' OR discoveredBy LIKE '%' || :query || '%' ORDER BY isPlanet DESC, englishName ASC")
    fun searchBodies(query: String): Flow<List<CelestialBodyEntity>>

    @Query("SELECT COUNT(*) FROM celestial_bodies")
    suspend fun countBodies(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodies(bodies: List<CelestialBodyEntity>)

    @Query("DELETE FROM celestial_bodies")
    suspend fun clearBodies()
}
