package space.o4bit.projectasteria.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver that gets called when device boots up or app is updated
 * Ensures widget updates are properly scheduled after device restarts
 */
class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",  // Qualcomm devices fast boot
            "com.htc.intent.action.QUICKBOOT_POWERON",  // HTC devices quick boot
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Boot completed or app updated, scheduling widget updates")
                WidgetUpdateWorker.schedulePeriodicUpdates(context)
                WidgetAlarmManager.scheduleWidgetUpdate(context)
            }
        }
    }

    companion object {
        private const val TAG = "WidgetBootReceiver"
    }
}
