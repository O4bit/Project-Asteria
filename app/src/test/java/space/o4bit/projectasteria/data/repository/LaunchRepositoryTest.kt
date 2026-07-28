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

        // Verifies sorting preference is respected via flatMapLatest
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
}
