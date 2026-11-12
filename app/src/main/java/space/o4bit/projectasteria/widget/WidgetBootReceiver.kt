package space.o4bit.projectasteria.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.worker.LaunchReminderScheduler

class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Boot completed or app updated, scheduling widget updates")
                WidgetUpdateWorker.schedulePeriodicUpdates(context)
                WidgetAlarmManager.scheduleWidgetUpdate(context)
                IssAppWidgetProvider.updateAllWidgets(context)
                LaunchAppWidgetProvider.updateAllWidgets(context)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as? AsteriaApplication
                        if (app != null) {
                            val launches = app.database.launchDao().getAllLaunches().first()
                            val remindedIds = app.reminderPreferences.remindedLaunchIds.first()
                            LaunchReminderScheduler(WorkManager.getInstance(context)).reconcile(launches, remindedIds)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
    companion object {
        private const val TAG = "WidgetBootReceiver"
    }
}