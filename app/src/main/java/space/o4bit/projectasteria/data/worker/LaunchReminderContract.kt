package space.o4bit.projectasteria.data.worker

import androidx.work.Data
import androidx.work.workDataOf

object LaunchReminderContract {
    const val WORK_PREFIX = "launch-reminder:"
    const val TAG_PREFIX = "launch-reminder-tag:"
    const val KEY_LAUNCH_ID = "launch_id"
    const val KEY_LAUNCH_NAME = "launch_name"
    const val DEFAULT_LEAD_MINUTES = 15L

    fun workName(launchId: String): String = WORK_PREFIX + launchId
    fun tag(launchId: String): String = TAG_PREFIX + launchId

    fun input(launchId: String, launchName: String? = null): Data =
        if (launchName != null) {
            workDataOf(KEY_LAUNCH_ID to launchId, KEY_LAUNCH_NAME to launchName)
        } else {
            workDataOf(KEY_LAUNCH_ID to launchId)
        }
}
