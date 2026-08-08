package space.o4bit.projectasteria

import android.app.Application
import android.os.Build
import androidx.room.Room
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import space.o4bit.projectasteria.data.local.ApodDatabase

class AsteriaApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(GifDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                }
            }
            .build()
    }

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

    val backgroundPreferences: space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository by lazy {
        space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(newImageLoader())
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
