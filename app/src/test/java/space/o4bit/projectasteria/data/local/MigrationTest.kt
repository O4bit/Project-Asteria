package space.o4bit.projectasteria.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class MigrationTest {

    @Test
    fun migration2To3_addsIsFavoriteColumn() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        assertEquals(2, ApodDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, ApodDatabase.MIGRATION_2_3.endVersion)
        ApodDatabase.MIGRATION_2_3.migrate(db)
        verify(exactly = 1) {
            db.execSQL("ALTER TABLE apods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Test
    fun migration3To4_createsCelestialBodiesTable() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        assertEquals(3, ApodDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, ApodDatabase.MIGRATION_3_4.endVersion)
        ApodDatabase.MIGRATION_3_4.migrate(db)
        verify(exactly = 1) {
            db.execSQL(match { it.startsWith("CREATE TABLE IF NOT EXISTS celestial_bodies") })
        }
    }

    @Test
    fun migration4To5_addsCelestialBodyColumns() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        assertEquals(4, ApodDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, ApodDatabase.MIGRATION_4_5.endVersion)
        ApodDatabase.MIGRATION_4_5.migrate(db)
        verify(exactly = 1) {
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN constellation TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN spectralType TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN distanceLightYears REAL NOT NULL DEFAULT 0.0")
        }
    }

    @Test
    fun migration5To7_addsImageUrlColumn() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        assertEquals(5, ApodDatabase.MIGRATION_5_7.startVersion)
        assertEquals(7, ApodDatabase.MIGRATION_5_7.endVersion)
        ApodDatabase.MIGRATION_5_7.migrate(db)
        verify(exactly = 1) {
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    @Test
    fun migration6To7_addsImageUrlColumn() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        assertEquals(6, ApodDatabase.MIGRATION_6_7.startVersion)
        assertEquals(7, ApodDatabase.MIGRATION_6_7.endVersion)
        ApodDatabase.MIGRATION_6_7.migrate(db)
        verify(exactly = 1) {
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    @Test
    fun migration3To7_createsFullCelestialBodiesTable() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        assertEquals(3, ApodDatabase.MIGRATION_3_7.startVersion)
        assertEquals(7, ApodDatabase.MIGRATION_3_7.endVersion)
        ApodDatabase.MIGRATION_3_7.migrate(db)
        verify(exactly = 1) {
            db.execSQL(match { it.startsWith("CREATE TABLE IF NOT EXISTS celestial_bodies") && it.contains("imageUrl") })
        }
    }

    @Test
    fun migration4To7_addsAllNewColumns() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        assertEquals(4, ApodDatabase.MIGRATION_4_7.startVersion)
        assertEquals(7, ApodDatabase.MIGRATION_4_7.endVersion)
        ApodDatabase.MIGRATION_4_7.migrate(db)
        verify(exactly = 1) {
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN constellation TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN spectralType TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN distanceLightYears REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE celestial_bodies ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    @Test
    fun migration2To7_addsIsFavoriteAndCreatesTable() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        assertEquals(2, ApodDatabase.MIGRATION_2_7.startVersion)
        assertEquals(7, ApodDatabase.MIGRATION_2_7.endVersion)
        ApodDatabase.MIGRATION_2_7.migrate(db)
        verify(exactly = 1) {
            db.execSQL("ALTER TABLE apods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL(match { it.startsWith("CREATE TABLE IF NOT EXISTS celestial_bodies") && it.contains("imageUrl") })
        }
    }

    @Test
    fun allMigrations_hasExpectedCount() {
        assertEquals(8, ApodDatabase.ALL_MIGRATIONS.size)
    }
}
