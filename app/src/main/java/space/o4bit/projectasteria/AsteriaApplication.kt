package space.o4bit.projectasteria

import android.app.Application
import androidx.work.Configuration
import com.google.firebase.FirebaseApp

/**
 * Application class for Project Asteria
 * Handles initialization of libraries and services
 */
class AsteriaApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
    
    /**
     * Configure WorkManager for background tasks
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
