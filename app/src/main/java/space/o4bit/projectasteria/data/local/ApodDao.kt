package space.o4bit.projectasteria.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApodDao {
    @Query("SELECT * FROM apods ORDER BY date DESC")
    fun getAllApods(): Flow<List<ApodEntity>>

    @Query("SELECT * FROM apods WHERE date = :date")
    suspend fun getApodByDate(date: String): ApodEntity?

    @Query("SELECT * FROM apods ORDER BY date DESC LIMIT 1")
    suspend fun getLatestApod(): ApodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApod(apod: ApodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApods(apods: List<ApodEntity>)

    @Query("SELECT * FROM apods ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedApods(limit: Int, offset: Int): List<ApodEntity>
}

