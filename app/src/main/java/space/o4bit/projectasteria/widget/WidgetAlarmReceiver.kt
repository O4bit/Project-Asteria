package space.o4bit.projectasteria.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.util.Log

/**
 * Receiver that handles scheduled alarms for widget updates
 * Used as a backup mechanism alongside WorkManager for improved reliability
 */
class WidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received alarm to update widget")
        
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, AsteriaAppWidget::class.java)
        )
        
        if (appWidgetIds.isNotEmpty()) {
            // Trigger widget update
            val updateIntent = Intent(context, AsteriaAppWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
            
            // Schedule the next update
            WidgetAlarmManager.scheduleWidgetUpdate(context)
        }
    }
    
    companion object {
        private const val TAG = "WidgetAlarmReceiver"
    }
}
