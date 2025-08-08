package space.o4bit.projectasteria.ui.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.MainActivity
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture

/**
 * Helper class to create and show rich, space-themed notifications
 * with Material 3 design elements
 */
object SpaceNotificationBuilder {

    private const val CHANNEL_ID = "space_discovery_channel"
    private const val NOTIFICATION_ID = 1

    // Material 3 color values for notifications
    private val PRIMARY_COLOR = "#00B0FF".toColorInt() // CosmicAccent

    /**
     * Creates and displays a rich notification with the astronomy picture
     * styled with Material 3 design principles
     */
    suspend fun showAstronomyNotification(
        context: Context,
        enhancedPicture: EnhancedAstronomyPicture
    ) {
        // Create notification channel for Android 8.0+
        createNotificationChannel(context)

        // Create intent for when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("ASTRONOMY_PICTURE_DATE", enhancedPicture.astronomyPicture.date)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Try to load the image for a rich notification
        val imageBitmap = loadImageBitmap(context, enhancedPicture.astronomyPicture.url)

        // Build the notification with Material 3 styling
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(enhancedPicture.notificationTitle)
            .setContentText(enhancedPicture.astronomyPicture.title) // Only show title, not random fact
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(PRIMARY_COLOR)
            .setColorized(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(enhancedPicture.astronomyPicture.title) // Only show title
            )

        // Add large image if available
        imageBitmap?.let { bitmap ->
            builder.setLargeIcon(bitmap)
            builder.setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null as Bitmap?)
                .setBigContentTitle(enhancedPicture.notificationTitle)
                .setSummaryText(enhancedPicture.astronomyPicture.title) // Only show title, not space fact
            )
        }

        // Add action buttons for direct fullscreen viewing
        val viewIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "asteria://image/${enhancedPicture.astronomyPicture.date}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("VIEW_FULL_IMAGE", true)
            putExtra("ASTRONOMY_PICTURE_DATE", enhancedPicture.astronomyPicture.date)
            putExtra("OPEN_FULLSCREEN", true)
            putExtra("DISMISS_NOTIFICATION", true)
            putExtra("NOTIFICATION_ID", NOTIFICATION_ID)
        }
        val viewPendingIntent = PendingIntent.getActivity(
            context, 1, viewIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        builder.addAction(
            NotificationCompat.Action.Builder(
                IconCompat.createWithResource(context, R.drawable.arrowback),
                "View Full Image",
                viewPendingIntent
            ).build()
        )

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Handle permission not granted
            e.printStackTrace()
        }
    }

    /**
     * Load image bitmap asynchronously for the notification
     */
    private suspend fun loadImageBitmap(context: Context, imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Needed for .toBitmap()
                    .build()

                val result = loader.execute(request)
                result.drawable?.toBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Create the notification channel for Android 8.0+
     * with Material 3 theming
     */
    private fun createNotificationChannel(context: Context) {
        // No need to check SDK version since minSdk is 29 (Android 10) and O is 26
        val name = context.getString(R.string.notification_channel_name)
        val description = context.getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            this.description = description
            enableLights(true)
            lightColor = PRIMARY_COLOR
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
