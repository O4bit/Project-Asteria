package space.o4bit.projectasteria.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
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

        // When DAO returns null and the network call throws, getApodByDate returns null gracefully.
        assertEquals(null, result)
    }

    @Test
    fun getPagedHistory_returnsEmptyList_whenNoCachedData() = runTest {
        coEvery { apodDao.getPagedApods(any(), any()) } returns emptyList()

        val result = spaceRepository.getPagedHistory(1, 20)

        assertEquals(emptyList<Any>(), result)
    }
}
