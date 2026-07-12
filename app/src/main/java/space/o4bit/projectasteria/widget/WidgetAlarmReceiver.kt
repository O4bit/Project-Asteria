package space.o4bit.projectasteria.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.util.Log

class WidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received alarm to update widget")

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

            WidgetAlarmManager.scheduleWidgetUpdate(context)
        }
    }

    companion object {
        private const val TAG = "WidgetAlarmReceiver"
    }
}
