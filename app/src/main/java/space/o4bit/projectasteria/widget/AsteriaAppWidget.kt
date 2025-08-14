package space.o4bit.projectasteria.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.MainActivity
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.repository.SpaceRepository
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Implementation of App Widget functionality for Project Asteria
 * Exactly replicates the main screen's astronomy picture component
 */
class AsteriaAppWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "space.o4bit.projectasteria.ACTION_REFRESH"
        const val ACTION_OPEN_APP = "space.o4bit.projectasteria.ACTION_OPEN_APP"
        const val ACTION_SHARE = "space.o4bit.projectasteria.ACTION_SHARE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
    }

    override fun onDisabled(context: Context) {
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, AsteriaAppWidget::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            ACTION_SHARE -> {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Check out this amazing astronomy discovery from Project Asteria!")
                    type = "text/plain"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val shareChooser = Intent.createChooser(shareIntent, "Share Astronomy Picture")
                shareChooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(shareChooser)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.asteria_app_widget)

        val openAppIntent = Intent(WidgetClickReceiver.ACTION_OPEN_APP_FROM_WIDGET).apply {
            setPackage(context.packageName)
        }
        val openAppPendingIntent = PendingIntent.getBroadcast(
            context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_main_card, openAppPendingIntent)

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        val currentDate = dateFormat.format(java.util.Date())
        views.setTextViewText(R.id.widget_date, currentDate)

        views.setTextViewText(R.id.widget_title, "Project Asteria")
        views.setTextViewText(R.id.widget_fact, "Loading space discoveries...")

        appWidgetManager.updateAppWidget(appWidgetId, views)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SpaceRepository()
                val enhancedPicture = repository.getTodaysAstronomyPicture()

                CoroutineScope(Dispatchers.Main).launch {
                    val apod = enhancedPicture.astronomyPicture
                    views.setTextViewText(R.id.widget_title, apod.title)
                    views.setTextViewText(R.id.widget_fact, enhancedPicture.shortFact)

                    val imageUrl = apod.url ?: apod.hdUrl
                    val isVideo = apod.mediaType.equals("video", ignoreCase = true)
                    if (!isVideo && imageUrl != null) {
                        try {
                            val loader = ImageLoader(context)
                            val request = ImageRequest.Builder(context)
                                .data(imageUrl)
                .allowHardware(false)
                                .build()
                            val result = loader.execute(request)
                            if (result is SuccessResult) {
                                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                if (bitmap != null) {
                                    views.setImageViewBitmap(R.id.widget_image, bitmap)
                                }
                            }
            } catch (_: Exception) {
                        }
                    }
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    views.setTextViewText(R.id.widget_title, "Project Asteria")
                    views.setTextViewText(R.id.widget_fact, "Tap to explore space discoveries")
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
