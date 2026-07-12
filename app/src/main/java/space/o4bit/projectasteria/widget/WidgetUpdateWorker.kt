package space.o4bit.projectasteria.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            Log.d(TAG, "WidgetUpdateWorker running")

            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(applicationContext, AsteriaAppWidget::class.java)
            )

            if (appWidgetIds.isNotEmpty()) {
                val updateIntent = Intent(applicationContext, AsteriaAppWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                applicationContext.sendBroadcast(updateIntent)
                Log.d(TAG, "Widget update broadcast sent")
            } else {
                Log.d(TAG, "No widgets to update")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val WIDGET_UPDATE_WORK_NAME = "widget_update_work"

        fun schedulePeriodicUpdates(context: Context) {
            Log.d(TAG, "Scheduling periodic widget updates")

            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun requestImmediateUpdate(context: Context) {
            Log.d(TAG, "Requesting immediate widget update")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, AsteriaAppWidget::class.java)
            )

            if (appWidgetIds.isNotEmpty()) {
                val updateIntent = Intent(context, AsteriaAppWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(updateIntent)
            }
        }
    }
}
