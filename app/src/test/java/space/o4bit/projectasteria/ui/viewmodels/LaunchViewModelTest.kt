package space.o4bit.projectasteria.ui.viewmodels

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository
import space.o4bit.projectasteria.data.repository.LaunchRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var launchRepository: LaunchRepository
    private lateinit var backgroundPrefs: BackgroundPreferencesRepository
    private lateinit var viewModel: LaunchViewModel

    private fun fakeLaunch(
        id: String,
        statusName: String = "Go for Launch",
        netMillis: Long = System.currentTimeMillis() + 86_400_000L  // tomorrow
    ) = LaunchEntity(
        id = id,
        name = "Mission $id",
        statusName = statusName,
        statusDescription = "Ready to launch.",
        net = "2026-08-01T12:00:00Z",
        providerName = "SpaceX",
        padName = "LC-39A",
        locationName = "Kennedy Space Center",
        missionName = "Cargo Mission $id",
        missionDescription = "ISS resupply.",
        image = null,
        netMillis = netMillis
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        launchRepository = mockk(relaxed = true)
        backgroundPrefs = mockk(relaxed = true)
        every { launchRepository.allLaunches } returns flowOf(emptyList())
        every { backgroundPrefs.hyperdriveThresholdMinutes } returns flowOf(1)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading() = runTest {
        viewModel = LaunchViewModel(launchRepository, backgroundPrefs)
        assertTrue(viewModel.uiState.value is LaunchUiState.Loading)
    }

    @Test
    fun emptyLaunchList_staysLoading() = runTest {
        every { launchRepository.allLaunches } returns flowOf(emptyList())
        viewModel = LaunchViewModel(launchRepository, backgroundPrefs)
        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(100)
        assertTrue(viewModel.uiState.value is LaunchUiState.Loading)
        job.cancel()
    }

    @Test
    fun nonEmptyLaunchList_transitionsToSuccess() = runTest {
        val launches = listOf(fakeLaunch("l1"), fakeLaunch("l2"))
        every { launchRepository.allLaunches } returns flowOf(launches)
        viewModel = LaunchViewModel(launchRepository, backgroundPrefs)
        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(100)
        val state = viewModel.uiState.value
        assertTrue("Expected Success, got $state", state is LaunchUiState.Success)
        assertEquals(2, (state as LaunchUiState.Success).launches.size)
        job.cancel()
    }

    @Test
    fun normalLaunchSpeed_isOne() = runTest {
        val launches = listOf(fakeLaunch("l1", netMillis = System.currentTimeMillis() + 86_400_000L))
        every { launchRepository.allLaunches } returns flowOf(launches)
        viewModel = LaunchViewModel(launchRepository, backgroundPrefs)
        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(100)
        val state = viewModel.uiState.value as LaunchUiState.Success
        assertEquals(1f, state.launchSpeedMultiplier)
        job.cancel()
    }

    @Test
    fun inFlightLaunch_triggersHyperdriveSpeed() = runTest {
        val inFlight = fakeLaunch(
            id = "l_inflight",
            statusName = "In Flight",
            netMillis = System.currentTimeMillis() - 300_000L   // launched 5 min ago
        )
        every { launchRepository.allLaunches } returns flowOf(listOf(inFlight))
        every { backgroundPrefs.hyperdriveThresholdMinutes } returns flowOf(1)
        viewModel = LaunchViewModel(launchRepository, backgroundPrefs)
        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(100)
        val state = viewModel.uiState.value as LaunchUiState.Success
        assertEquals(15f, state.launchSpeedMultiplier)
        job.cancel()
    }

    @Test
    fun immediateLaunch_triggersHyperdriveSpeed() = runTest {
        val imminent = fakeLaunch(
            id = "l_imminent",
            statusName = "Go for Launch",
            netMillis = System.currentTimeMillis() + 30_000L    // 30 seconds from now
        )
        every { launchRepository.allLaunches } returns flowOf(listOf(imminent))
        every { backgroundPrefs.hyperdriveThresholdMinutes } returns flowOf(1)
        viewModel = LaunchViewModel(launchRepository, backgroundPrefs)
        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(100)
        val state = viewModel.uiState.value as LaunchUiState.Success
        assertEquals(15f, state.launchSpeedMultiplier)
        job.cancel()
    }
}
