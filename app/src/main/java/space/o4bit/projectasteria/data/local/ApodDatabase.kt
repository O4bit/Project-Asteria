package space.o4bit.projectasteria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ApodEntity::class,
        LaunchEntity::class,
        AsteroidEntity::class,
        CelestialBodyEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class ApodDatabase : RoomDatabase() {
    abstract fun apodDao(): ApodDao
    abstract fun launchDao(): LaunchDao
    abstract fun asteroidDao(): AsteroidDao
    abstract fun celestialBodyDao(): CelestialBodyDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS celestial_bodies (id TEXT NOT NULL, name TEXT NOT NULL, englishName TEXT NOT NULL, isPlanet INTEGER NOT NULL, bodyType TEXT NOT NULL, semimajorAxis REAL NOT NULL, perihelion REAL NOT NULL, aphelion REAL NOT NULL, eccentricity REAL NOT NULL, inclination REAL NOT NULL, massValue REAL, massExponent INTEGER, volValue REAL, volExponent INTEGER, density REAL NOT NULL, gravity REAL NOT NULL, escape REAL NOT NULL, meanRadius REAL NOT NULL, equaRadius REAL NOT NULL, polarRadius REAL NOT NULL, flattening REAL NOT NULL, dimension TEXT NOT NULL, sideralOrbit REAL NOT NULL, sideralRotation REAL NOT NULL, aroundPlanetName TEXT, discoveredBy TEXT NOT NULL, discoveryDate TEXT NOT NULL, alternativeName TEXT NOT NULL, axialTilt REAL NOT NULL, avgTemp REAL NOT NULL, moonCount INTEGER NOT NULL, moonsSummary TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN constellation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN spectralType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN distanceLightYears REAL NOT NULL DEFAULT 0.0")
            }
        }
        val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_4_7 = object : Migration(4, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN constellation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN spectralType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN distanceLightYears REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_3_7 = object : Migration(3, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS celestial_bodies (id TEXT NOT NULL, name TEXT NOT NULL, englishName TEXT NOT NULL, isPlanet INTEGER NOT NULL, bodyType TEXT NOT NULL, semimajorAxis REAL NOT NULL, perihelion REAL NOT NULL, aphelion REAL NOT NULL, eccentricity REAL NOT NULL, inclination REAL NOT NULL, massValue REAL, massExponent INTEGER, volValue REAL, volExponent INTEGER, density REAL NOT NULL, gravity REAL NOT NULL, escape REAL NOT NULL, meanRadius REAL NOT NULL, equaRadius REAL NOT NULL, polarRadius REAL NOT NULL, flattening REAL NOT NULL, dimension TEXT NOT NULL, sideralOrbit REAL NOT NULL, sideralRotation REAL NOT NULL, aroundPlanetName TEXT, discoveredBy TEXT NOT NULL, discoveryDate TEXT NOT NULL, alternativeName TEXT NOT NULL, axialTilt REAL NOT NULL, avgTemp REAL NOT NULL, moonCount INTEGER NOT NULL, moonsSummary TEXT NOT NULL, constellation TEXT NOT NULL DEFAULT '', description TEXT NOT NULL DEFAULT '', spectralType TEXT NOT NULL DEFAULT '', distanceLightYears REAL NOT NULL DEFAULT 0.0, imageUrl TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))")
            }
        }
        val MIGRATION_2_7 = object : Migration(2, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS celestial_bodies (id TEXT NOT NULL, name TEXT NOT NULL, englishName TEXT NOT NULL, isPlanet INTEGER NOT NULL, bodyType TEXT NOT NULL, semimajorAxis REAL NOT NULL, perihelion REAL NOT NULL, aphelion REAL NOT NULL, eccentricity REAL NOT NULL, inclination REAL NOT NULL, massValue REAL, massExponent INTEGER, volValue REAL, volExponent INTEGER, density REAL NOT NULL, gravity REAL NOT NULL, escape REAL NOT NULL, meanRadius REAL NOT NULL, equaRadius REAL NOT NULL, polarRadius REAL NOT NULL, flattening REAL NOT NULL, dimension TEXT NOT NULL, sideralOrbit REAL NOT NULL, sideralRotation REAL NOT NULL, aroundPlanetName TEXT, discoveredBy TEXT NOT NULL, discoveryDate TEXT NOT NULL, alternativeName TEXT NOT NULL, axialTilt REAL NOT NULL, avgTemp REAL NOT NULL, moonCount INTEGER NOT NULL, moonsSummary TEXT NOT NULL, constellation TEXT NOT NULL DEFAULT '', description TEXT NOT NULL DEFAULT '', spectralType TEXT NOT NULL DEFAULT '', distanceLightYears REAL NOT NULL DEFAULT 0.0, imageUrl TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))")
            }
        }
        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_7,
            MIGRATION_6_7,
            MIGRATION_2_7,
            MIGRATION_3_7,
            MIGRATION_4_7
        )
    }
}