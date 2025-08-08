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
import java.util.concurrent.TimeUnit

/**
 * Worker to fetch daily space data and show notifications
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
         * Schedule the daily worker to run once per day
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailySpaceWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_SPACE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
