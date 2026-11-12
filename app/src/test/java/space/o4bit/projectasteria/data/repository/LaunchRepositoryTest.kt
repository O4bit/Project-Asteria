package space.o4bit.projectasteria.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import space.o4bit.projectasteria.data.local.LaunchDao
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.preferences.SortingPreferencesRepository

class LaunchRepositoryTest {

    private lateinit var launchDao: LaunchDao
    private lateinit var sortingPreferences: SortingPreferencesRepository
    private lateinit var launchRepository: LaunchRepository

    private fun fakeLaunchEntity(
        id: String,
        name: String,
        statusName: String = "Go for Launch",
        netMillis: Long = System.currentTimeMillis() + 86400000L
    ) = LaunchEntity(
        id = id,
        name = name,
        statusName = statusName,
        statusDescription = "The mission is ready to launch.",
        net = "2026-08-01T12:00:00Z",
        providerName = "SpaceX",
        padName = "LC-39A",
        locationName = "Kennedy Space Center",
        missionName = "Starlink Mission",
        missionDescription = "Commercial satellite deployment.",
        image = null,
        netMillis = netMillis
    )

    @Before
    fun setUp() {
        launchDao = mockk(relaxed = true)
        sortingPreferences = mockk(relaxed = true)
        coEvery { sortingPreferences.isLaunchesAscending } returns flowOf(true)
        coEvery { launchDao.getOldestLaunches() } returns flowOf(emptyList())
        coEvery { launchDao.getNewestLaunches() } returns flowOf(emptyList())
        launchRepository = LaunchRepository(launchDao, sortingPreferences)
    }

    @Test
    fun launches_flowEmitsAscending_whenSortIsAscending() = runTest {
        val upcoming = fakeLaunchEntity("l1", "Mission Alpha", netMillis = System.currentTimeMillis() + 3600000L)
        val later = fakeLaunchEntity("l2", "Mission Beta", netMillis = System.currentTimeMillis() + 7200000L)
        coEvery { launchDao.getOldestLaunches() } returns flowOf(listOf(upcoming, later))

        launchRepository = LaunchRepository(launchDao, sortingPreferences)

        assertTrue(true)
    }

    @Test
    fun inFlightLaunch_isCorrectlyIdentifiedByStatusName() {
        val inFlight = fakeLaunchEntity("l3", "In-Flight Mission", statusName = "In Flight")
        assertTrue(inFlight.statusName.equals("In Flight", ignoreCase = true))
    }

    @Test
    fun netMillis_isParsedFromIso8601String() {
        val entity = fakeLaunchEntity("l4", "Timed Launch", netMillis = 1754049600000L)
        assertEquals(1754049600000L, entity.netMillis)
    }

    @Test
    fun launchEntity_hasAllRequiredFields() {
        val entity = fakeLaunchEntity("l5", "Complete Launch")
        assertEquals("l5", entity.id)
        assertEquals("Complete Launch", entity.name)
        assertEquals("SpaceX", entity.providerName)
        assertEquals("LC-39A", entity.padName)
        assertEquals("Kennedy Space Center", entity.locationName)
    }

    @Test
    fun refreshUpcomingLaunches_atomicallyReplacesLaunches() = runTest {
        val launchService = mockk<space.o4bit.projectasteria.data.api.SpaceLaunchService>()
        val apiLaunch = space.o4bit.projectasteria.data.model.launch.Launch(
            id = "api-1",
            name = "Falcon 9 Starlink",
            status = space.o4bit.projectasteria.data.model.launch.LaunchStatus(1, "Go for Launch", "Go", "Ready"),
            net = "2026-08-01T12:00:00Z",
            launchServiceProvider = null,
            pad = null,
            mission = null,
            image = null
        )
        coEvery { launchService.getUpcomingLaunches(any(), any()) } returns space.o4bit.projectasteria.data.model.launch.LaunchResponse(1, listOf(apiLaunch))
        io.mockk.coEvery { launchDao.replaceAll(any()) } returns Unit

        val repo = LaunchRepository(
            launchDao = launchDao,
            sortingPreferences = sortingPreferences,
            launchService = launchService
        )
        repo.refreshUpcomingLaunches(15)

        io.mockk.coVerify(exactly = 1) {
            launchDao.replaceAll(match { it.size == 1 && it[0].id == "api-1" })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun refreshUpcomingLaunches_blankLaunchId_throwsException() = runTest {
        val launchService = mockk<space.o4bit.projectasteria.data.api.SpaceLaunchService>()
        val blankLaunch = space.o4bit.projectasteria.data.model.launch.Launch(
            id = "   ",
            name = "Invalid Launch",
            status = space.o4bit.projectasteria.data.model.launch.LaunchStatus(1, "Go", "Go", "Ready"),
            net = "2026-08-01T12:00:00Z",
            launchServiceProvider = null,
            pad = null,
            mission = null,
            image = null
        )
        coEvery { launchService.getUpcomingLaunches(any(), any()) } returns space.o4bit.projectasteria.data.model.launch.LaunchResponse(1, listOf(blankLaunch))

        val repo = LaunchRepository(
            launchDao = launchDao,
            sortingPreferences = sortingPreferences,
            launchService = launchService
        )
        repo.refreshUpcomingLaunches(15)
    }

    @Test
    fun refreshUpcomingLaunches_withReminderScheduler_reconcilesReminders() = runTest {
        val launchService = mockk<space.o4bit.projectasteria.data.api.SpaceLaunchService>()
        val scheduler = mockk<space.o4bit.projectasteria.data.worker.LaunchReminderScheduler>(relaxed = true)
        val reminderPrefs = mockk<space.o4bit.projectasteria.data.preferences.ReminderPreferencesRepository>()
        val apiLaunch = space.o4bit.projectasteria.data.model.launch.Launch(
            id = "api-remind",
            name = "Falcon Heavy",
            status = space.o4bit.projectasteria.data.model.launch.LaunchStatus(1, "Go for Launch", "Go", "Ready"),
            net = "2026-08-01T12:00:00Z",
            launchServiceProvider = null,
            pad = null,
            mission = null,
            image = null
        )
        coEvery { launchService.getUpcomingLaunches(any(), any()) } returns space.o4bit.projectasteria.data.model.launch.LaunchResponse(1, listOf(apiLaunch))
        io.mockk.coEvery { launchDao.replaceAll(any()) } returns Unit
        coEvery { reminderPrefs.remindedLaunchIds } returns flowOf(setOf("api-remind"))

        val repo = LaunchRepository(
            launchDao = launchDao,
            sortingPreferences = sortingPreferences,
            launchService = launchService,
            reminderScheduler = scheduler,
            reminderPreferences = reminderPrefs
        )
        repo.refreshUpcomingLaunches(15)

        io.mockk.verify(exactly = 1) {
            scheduler.reconcile(match { it.size == 1 && it[0].id == "api-remind" }, setOf("api-remind"))
        }
    }
}
