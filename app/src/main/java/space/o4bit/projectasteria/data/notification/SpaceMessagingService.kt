package space.o4bit.projectasteria.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import space.o4bit.projectasteria.MainActivity
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture

/**
 * Service to handle incoming Firebase Cloud Messaging notifications
 */
class SpaceMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Daily Space Image"
            val body = notification.body ?: "Check out today's amazing space discovery!"

            // Show the notification
            sendNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        // You would typically send this token to your backend server
        // For simplicity, we're just logging it here
        println("New FCM token: $token")
    }

    /**
     * Create and show a notification with the provided title and body
     */
    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.default_notification_channel_id)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(this)

        // Create notification channel (no need to check SDK version since minSdk is 29)
        val channel = NotificationChannel(
            channelId,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)

        try {
            notificationManager.notify(0, notificationBuilder.build())
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            e.printStackTrace()
        }
    }

    companion object {
        /**
         * Show a local notification with the enhanced astronomy picture
         */
        @Suppress("unused")
        fun showLocalNotification(context: Context, enhancedPicture: EnhancedAstronomyPicture) {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = context.getString(R.string.default_notification_channel_id)

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(enhancedPicture.notificationTitle)
                .setContentText(enhancedPicture.notificationBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(NotificationCompat.BigTextStyle().bigText(enhancedPicture.notificationBody))

            val notificationManager = NotificationManagerCompat.from(context)

            // Create notification channel (no need to check SDK version since minSdk is 29)
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)

            try {
                notificationManager.notify(0, notificationBuilder.build())
            } catch (e: SecurityException) {
                // Handle case where notification permission is not granted
                e.printStackTrace()
            }
        }
    }
}
