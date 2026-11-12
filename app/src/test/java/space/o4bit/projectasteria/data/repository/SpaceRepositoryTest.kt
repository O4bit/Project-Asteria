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
        coEvery { nasaApodService.getDirectAstronomyPictureByDate(any()) } throws
            RuntimeException("Direct network unavailable in tests")

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

    @Test
    fun getApodByDate_fetchesFromNetworkAndInsertsToDao_whenDaoHasNoData() = runTest {
        val remoteApod = space.o4bit.projectasteria.data.model.AstronomyPicture(
            date = "1995-06-16",
            explanation = "First APOD",
            hdUrl = null,
            mediaType = "image",
            title = "Neutron Star",
            url = "https://apod.nasa.gov/apod/image/neutron_star.jpg"
        )
        coEvery { apodDao.getApodByDate("1995-06-16") } returns null
        coEvery { nasaApodService.getAstronomyPictureByDate("1995-06-16") } returns remoteApod

        val result = spaceRepository.getApodByDate("1995-06-16")

        assertNotNull(result)
        assertEquals("Neutron Star", result?.astronomyPicture?.title)
        assertEquals("1995-06-16", result?.astronomyPicture?.date)
        coVerify(exactly = 1) { apodDao.insertApod(any()) }
    }

    @Test
    fun getApodByDate_handlesLeapDayBoundary() = runTest {
        val testEntity = fakeApodEntity("1996-02-29", "Leap Day APOD")
        coEvery { apodDao.getApodByDate("1996-02-29") } returns testEntity

        val result = spaceRepository.getApodByDate("1996-02-29")

        assertNotNull(result)
        assertEquals("Leap Day APOD", result?.astronomyPicture?.title)
        assertEquals("1996-02-29", result?.astronomyPicture?.date)
    }

    @Test
    fun getApodByDate_fallsBackToDirectNasaApi_whenProxyReturnsWrongDate() = runTest {
        val proxyMismatchApod = space.o4bit.projectasteria.data.model.AstronomyPicture(
            date = "2026-10-14",
            explanation = "Today APOD from proxy",
            hdUrl = null,
            mediaType = "image",
            title = "Wrong Date APOD",
            url = "https://example.com/today.jpg"
        )
        val directTargetApod = space.o4bit.projectasteria.data.model.AstronomyPicture(
            date = "2024-04-08",
            explanation = "Total Solar Eclipse",
            hdUrl = null,
            mediaType = "image",
            title = "Great American Eclipse",
            url = "https://example.com/eclipse.jpg"
        )
        coEvery { apodDao.getApodByDate("2024-04-08") } returns null
        coEvery { nasaApodService.getAstronomyPictureByDate("2024-04-08") } returns proxyMismatchApod
        coEvery { nasaApodService.getDirectAstronomyPictureByDate("2024-04-08") } returns directTargetApod

        val result = spaceRepository.getApodByDate("2024-04-08")

        assertNotNull(result)
        assertEquals("Great American Eclipse", result?.astronomyPicture?.title)
        assertEquals("2024-04-08", result?.astronomyPicture?.date)
        coVerify(exactly = 1) { nasaApodService.getDirectAstronomyPictureByDate("2024-04-08") }
        coVerify(exactly = 1) { apodDao.insertApod(any()) }
    }

    @Test
    fun getApodByDate_fallsBackToDirectNasaApi_whenProxyThrows() = runTest {
        val directTargetApod = space.o4bit.projectasteria.data.model.AstronomyPicture(
            date = "2023-01-01",
            explanation = "New Year APOD",
            hdUrl = null,
            mediaType = "image",
            title = "New Year Cosmos",
            url = "https://example.com/ny.jpg"
        )
        coEvery { apodDao.getApodByDate("2023-01-01") } returns null
        coEvery { nasaApodService.getAstronomyPictureByDate("2023-01-01") } throws
            RuntimeException("Proxy 500 error")
        coEvery { nasaApodService.getDirectAstronomyPictureByDate("2023-01-01") } returns directTargetApod

        val result = spaceRepository.getApodByDate("2023-01-01")

        assertNotNull(result)
        assertEquals("New Year Cosmos", result?.astronomyPicture?.title)
        assertEquals("2023-01-01", result?.astronomyPicture?.date)
        coVerify(exactly = 1) { nasaApodService.getDirectAstronomyPictureByDate("2023-01-01") }
    }

    @Test
    fun getApodByDate_extractsVideoUrlFromBasicHtml_whenScienceNasaApodHasEmptyUrl() = runTest {
        val videoApod = space.o4bit.projectasteria.data.model.AstronomyPicture(
            date = "2026-10-16",
            explanation = "Superfluid Helium in Space",
            hdUrl = "",
            mediaType = "video",
            title = "Superfluid Helium",
            url = "",
            basicHtml = "<video controls><source src=\"https://apod.nasa.gov/apod/image/2604/SuperfluidHelium_1080p.mp4\" type=\"video/mp4\"></video>"
        )
        coEvery { apodDao.getApodByDate("2026-10-16") } returns null
        coEvery { nasaApodService.getAstronomyPictureByDate("2026-10-16") } returns videoApod

        val result = spaceRepository.getApodByDate("2026-10-16")

        assertNotNull(result)
        assertEquals("https://apod.nasa.gov/apod/image/2604/SuperfluidHelium_1080p.mp4", result?.astronomyPicture?.url)
        assertEquals(null, result?.astronomyPicture?.hdUrl)
    }

    @Test
    fun getApodByDate_ignoresPlaceholderNasaScienceApod_andFetchesByCode() = runTest {
        val placeholderApod = space.o4bit.projectasteria.data.model.AstronomyPicture(
            date = "2025-01-01",
            explanation = "NASA.gov brings you the latest news, images and videos",
            hdUrl = "https://www.nasa.gov/wp-content/plugins/nasa-hds-core-setup/assets/images/news-thumbnail.png",
            mediaType = "image",
            title = "NASA Science",
            url = "https://www.nasa.gov/wp-content/plugins/nasa-hds-core-setup/assets/images/news-thumbnail.png"
        )
        val realCodeApod = space.o4bit.projectasteria.data.model.AstronomyPicture(
            date = "2025-01-01",
            explanation = "Real historical APOD",
            hdUrl = "https://apod.nasa.gov/apod/image/2501/real_hd.jpg",
            mediaType = "image",
            title = "Real Nebula 2025",
            url = "https://apod.nasa.gov/apod/image/2501/real.jpg"
        )
        coEvery { apodDao.getApodByDate("2025-01-01") } returns null
        coEvery { nasaApodService.getAstronomyPictureByDate("2025-01-01") } returns placeholderApod
        coEvery { nasaApodService.getScienceNasaApodByCode("250101") } returns realCodeApod

        val result = spaceRepository.getApodByDate("2025-01-01")

        assertNotNull(result)
        assertEquals("Real Nebula 2025", result?.astronomyPicture?.title)
        assertEquals("https://apod.nasa.gov/apod/image/2501/real.jpg", result?.astronomyPicture?.url)
        coVerify(exactly = 1) { nasaApodService.getScienceNasaApodByCode("250101") }
    }
}
