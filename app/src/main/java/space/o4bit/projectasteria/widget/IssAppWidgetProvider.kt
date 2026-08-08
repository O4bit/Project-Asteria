package space.o4bit.projectasteria.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.MainActivity
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.repository.IssRepository

class IssAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating ISS widgets", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == Intent.ACTION_BOOT_COMPLETED
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, IssAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        for (id in appWidgetIds) {
                            updateWidget(context, appWidgetManager, id)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onReceive updating ISS widgets", e)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "IssAppWidgetProvider"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, IssAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    for (id in appWidgetIds) {
                        updateWidget(context, appWidgetManager, id)
                    }
                }
            }
        }

        private suspend fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.iss_app_widget_layout)

            // PendingIntent to launch app when tapping widget
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(android.R.id.background, pendingIntent)

            // Draw initial vector map frame
            val initialMap = IssWidgetMapRenderer.generateMapBitmap(context, 540, 290, null)
            views.setImageViewBitmap(R.id.iss_widget_map_image, initialMap)

            withContext(Dispatchers.Main) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            try {
                val repository = IssRepository()
                val position = repository.getIssPosition()

                // Render vector world map with live ISS target crosshair
                val liveMap = IssWidgetMapRenderer.generateMapBitmap(context, 540, 290, position)
                views.setImageViewBitmap(R.id.iss_widget_map_image, liveMap)

                views.setTextViewText(
                    R.id.iss_widget_lat,
                    String.format(java.util.Locale.US, "%.2f°", position.latitude)
                )
                views.setTextViewText(
                    R.id.iss_widget_lon,
                    String.format(java.util.Locale.US, "%.2f°", position.longitude)
                )
                views.setTextViewText(
                    R.id.iss_widget_alt,
                    "${String.format(java.util.Locale.US, "%.0f", position.altitude)} km"
                )
                views.setTextViewText(
                    R.id.iss_widget_speed,
                    "${String.format(java.util.Locale.US, "%.0f", position.velocity)} km/h"
                )
                views.setTextViewText(R.id.iss_widget_status, "LIVE")

                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch ISS position for widget", e)
                views.setTextViewText(R.id.iss_widget_status, "SYNCING")
                withContext(Dispatchers.Main) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
