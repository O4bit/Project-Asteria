package space.o4bit.projectasteria.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import space.o4bit.projectasteria.ui.components.SpaceNotificationBuilder

class LaunchReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val launchId = inputData.getString("launch_id") ?: return Result.failure()
        val launchName = inputData.getString("launch_name") ?: "Upcoming Launch"

        Log.d("LaunchReminderWorker", "Triggering reminder for $launchName ($launchId)")

        SpaceNotificationBuilder.showLaunchReminderNotification(
            context = applicationContext,
            launchName = launchName,
            launchId = launchId
        )

        return Result.success()
    }
}
