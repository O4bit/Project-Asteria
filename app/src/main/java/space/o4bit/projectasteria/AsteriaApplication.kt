package space.o4bit.projectasteria

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import space.o4bit.projectasteria.data.local.ApodDatabase

/**
 * Application class for Project Asteria
 * Handles initialization of libraries and services
 */
class AsteriaApplication : Application(), Configuration.Provider {

    val database: ApodDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ApodDatabase::class.java,
            "apod_database"
        )
        .fallbackToDestructiveMigration() // Useful for this dev phase where we changed the schema
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
        
        // CrashReportingUtils.initialize(this) is called in MainActivity
        // but it's often better to initialize crash reporting as early as possible.
        // We'll keep it in MainActivity for now to match existing logic, 
        // but relocating it to the flavor-specific initialize(this) here is also an option.
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
