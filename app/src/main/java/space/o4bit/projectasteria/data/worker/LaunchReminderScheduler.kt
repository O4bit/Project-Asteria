package space.o4bit.projectasteria.data.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import space.o4bit.projectasteria.data.local.LaunchEntity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class LaunchReminderScheduler(
    private val workManager: WorkManager,
    private val clock: Clock = Clock.systemUTC()
) {
    fun schedule(launch: LaunchEntity, leadDuration: Duration = Duration.ofMinutes(LaunchReminderContract.DEFAULT_LEAD_MINUTES)): Boolean {
        if (launch.netMillis <= 0L) {
            cancel(launch.id)
            return false
        }
        val reminderAt = Instant.ofEpochMilli(launch.netMillis).minus(leadDuration)
        val now = clock.instant()
        val delay = Duration.between(now, reminderAt)

        if (delay.isNegative || delay.isZero) {
            cancel(launch.id)
            return false
        }

        val request = OneTimeWorkRequestBuilder<LaunchReminderWorker>()
            .setInputData(LaunchReminderContract.input(launch.id, launch.name))
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag(LaunchReminderContract.tag(launch.id))
            .addTag(launch.id)
            .build()

        workManager.enqueueUniqueWork(
            LaunchReminderContract.workName(launch.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
        return true
    }

    fun cancel(launchId: String) {
        workManager.cancelUniqueWork(LaunchReminderContract.workName(launchId))
    }

    fun reconcile(launches: List<LaunchEntity>, remindedIds: Set<String>) {
        val launchMap = launches.associateBy { it.id }
        for (launchId in remindedIds) {
            val launch = launchMap[launchId]
            if (launch == null) {
                cancel(launchId)
            } else {
                val isCompleted = launch.statusName.equals("Success", ignoreCase = true) ||
                        launch.statusName.equals("Failure", ignoreCase = true)
                if (isCompleted || launch.netMillis <= clock.millis()) {
                    cancel(launchId)
                } else {
                    schedule(launch)
                }
            }
        }
    }
}
