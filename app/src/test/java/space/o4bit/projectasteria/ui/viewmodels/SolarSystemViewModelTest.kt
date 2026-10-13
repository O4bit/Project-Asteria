package space.o4bit.projectasteria.ui.viewmodels

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import space.o4bit.projectasteria.data.local.CelestialBodyEntity
import space.o4bit.projectasteria.data.repository.SolarSystemRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SolarSystemViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SolarSystemRepository
    private lateinit var viewModel: SolarSystemViewModel

    private val earth = CelestialBodyEntity(
        id = "earth",
        name = "Earth",
        englishName = "Earth",
        isPlanet = true,
        bodyType = "Planet",
        semimajorAxis = 149598000.0,
        perihelion = 147000000.0,
        aphelion = 152000000.0,
        eccentricity = 0.0167,
        inclination = 0.0,
        massValue = 5.97,
        massExponent = 24,
        volValue = 1.08,
        volExponent = 12,
        density = 5.51,
        gravity = 9.8,
        escape = 11200.0,
        meanRadius = 6371.0,
        equaRadius = 6378.0,
        polarRadius = 6356.0,
        flattening = 0.0033,
        dimension = "",
        sideralOrbit = 365.25,
        sideralRotation = 24.0,
        aroundPlanetName = null,
        discoveredBy = "",
        discoveryDate = "",
        alternativeName = "",
        axialTilt = 23.44,
        avgTemp = 288.0,
        moonCount = 1,
        moonsSummary = "Moon"
    )

    private val moon = CelestialBodyEntity(
        id = "moon",
        name = "Moon",
        englishName = "Moon",
        isPlanet = false,
        bodyType = "Moon",
        semimajorAxis = 384400.0,
        perihelion = 363300.0,
        aphelion = 405500.0,
        eccentricity = 0.0549,
        inclination = 5.145,
        massValue = 7.346,
        massExponent = 22,
        volValue = 2.197,
        volExponent = 10,
        density = 3.344,
        gravity = 1.62,
        escape = 2380.0,
        meanRadius = 1737.4,
        equaRadius = 1738.1,
        polarRadius = 1736.0,
        flattening = 0.0012,
        dimension = "",
        sideralOrbit = 27.32,
        sideralRotation = 655.72,
        aroundPlanetName = "Earth",
        discoveredBy = "",
        discoveryDate = "",
        alternativeName = "Luna",
        axialTilt = 6.68,
        avgTemp = 250.0,
        moonCount = 0,
        moonsSummary = ""
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.initializeDatabase() } returns Unit
        coEvery { repository.refreshBodies(any()) } returns Unit
        every { repository.getBodiesByType("All") } returns flowOf(listOf(earth, moon))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_transitionsToSuccessWithBodies() = runTest {
        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SolarSystemUiState.Success)
        val success = state as SolarSystemUiState.Success
        assertEquals(2, success.bodies.size)
        assertEquals("All", success.selectedFilter)
    }

    @Test
    fun onFilterSelected_updatesSelectedFilterAndObserves() = runTest {
        every { repository.getBodiesByType("Planets") } returns flowOf(listOf(earth))

        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        viewModel.onFilterSelected("Planets")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SolarSystemUiState.Success)
        val success = state as SolarSystemUiState.Success
        assertEquals(1, success.bodies.size)
        assertEquals("Earth", success.bodies[0].englishName)
        assertEquals("Planets", success.selectedFilter)
    }

    @Test
    fun onSearchQueryChanged_queriesRepositorySearch() = runTest {
        every { repository.searchBodies("Moon") } returns flowOf(listOf(moon))

        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Moon")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SolarSystemUiState.Success)
        val success = state as SolarSystemUiState.Success
        assertEquals(1, success.bodies.size)
        assertEquals("Moon", success.bodies[0].englishName)
        assertEquals("Moon", success.searchQuery)
    }

    @Test
    fun refresh_invokesRepositoryRefresh() = runTest {
        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(atLeast = 2) { repository.refreshBodies() }
    }

    @Test
    fun searchWithFilter_filtersSearchResults() = runTest {
        every { repository.searchBodies("a") } returns flowOf(listOf(earth, moon))

        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        viewModel.onFilterSelected("Planets")
        viewModel.onSearchQueryChanged("a")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SolarSystemUiState.Success)
        val success = state as SolarSystemUiState.Success
        assertEquals(1, success.bodies.size)
        assertEquals("Earth", success.bodies[0].englishName)
    }

    @Test
    fun onFilterSelected_stars_observesStars() = runTest {
        val sirius = earth.copy(id = "sirius", name = "Sirius", englishName = "Sirius", isPlanet = false, bodyType = "Star")
        every { repository.getBodiesByType("Stars") } returns flowOf(listOf(sirius))

        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        viewModel.onFilterSelected("Stars")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SolarSystemUiState.Success)
        val success = state as SolarSystemUiState.Success
        assertEquals(1, success.bodies.size)
        assertEquals("Sirius", success.bodies[0].englishName)
        assertEquals("Stars", success.selectedFilter)
    }

    @Test
    fun onFilterSelected_exoplanets_observesExoplanets() = runTest {
        val trappist = earth.copy(id = "trappist1e", name = "TRAPPIST-1e", englishName = "TRAPPIST-1e", isPlanet = false, bodyType = "Exoplanet")
        every { repository.getBodiesByType("Exoplanets") } returns flowOf(listOf(trappist))

        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        viewModel.onFilterSelected("Exoplanets")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SolarSystemUiState.Success)
        val success = state as SolarSystemUiState.Success
        assertEquals(1, success.bodies.size)
        assertEquals("TRAPPIST-1e", success.bodies[0].englishName)
        assertEquals("Exoplanets", success.selectedFilter)
    }

    @Test
    fun onFilterSelected_deepSky_observesDeepSky() = runTest {
        val andromeda = earth.copy(id = "andromeda", name = "Andromeda Galaxy", englishName = "Andromeda Galaxy", isPlanet = false, bodyType = "Deep Sky")
        every { repository.getBodiesByType("Deep Sky") } returns flowOf(listOf(andromeda))

        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        viewModel.onFilterSelected("Deep Sky")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SolarSystemUiState.Success)
        val success = state as SolarSystemUiState.Success
        assertEquals(1, success.bodies.size)
        assertEquals("Andromeda Galaxy", success.bodies[0].englishName)
        assertEquals("Deep Sky", success.selectedFilter)
    }

    @Test
    fun searchWithFilter_filtersStarsAndExoplanetsAndDeepSky() = runTest {
        val sirius = earth.copy(id = "sirius", name = "Sirius", englishName = "Sirius", isPlanet = false, bodyType = "Star")
        val trappist = earth.copy(id = "trappist1e", name = "TRAPPIST-1e", englishName = "TRAPPIST-1e", isPlanet = false, bodyType = "Exoplanet")
        val andromeda = earth.copy(id = "andromeda", name = "Andromeda Galaxy", englishName = "Andromeda Galaxy", isPlanet = false, bodyType = "Deep Sky")
        every { repository.searchBodies("a") } returns flowOf(listOf(sirius, trappist, andromeda))

        viewModel = SolarSystemViewModel(repository)
        advanceUntilIdle()

        viewModel.onFilterSelected("Stars")
        viewModel.onSearchQueryChanged("a")
        advanceUntilIdle()

        val stateStars = viewModel.uiState.value as SolarSystemUiState.Success
        assertEquals(1, stateStars.bodies.size)
        assertEquals("Sirius", stateStars.bodies[0].englishName)

        viewModel.onFilterSelected("Exoplanets")
        advanceUntilIdle()

        val stateExo = viewModel.uiState.value as SolarSystemUiState.Success
        assertEquals(1, stateExo.bodies.size)
        assertEquals("TRAPPIST-1e", stateExo.bodies[0].englishName)

        viewModel.onFilterSelected("Deep Sky")
        advanceUntilIdle()

        val stateDeep = viewModel.uiState.value as SolarSystemUiState.Success
        assertEquals(1, stateDeep.bodies.size)
        assertEquals("Andromeda Galaxy", stateDeep.bodies[0].englishName)
    }
}
