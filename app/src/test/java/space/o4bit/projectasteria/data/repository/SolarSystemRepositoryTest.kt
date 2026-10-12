package space.o4bit.projectasteria.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import space.o4bit.projectasteria.data.api.SolarSystemService
import space.o4bit.projectasteria.data.local.CelestialBodyDao
import space.o4bit.projectasteria.data.local.CelestialBodyEntity
import space.o4bit.projectasteria.data.model.solarsystem.BodiesResponse
import space.o4bit.projectasteria.data.model.solarsystem.CelestialBodyDto

class SolarSystemRepositoryTest {

    private lateinit var celestialBodyDao: CelestialBodyDao
    private lateinit var solarSystemService: SolarSystemService
    private lateinit var repository: SolarSystemRepository

    private fun fakeBodyEntity(id: String, name: String, isPlanet: Boolean = true, type: String = "Planet") =
        CelestialBodyEntity(
            id = id,
            name = name,
            englishName = name,
            isPlanet = isPlanet,
            bodyType = type,
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

    @Before
    fun setUp() {
        celestialBodyDao = mockk(relaxed = true)
        solarSystemService = mockk()
        repository = SolarSystemRepository(celestialBodyDao, solarSystemService)
    }

    @Test
    fun initializeDatabase_insertsDefaultBodies_whenDaoIsEmpty() = runTest {
        coEvery { celestialBodyDao.countBodies() } returns 0

        repository.initializeDatabase()

        coVerify(exactly = 1) { celestialBodyDao.insertBodies(any()) }
    }

    @Test
    fun initializeDatabase_doesNotInsert_whenDaoHasRecords() = runTest {
        coEvery { celestialBodyDao.countBodies() } returns 10

        repository.initializeDatabase()

        coVerify(exactly = 0) { celestialBodyDao.insertBodies(any()) }
    }

    @Test
    fun getBodyById_delegatesToDao() = runTest {
        val mars = fakeBodyEntity("mars", "Mars")
        coEvery { celestialBodyDao.getBodyById("mars") } returns mars

        val result = repository.getBodyById("mars")

        assertNotNull(result)
        assertEquals("Mars", result?.englishName)
        coVerify(exactly = 1) { celestialBodyDao.getBodyById("mars") }
    }

    @Test
    fun searchBodies_delegatesToDao() = runTest {
        val jupiter = fakeBodyEntity("jupiter", "Jupiter")
        every { celestialBodyDao.searchBodies("Jup") } returns flowOf(listOf(jupiter))

        val results = repository.searchBodies("Jup").first()

        assertEquals(1, results.size)
        assertEquals("Jupiter", results[0].englishName)
    }

    @Test
    fun searchBodies_blankQuery_returnsAllBodies() = runTest {
        val jupiter = fakeBodyEntity("jupiter", "Jupiter")
        every { celestialBodyDao.getAllBodies() } returns flowOf(listOf(jupiter))

        val results = repository.searchBodies("   ").first()

        assertEquals(1, results.size)
        io.mockk.verify(exactly = 0) { celestialBodyDao.searchBodies(any()) }
    }

    @Test
    fun getBodiesByType_planets_callsGetPlanets() = runTest {
        val earth = fakeBodyEntity("earth", "Earth")
        every { celestialBodyDao.getPlanets() } returns flowOf(listOf(earth))

        val results = repository.getBodiesByType("Planets").first()

        assertEquals(1, results.size)
        assertEquals("Earth", results[0].englishName)
        coVerify(exactly = 1) { celestialBodyDao.getPlanets() }
    }

    @Test
    fun refreshBodies_fetchesFromServiceAndInsertsToDao() = runTest {
        coEvery { celestialBodyDao.countBodies() } returns 5
        val dto = CelestialBodyDto(
            id = "saturn",
            name = "Saturne",
            englishName = "Saturn",
            isPlanet = true,
            bodyType = "Planet"
        )
        coEvery { solarSystemService.getBodies(any(), any()) } returns BodiesResponse(listOf(dto))

        repository.refreshBodies()

        coVerify { celestialBodyDao.insertBodies(match { list -> list.any { it.id == "saturn" } }) }
    }

    @Test
    fun refreshBodies_fallsBackGracefullyWhenServiceFails() = runTest {
        coEvery { celestialBodyDao.countBodies() } returns 0 andThen 0
        coEvery { solarSystemService.getBodies(any(), any()) } throws RuntimeException("Service unreachable")

        repository.refreshBodies()

        coVerify(atLeast = 1) { celestialBodyDao.insertBodies(any()) }
    }
}
