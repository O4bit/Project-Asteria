package space.o4bit.projectasteria.data.worker

import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.data.local.LaunchDao
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.preferences.ReminderPreferencesRepository
import space.o4bit.projectasteria.ui.components.SpaceNotificationBuilder

class LaunchReminderWorkerTest {

    private lateinit var app: AsteriaApplication
    private lateinit var launchDao: LaunchDao
    private lateinit var reminderPrefs: ReminderPreferencesRepository
    private lateinit var workerParams: WorkerParameters

    private fun fakeLaunch(
        id: String,
        name: String = "Test Launch",
        netMillis: Long = System.currentTimeMillis() + 900_000L
    ) = LaunchEntity(
        id = id,
        name = name,
        statusName = "Go for Launch",
        statusDescription = "Ready",
        net = "2026-07-21T12:00:00Z",
        providerName = "SpaceX",
        padName = "LC-39A",
        locationName = "KSC",
        missionName = "Starlink",
        missionDescription = "Satellites",
        image = null,
        netMillis = netMillis
    )

    @Before
    fun setUp() {
        app = mockk(relaxed = true)
        launchDao = mockk(relaxed = true)
        reminderPrefs = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        every { app.database.launchDao() } returns launchDao
        every { app.reminderPreferences } returns reminderPrefs
        mockkObject(SpaceNotificationBuilder)
        every { SpaceNotificationBuilder.showLaunchReminderNotification(any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun doWork_missingLaunchId_returnsFailure() = runTest {
        every { workerParams.inputData } returns Data.EMPTY
        val worker = LaunchReminderWorker(app, workerParams)

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
        verify(exactly = 0) { SpaceNotificationBuilder.showLaunchReminderNotification(any(), any(), any()) }
    }

    @Test
    fun doWork_deletedLaunch_returnsSuccessWithoutNotification() = runTest {
        val input = LaunchReminderContract.input("launch-deleted")
        every { workerParams.inputData } returns input
        coEvery { launchDao.getLaunchById("launch-deleted") } returns null
        coEvery { reminderPrefs.remindedLaunchIds } returns flowOf(setOf("launch-deleted"))

        val worker = LaunchReminderWorker(app, workerParams)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verify(exactly = 0) { SpaceNotificationBuilder.showLaunchReminderNotification(any(), any(), any()) }
    }

    @Test
    fun doWork_unremindedLaunch_returnsSuccessWithoutNotification() = runTest {
        val input = LaunchReminderContract.input("launch-unreminded")
        val launch = fakeLaunch("launch-unreminded")
        every { workerParams.inputData } returns input
        coEvery { launchDao.getLaunchById("launch-unreminded") } returns launch
        coEvery { reminderPrefs.remindedLaunchIds } returns flowOf(emptySet())

        val worker = LaunchReminderWorker(app, workerParams)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verify(exactly = 0) { SpaceNotificationBuilder.showLaunchReminderNotification(any(), any(), any()) }
    }

    @Test
    fun doWork_nonPositiveNetMillis_returnsSuccessWithoutNotification() = runTest {
        val input = LaunchReminderContract.input("launch-invalid-date")
        val launch = fakeLaunch("launch-invalid-date", netMillis = -1L)
        every { workerParams.inputData } returns input
        coEvery { launchDao.getLaunchById("launch-invalid-date") } returns launch
        coEvery { reminderPrefs.remindedLaunchIds } returns flowOf(setOf("launch-invalid-date"))

        val worker = LaunchReminderWorker(app, workerParams)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verify(exactly = 0) { SpaceNotificationBuilder.showLaunchReminderNotification(any(), any(), any()) }
    }

    @Test
    fun doWork_stalePastLaunch_returnsSuccessWithoutNotification() = runTest {
        val input = LaunchReminderContract.input("launch-past")
        val stalePastTime = System.currentTimeMillis() - 3_600_000L
        val launch = fakeLaunch("launch-past", netMillis = stalePastTime)
        every { workerParams.inputData } returns input
        coEvery { launchDao.getLaunchById("launch-past") } returns launch
        coEvery { reminderPrefs.remindedLaunchIds } returns flowOf(setOf("launch-past"))

        val worker = LaunchReminderWorker(app, workerParams)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verify(exactly = 0) { SpaceNotificationBuilder.showLaunchReminderNotification(any(), any(), any()) }
    }

    @Test
    fun doWork_rescheduledFarFutureLaunch_returnsSuccessWithoutNotification() = runTest {
        val input = LaunchReminderContract.input("launch-future")
        val farFutureTime = System.currentTimeMillis() + 86_400_000L
        val launch = fakeLaunch("launch-future", netMillis = farFutureTime)
        every { workerParams.inputData } returns input
        coEvery { launchDao.getLaunchById("launch-future") } returns launch
        coEvery { reminderPrefs.remindedLaunchIds } returns flowOf(setOf("launch-future"))

        val worker = LaunchReminderWorker(app, workerParams)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verify(exactly = 0) { SpaceNotificationBuilder.showLaunchReminderNotification(any(), any(), any()) }
    }

    @Test
    fun doWork_validLaunch_showsNotificationAndReturnsSuccess() = runTest {
        val input = LaunchReminderContract.input("launch-valid")
        val onTime = System.currentTimeMillis() + 600_000L
        val launch = fakeLaunch("launch-valid", name = "Falcon Heavy Starman", netMillis = onTime)
        every { workerParams.inputData } returns input
        coEvery { launchDao.getLaunchById("launch-valid") } returns launch
        coEvery { reminderPrefs.remindedLaunchIds } returns flowOf(setOf("launch-valid"))

        val worker = LaunchReminderWorker(app, workerParams)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verify(exactly = 1) {
            SpaceNotificationBuilder.showLaunchReminderNotification(
                context = app,
                launchName = "Falcon Heavy Starman",
                launchId = "launch-valid"
            )
        }
    }
}
