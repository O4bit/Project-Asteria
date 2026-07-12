package space.o4bit.projectasteria.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import space.o4bit.projectasteria.data.preferences.NotificationPreferencesRepository
import space.o4bit.projectasteria.data.repository.SpaceRepository
import space.o4bit.projectasteria.ui.components.SpaceNotificationBuilder
import java.util.Calendar
import java.util.concurrent.TimeUnit

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.BackoffPolicy

class DailySpaceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = SpaceRepository(
        (context.applicationContext as space.o4bit.projectasteria.AsteriaApplication).database.apodDao()
    )

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val notificationPrefs = NotificationPreferencesRepository(applicationContext)
            val notificationsEnabled = notificationPrefs.dailyNotificationsEnabled.first()

            if (!notificationsEnabled) {
                return@coroutineScope Result.success()
            }

            val enhancedPicture = repository.getTodaysAstronomyPicture()

            SpaceNotificationBuilder.showAstronomyNotification(
                applicationContext,
                enhancedPicture
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val DAILY_SPACE_WORK_NAME = "daily_space_work"

        suspend fun schedule(context: Context) {
            val notificationPrefs = NotificationPreferencesRepository(context)
            val wifiOnly = notificationPrefs.wifiOnlyPrefetch.first()
            val hour = notificationPrefs.notificationHour.first()
            val minute = notificationPrefs.notificationMinute.first()

            WorkManager.getInstance(context).cancelUniqueWork(DAILY_SPACE_WORK_NAME)

            val initialDelay = calculateInitialDelayTo(hour, minute)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DailySpaceWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_SPACE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(DAILY_SPACE_WORK_NAME)
        }

        private fun calculateInitialDelayTo(hour: Int, minute: Int): Long {
            val now = Calendar.getInstance()
            val nextRun = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            return nextRun.timeInMillis - now.timeInMillis
        }
    }
}
