package space.o4bit.projectasteria.ui.viewmodels

import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import space.o4bit.projectasteria.data.local.AsteroidEntity
import space.o4bit.projectasteria.data.repository.AsteroidRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AsteroidViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var asteroidRepository: AsteroidRepository
    private lateinit var viewModel: AsteroidViewModel

    private fun fakeEntity(id: String, isPotentiallyHazardous: Boolean = false) = AsteroidEntity(
        id = id,
        name = "Asteroid $id",
        nasaJplUrl = "https://ssd.jpl.nasa.gov/tools/sbdb_lookup.html#/?sstr=$id",
        absoluteMagnitudeH = 19.0,
        estimatedDiameterMinKm = 0.2,
        estimatedDiameterMaxKm = 0.5,
        isPotentiallyHazardous = isPotentiallyHazardous,
        isSentryObject = false,
        closeApproachDate = "2026-07-21",
        relativeVelocityKms = "10.0",
        relativeVelocityKmh = "36000.0",
        missDistanceKm = 750000.0,
        missDistanceLunar = "1.95"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        asteroidRepository = mockk(relaxed = true)
        every { asteroidRepository.allAsteroids } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading_beforeFlowEmits() = runTest {
        every { asteroidRepository.allAsteroids } returns flowOf(emptyList())
        viewModel = AsteroidViewModel(asteroidRepository)
        assertTrue(viewModel.uiState.value is AsteroidUiState.Loading)
    }

    @Test
    fun emptyList_stillTransitionsToSuccess() = runTest {
        every { asteroidRepository.allAsteroids } returns flowOf(emptyList())
        viewModel = AsteroidViewModel(asteroidRepository)
        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(100)
        val state = viewModel.uiState.value
        assertTrue("Expected Success, got $state", state is AsteroidUiState.Success)
        assertTrue((state as AsteroidUiState.Success).asteroids.isEmpty())
        job.cancel()
    }

    @Test
    fun nonEmptyList_populatesSuccessState() = runTest {
        val entities = listOf(fakeEntity("a1"), fakeEntity("a2", isPotentiallyHazardous = true))
        every { asteroidRepository.allAsteroids } returns flowOf(entities)
        viewModel = AsteroidViewModel(asteroidRepository)
        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(100)
        val state = viewModel.uiState.value
        assertTrue(state is AsteroidUiState.Success)
        val success = state as AsteroidUiState.Success
        assertTrue(success.asteroids.size == 2)
        assertTrue(success.asteroids.any { it.isPotentiallyHazardous })
        job.cancel()
    }

    @Test
    fun refresh_callsRepositoryRefresh() = runTest {
        every { asteroidRepository.allAsteroids } returns flowOf(emptyList())
        viewModel = AsteroidViewModel(asteroidRepository)
        val job = launch { viewModel.uiState.collect {} }
        advanceTimeBy(100)
        viewModel.refresh()
        advanceTimeBy(100)
        coVerify(atLeast = 1) { asteroidRepository.refreshTodaysAsteroids() }
        job.cancel()
    }
}
