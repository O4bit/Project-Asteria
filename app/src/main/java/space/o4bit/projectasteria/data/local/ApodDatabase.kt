package space.o4bit.projectasteria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ApodEntity::class,
        LaunchEntity::class,
        AsteroidEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ApodDatabase : RoomDatabase() {
    abstract fun apodDao(): ApodDao
    abstract fun launchDao(): LaunchDao
    abstract fun asteroidDao(): AsteroidDao
}

