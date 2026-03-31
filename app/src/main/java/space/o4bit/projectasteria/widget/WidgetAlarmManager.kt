package space.o4bit.projectasteria.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Helper class to manage AlarmManager for widget updates
 * Uses different approaches based on Android version
 */
object WidgetAlarmManager {
    private const val TAG = "WidgetAlarmManager"
    private const val WIDGET_UPDATE_REQUEST_CODE = 42

    /**
     * Schedule a widget update using AlarmManager
     * Uses exact alarms when available, otherwise uses inexact alarms
     */
    fun scheduleWidgetUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WIDGET_UPDATE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set alarm for 4 AM tomorrow
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        try {
            when {
                // Android 12+ (S/API 31+): Use exact alarms if permission is granted
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled exact alarm on Android 12+ for widget update at ${calendar.time}")
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled inexact alarm on Android 12+ for widget update at ${calendar.time}")
                    }
                }
                // Android 6.0-11 (M/API 23 to R/API 30): Use exact alarms with Doze allowance
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for widget update at ${calendar.time}")
                }
                // Android 4.4-5.1 (KitKat/API 19 to Lollipop/API 22): Use exact alarms
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for widget update at ${calendar.time}")
                }
                // Older devices: Fall back to inexact alarms
                else -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact alarm for widget update at ${calendar.time}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for widget update", e)
            // Fallback to basic alarm
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled fallback inexact alarm for widget update at ${calendar.time}")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule even basic alarm for widget update", e2)
            }
        }
    }

    /**
     * Cancel any pending widget update alarms
     */
    fun cancelWidgetUpdateAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WIDGET_UPDATE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "Cancelled widget update alarm")
        }
    }
}
