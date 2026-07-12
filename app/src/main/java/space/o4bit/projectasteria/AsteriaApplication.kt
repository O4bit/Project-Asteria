package space.o4bit.projectasteria

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import space.o4bit.projectasteria.data.local.ApodDatabase

class AsteriaApplication : Application(), Configuration.Provider {

    val database: ApodDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ApodDatabase::class.java,
            "apod_database"
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }

    val sortingPreferences: space.o4bit.projectasteria.data.preferences.SortingPreferencesRepository by lazy {
        space.o4bit.projectasteria.data.preferences.SortingPreferencesRepository(applicationContext)
    }

    val reminderPreferences: space.o4bit.projectasteria.data.preferences.ReminderPreferencesRepository by lazy {
        space.o4bit.projectasteria.data.preferences.ReminderPreferencesRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
