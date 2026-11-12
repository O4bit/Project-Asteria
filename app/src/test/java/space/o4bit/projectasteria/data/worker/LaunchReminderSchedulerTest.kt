package space.o4bit.projectasteria.data.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import space.o4bit.projectasteria.data.local.LaunchEntity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class LaunchReminderSchedulerTest {

    private lateinit var workManager: WorkManager
    private val fixedNow = Instant.parse("2026-07-21T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private lateinit var scheduler: LaunchReminderScheduler

    private fun fakeLaunch(
        id: String,
        statusName: String = "Go for Launch",
        netMillis: Long = fixedNow.plus(Duration.ofHours(2)).toEpochMilli()
    ) = LaunchEntity(
        id = id,
        name = "Mission $id",
        statusName = statusName,
        statusDescription = "Flight ready",
        net = "2026-07-21T14:00:00Z",
        providerName = "NASA",
        padName = "LC-39A",
        locationName = "KSC",
        missionName = "Test Payload",
        missionDescription = "Orbital test",
        image = null,
        netMillis = netMillis
    )

    @Before
    fun setUp() {
        workManager = mockk(relaxed = true)
        scheduler = LaunchReminderScheduler(workManager, clock)
    }

    @Test
    fun schedule_futureLaunch_enqueuesUniqueWorkAndReturnsTrue() {
        val launch = fakeLaunch("launch-1", netMillis = fixedNow.plus(Duration.ofHours(1)).toEpochMilli())
        val result = scheduler.schedule(launch)

        assertTrue(result)
        verify {
            workManager.enqueueUniqueWork(
                LaunchReminderContract.workName("launch-1"),
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun schedule_pastLaunch_cancelsWorkAndReturnsFalse() {
        val launch = fakeLaunch("launch-2", netMillis = fixedNow.minus(Duration.ofMinutes(5)).toEpochMilli())
        val result = scheduler.schedule(launch)

        assertFalse(result)
        verify {
            workManager.cancelUniqueWork(LaunchReminderContract.workName("launch-2"))
        }
    }

    @Test
    fun schedule_zeroOrNegativeNetMillis_cancelsWorkAndReturnsFalse() {
        val launch = fakeLaunch("launch-3", netMillis = 0L)
        val result = scheduler.schedule(launch)

        assertFalse(result)
        verify {
            workManager.cancelUniqueWork(LaunchReminderContract.workName("launch-3"))
        }
    }

    @Test
    fun schedule_withinLeadWindow_cancelsWorkAndReturnsFalse() {
        val launch = fakeLaunch("launch-4", netMillis = fixedNow.plus(Duration.ofMinutes(5)).toEpochMilli())
        val result = scheduler.schedule(launch, Duration.ofMinutes(10))

        assertFalse(result)
        verify {
            workManager.cancelUniqueWork(LaunchReminderContract.workName("launch-4"))
        }
    }

    @Test
    fun cancel_invokesWorkManagerCancelUniqueWork() {
        scheduler.cancel("launch-5")

        verify {
            workManager.cancelUniqueWork(LaunchReminderContract.workName("launch-5"))
        }
    }

    @Test
    fun reconcile_missingLaunch_cancelsReminder() {
        scheduler.reconcile(emptyList(), setOf("launch-missing"))

        verify {
            workManager.cancelUniqueWork(LaunchReminderContract.workName("launch-missing"))
        }
    }

    @Test
    fun reconcile_completedLaunch_cancelsReminder() {
        val launch = fakeLaunch("launch-done", statusName = "Success")
        scheduler.reconcile(listOf(launch), setOf("launch-done"))

        verify {
            workManager.cancelUniqueWork(LaunchReminderContract.workName("launch-done"))
        }
    }

    @Test
    fun reconcile_pastLaunch_cancelsReminder() {
        val launch = fakeLaunch(
            "launch-past",
            statusName = "Go for Launch",
            netMillis = fixedNow.minus(Duration.ofSeconds(10)).toEpochMilli()
        )
        scheduler.reconcile(listOf(launch), setOf("launch-past"))

        verify {
            workManager.cancelUniqueWork(LaunchReminderContract.workName("launch-past"))
        }
    }

    @Test
    fun reconcile_activeUpcomingLaunch_reschedulesWork() {
        val launch = fakeLaunch(
            "launch-upcoming",
            statusName = "Go for Launch",
            netMillis = fixedNow.plus(Duration.ofHours(3)).toEpochMilli()
        )
        scheduler.reconcile(listOf(launch), setOf("launch-upcoming"))

        verify {
            workManager.enqueueUniqueWork(
                LaunchReminderContract.workName("launch-upcoming"),
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }
}
