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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Create notification channel for Android 8.0+
        createNotificationChannel(context)

        // Create intent for when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = Intent.ACTION_VIEW
            data = "asteria://image/${enhancedPicture.astronomyPicture.date}".toUri()
            putExtra("ASTRONOMY_PICTURE_DATE", enhancedPicture.astronomyPicture.date)
            putExtra("OPEN_FULLSCREEN", enhancedPicture.astronomyPicture.mediaType == "video")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val apod = enhancedPicture.astronomyPicture
        val title = apod.title
        
        // Strip HTML and truncate sensibly for the notification
        val rawExplanation = apod.explanation ?: ""
        val cleanExplanation = space.o4bit.projectasteria.utils.TextUtils.stripHtml(rawExplanation)
        val snippet = if (cleanExplanation.length > 100) cleanExplanation.take(100) + "..." else cleanExplanation

        // Build the notification with Material 3 styling
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name) // Your custom monochrome notification icon
            .setContentTitle("Today's Space Discovery")
            .setContentText(title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(PRIMARY_COLOR)
            .setColorized(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)

        if (apod.mediaType == "video") {
            builder.setStyle(NotificationCompat.BigTextStyle()
                .bigText("🎥 $title\n\n$snippet")
            )
        } else {
            // Try to load the image for a rich notification
            val imageUrl = apod.url ?: apod.hdUrl
            val imageBitmap = if (imageUrl != null) loadImageBitmap(context, imageUrl) else null
            
            if (imageBitmap != null) {
                builder.setLargeIcon(imageBitmap)
                builder.setStyle(NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setBigContentTitle(title)
                    .setSummaryText(snippet)
                )
            } else {
                builder.setStyle(NotificationCompat.BigTextStyle()
                    .bigText(snippet)
                )
            }
            
            // Add action buttons for direct fullscreen viewing
            val viewIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "asteria://image/${apod.date}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("VIEW_FULL_IMAGE", true)
                putExtra("ASTRONOMY_PICTURE_DATE", apod.date)
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
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
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
        val name = "APOD Updates"
        val description = "Daily Astronomy Pictures"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            this.description = description
            enableLights(true)
            lightColor = PRIMARY_COLOR
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val launchChannel = NotificationChannel("launch_reminders", "Launch Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            this.description = "Reminders for scheduled orbital launches"
            enableLights(true)
            lightColor = PRIMARY_COLOR
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(launchChannel)
    }

    fun showLaunchReminderNotification(context: Context, launchName: String, launchId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            launchId.hashCode(), // Unique ID per launch
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, "launch_reminders")
            .setSmallIcon(R.drawable.baseline_widgets_24) // Use a generic icon if specific one isn't present
            .setContentTitle("🚀 Upcoming Launch!")
            .setContentText("$launchName is launching soon!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Get ready! $launchName is launching within 15 minutes! Open app for live timer."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(PRIMARY_COLOR)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(launchId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
