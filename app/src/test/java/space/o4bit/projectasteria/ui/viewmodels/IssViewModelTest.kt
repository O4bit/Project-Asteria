package space.o4bit.projectasteria.ui.viewmodels

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import space.o4bit.projectasteria.data.model.iss.IssPosition
import space.o4bit.projectasteria.data.repository.IssRepository

@OptIn(ExperimentalCoroutinesApi::class)
class IssViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var issRepository: IssRepository
    private lateinit var viewModel: IssViewModel

    private fun fakePosition(
        latitude: Double = 51.5074,
        longitude: Double = -0.1278,
        altitude: Double = 408.5,
        velocity: Double = 27600.0
    ) = IssPosition(
        name = "ISS",
        id = 25544,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        velocity = velocity,
        visibility = "daylight",
        footprint = 4504.0,
        timestamp = System.currentTimeMillis() / 1000,
        units = "kilometers"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        issRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasNoLocation_andIsNotLive() = runTest {
        coEvery { issRepository.getIssPosition() } coAnswers {
            kotlinx.coroutines.delay(10_000)
            fakePosition()
        }
        viewModel = IssViewModel(issRepository)
        val collectJob = launch { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.value
        assertNull("Initial location should be null", initial.location)
        assertFalse("Initial state should not be live", initial.isLive)
        assertNull("Initial state should have no error", initial.errorMessage)
        collectJob.cancel()
    }

    @Test
    fun successfulFetch_setsLocationAndIsLive() = runTest {
        val position = fakePosition()
        coEvery { issRepository.getIssPosition() } returns position
        viewModel = IssViewModel(issRepository)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceTimeBy(500)
        val state = viewModel.uiState.value
        assertNotNull("Location should be populated", state.location)
        assertEquals(position.latitude, state.location!!.latitude, 0.0001)
        assertEquals(position.longitude, state.location!!.longitude, 0.0001)
        assertTrue("State should be live after success", state.isLive)
        assertNull("No error on success", state.errorMessage)
        collectJob.cancel()
    }

    @Test
    fun networkFailure_setsErrorMessage_andIsNotLive() = runTest {
        coEvery { issRepository.getIssPosition() } throws RuntimeException("Network timeout")
        viewModel = IssViewModel(issRepository)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceTimeBy(500)
        val state = viewModel.uiState.value
        assertFalse("Should not be live on error", state.isLive)
        assertNotNull("Should have error message", state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Retrying", ignoreCase = true))
        collectJob.cancel()
    }

    @Test
    fun networkFailure_preservesPreviousLocation() = runTest {
        val position = fakePosition()
        coEvery { issRepository.getIssPosition() }
            .returns(position)
            .andThenThrows(RuntimeException("Connection lost"))
        viewModel = IssViewModel(issRepository)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceTimeBy(500)
        assertTrue(viewModel.uiState.value.isLive)
        advanceTimeBy(60500)
        val stateAfterFailure = viewModel.uiState.value
        assertFalse(stateAfterFailure.isLive)
        assertNotNull("Previous location preserved on error", stateAfterFailure.location)
        assertEquals(position.latitude, stateAfterFailure.location!!.latitude, 0.0001)
        collectJob.cancel()
    }

    @Test
    fun meaningfulChange_requiredToEmitNewState() = runTest {
        val pos1 = fakePosition(latitude = 51.5074, longitude = -0.1278)
        val pos2 = fakePosition(latitude = 51.50745, longitude = -0.12775)
        val pos3 = fakePosition(latitude = 51.515, longitude = -0.135)
        coEvery { issRepository.getIssPosition() }
            .returns(pos1)
            .andThen(pos2)
            .andThen(pos3)
        viewModel = IssViewModel(issRepository)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceTimeBy(500)
        val stateAfterFirst = viewModel.uiState.value
        assertNotNull(stateAfterFirst.location)
        assertEquals(pos1.latitude, stateAfterFirst.location!!.latitude, 0.0001)
        advanceTimeBy(60500)
        val stateAfterSecond = viewModel.uiState.value
        assertEquals(pos1.latitude, stateAfterSecond.location!!.latitude, 0.0001)
        advanceTimeBy(60500)
        val stateAfterThird = viewModel.uiState.value
        assertEquals(pos3.latitude, stateAfterThird.location!!.latitude, 0.0001)
        collectJob.cancel()
    }

    @Test
    fun refresh_resetsPollingLoop() = runTest {
        val position = fakePosition()
        coEvery { issRepository.getIssPosition() } returns position
        viewModel = IssViewModel(issRepository)
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceTimeBy(500)
        assertTrue(viewModel.uiState.value.isLive)
        viewModel.refresh()
        advanceTimeBy(500)
        assertTrue(viewModel.uiState.value.isLive)
        collectJob.cancel()
    }
}
