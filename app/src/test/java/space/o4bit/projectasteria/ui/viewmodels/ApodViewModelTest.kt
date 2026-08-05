package space.o4bit.projectasteria.ui.viewmodels

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import space.o4bit.projectasteria.data.model.AstronomyPicture
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.repository.SpaceRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ApodViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var spaceRepository: SpaceRepository
    private lateinit var viewModel: ApodViewModel

    private val fakePicture = EnhancedAstronomyPicture(
        astronomyPicture = AstronomyPicture(
            date = "2026-07-21",
            explanation = "A beautiful nebula.",
            hdUrl = null,
            mediaType = "image",
            serviceVersion = "v1",
            title = "Test Nebula",
            url = "https://example.com/test.jpg",
            copyright = "NASA",
            thumbnail = null
        ),
        shortFact = "Stars are massive.",
        notificationTitle = "Today: Test Nebula",
        notificationBody = "A beautiful nebula."
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        spaceRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading() = runTest {
        coEvery { spaceRepository.getTodaysAstronomyPicture() } coAnswers {
            kotlinx.coroutines.delay(100)
            fakePicture
        }
        viewModel = ApodViewModel(spaceRepository)
        assertTrue(viewModel.uiState.value is ApodUiState.Loading)
    }

    @Test
    fun afterSuccessfulLoad_stateIsSuccess() = runTest {
        coEvery { spaceRepository.getTodaysAstronomyPicture() } returns fakePicture
        viewModel = ApodViewModel(spaceRepository)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is ApodUiState.Success)
        assertEquals("Test Nebula", (state as ApodUiState.Success).picture.astronomyPicture.title)
    }

    @Test
    fun afterFailedLoad_stateIsError() = runTest {
        coEvery { spaceRepository.getTodaysAstronomyPicture() } throws RuntimeException("Network failure")
        viewModel = ApodViewModel(spaceRepository)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is ApodUiState.Error)
        // RuntimeException is not IOException/HttpException → maps to generic user message
        assertEquals(
            "Couldn't load today's space picture. Tap Retry to try again.",
            (state as ApodUiState.Error).message
        )
    }

    @Test
    fun retryAfterError_transitionsBackToSuccess() = runTest {
        coEvery { spaceRepository.getTodaysAstronomyPicture() }
            .throwsMany(listOf(RuntimeException("Network failure")))
            .andThen(fakePicture)
        viewModel = ApodViewModel(spaceRepository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ApodUiState.Error)
        viewModel.loadTodayApod()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ApodUiState.Success)
    }
}
