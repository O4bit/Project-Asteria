package space.o4bit.projectasteria.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import space.o4bit.projectasteria.data.repository.SpaceRepository
import space.o4bit.projectasteria.ui.components.SpaceNotificationBuilder
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Worker to fetch daily space data and show notifications
 * Scheduled to run at 8:00 AM local time daily
 */
class DailySpaceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = SpaceRepository()

    override suspend fun doWork(): Result = coroutineScope {
        try {
            // Fetch today's astronomy picture with space fact
            val enhancedPicture = repository.getTodaysAstronomyPicture()

            // Show a rich notification with the space discovery using our custom builder
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

        /**
         * Schedule the daily worker to run at 8:00 AM local time every day
         */
        fun schedule(context: Context) {
            // Cancel any existing work first
            WorkManager.getInstance(context).cancelUniqueWork(DAILY_SPACE_WORK_NAME)
            
            // Calculate initial delay to next 8:00 AM
            val initialDelay = calculateInitialDelayTo8AM()
            
            val request = PeriodicWorkRequestBuilder<DailySpaceWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_SPACE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }
        
        /**
         * Calculate the delay in milliseconds until the next 8:00 AM
         */
        private fun calculateInitialDelayTo8AM(): Long {
            val now = Calendar.getInstance()
            val next8AM = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If it's already past 8:00 AM today, schedule for tomorrow's 8:00 AM
                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            return next8AM.timeInMillis - now.timeInMillis
        }
    }
}
