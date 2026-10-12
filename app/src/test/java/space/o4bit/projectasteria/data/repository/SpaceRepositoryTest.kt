package space.o4bit.projectasteria.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import space.o4bit.projectasteria.data.api.NasaApodService
import space.o4bit.projectasteria.data.local.ApodDao
import space.o4bit.projectasteria.data.local.ApodEntity

class SpaceRepositoryTest {

    private lateinit var apodDao: ApodDao
    private lateinit var nasaApodService: NasaApodService
    private lateinit var spaceRepository: SpaceRepository

    private fun fakeApodEntity(date: String, title: String) = ApodEntity(
        date = date,
        explanation = "Test explanation for $title",
        hdUrl = null,
        mediaType = "image",
        serviceVersion = null,
        title = title,
        url = "https://example.com/test.jpg",
        copyright = null,
        thumbnail = null,
        shortFact = "A space fact",
        notificationTitle = "Today: $title",
        notificationBody = "Test body"
    )

    @Before
    fun setUp() {
        apodDao = mockk(relaxed = true)
        nasaApodService = mockk()
        spaceRepository = SpaceRepository(apodDao, nasaApodService)
    }

    @Test
    fun getApodByDate_returnsCachedApod_whenDaoHasData() = runTest {
        val testEntity = fakeApodEntity("2026-07-21", "Test Nebula")
        coEvery { apodDao.getApodByDate("2026-07-21") } returns testEntity

        val result = spaceRepository.getApodByDate("2026-07-21")

        assertNotNull(result)
        assertEquals("Test Nebula", result?.astronomyPicture?.title)
        assertEquals("2026-07-21", result?.astronomyPicture?.date)
        coVerify(exactly = 1) { apodDao.getApodByDate("2026-07-21") }
    }

    @Test
    fun getApodByDate_returnsNull_whenDaoHasNoDataAndNetworkFails() = runTest {
        coEvery { apodDao.getApodByDate(any()) } returns null
        coEvery { nasaApodService.getAstronomyPictureByDate(any()) } throws
            RuntimeException("Network unavailable in tests")

        val result = spaceRepository.getApodByDate("2026-01-01")

        assertEquals(null, result)
    }

    @Test
    fun getPagedHistory_returnsEmptyList_whenNoCachedData() = runTest {
        coEvery { apodDao.getPagedApods(any(), any()) } returns emptyList()

        val result = spaceRepository.getPagedHistory(1, 20)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun searchApods_delegatesToDaoAndMapsResults() = runTest {
        val testEntity = fakeApodEntity("2026-07-21", "Searchable Star")
        coEvery { apodDao.searchApods("Star") } returns flowOf(listOf(testEntity))

        val results = spaceRepository.searchApods("Star").first()

        assertEquals(1, results.size)
        assertEquals("Searchable Star", results[0].astronomyPicture.title)
        assertEquals("2026-07-21", results[0].astronomyPicture.date)
    }

    @Test
    fun searchApods_blankQuery_returnsEmptyWithoutCallingDao() = runTest {
        val results = spaceRepository.searchApods("   ").first()

        assertEquals(0, results.size)
        io.mockk.verify(exactly = 0) { apodDao.searchApods(any()) }
    }

    @Test
    fun searchFavoriteApods_delegatesToDaoAndMapsResults() = runTest {
        val testEntity = fakeApodEntity("2026-07-21", "Favorite Moon")
        coEvery { apodDao.searchFavoriteApods("Moon") } returns flowOf(listOf(testEntity))

        val results = spaceRepository.searchFavoriteApods("Moon").first()

        assertEquals(1, results.size)
        assertEquals("Favorite Moon", results[0].astronomyPicture.title)
    }
}
