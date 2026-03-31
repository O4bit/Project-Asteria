package space.o4bit.projectasteria.data.notification

import android.content.Context
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture

/**
 * No-op messaging service for FOSS builds (F-Droid)
 * Does not inherit from FirebaseMessagingService
 */
class SpaceMessagingService {
    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun showLocalNotification(context: Context, enhancedPicture: EnhancedAstronomyPicture) {
            // No-op for FOSS FCM - but we could implement local notifications here if needed
            // However, Project Asteria already uses WorkManager for daily notifications,
            // so FCM is redundant for FOSS.
        }
    }
}
