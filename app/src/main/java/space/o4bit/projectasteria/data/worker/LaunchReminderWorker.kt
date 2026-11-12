package space.o4bit.projectasteria.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.ui.components.SpaceNotificationBuilder

class LaunchReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val launchId = inputData.getString(LaunchReminderContract.KEY_LAUNCH_ID)
            ?: inputData.getString("LAUNCH_ID")
            ?: return Result.failure()

        val app = applicationContext as? AsteriaApplication ?: return Result.failure()
        val launch = app.database.launchDao().getLaunchById(launchId)
            ?: return Result.success()

        val isEnabled = app.reminderPreferences.remindedLaunchIds.first().contains(launchId)
        if (!isEnabled || launch.netMillis <= 0L) {
            return Result.success()
        }

        val now = System.currentTimeMillis()
        if (now > launch.netMillis + 30 * 60 * 1000L || launch.netMillis - now > 30 * 60 * 1000L) {
            return Result.success()
        }

        SpaceNotificationBuilder.showLaunchReminderNotification(
            context = applicationContext,
            launchName = launch.name,
            launchId = launch.id
        )
        return Result.success()
    }
}