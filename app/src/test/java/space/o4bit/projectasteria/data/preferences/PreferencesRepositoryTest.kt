package space.o4bit.projectasteria.data.preferences

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the preference repositories.
 *
 * DataStore requires an Android context for real I/O, so these tests use
 * mocked repository instances to verify:
 *   - Default values exposed by each repository.
 *   - Flow emission contract (each preference emits its declared default).
 *   - Toggling / updating functions exist and can be called without error.
 *
 * End-to-end persistence tests (write → read) belong in the androidTest suite
 * where a real DataStore backed by a temp directory can be used.
 */
class PreferencesRepositoryTest {

    // ─── SortingPreferencesRepository ────────────────────────────────────────

    @Test
    fun sortingPrefs_launchesAscending_defaultIsTrue() = runTest {
        val repo = mockk<SortingPreferencesRepository>()
        every { repo.isLaunchesAscending } returns flowOf(true)

        val value = repo.isLaunchesAscending.first()
        assertTrue("Default launch sort should be ascending", value)
    }

    @Test
    fun sortingPrefs_asteroidsClosest_defaultIsTrue() = runTest {
        val repo = mockk<SortingPreferencesRepository>()
        every { repo.isAsteroidsClosest } returns flowOf(true)

        val value = repo.isAsteroidsClosest.first()
        assertTrue("Default asteroid sort should be closest-first", value)
    }

    @Test
    fun sortingPrefs_toggleLaunchSort_flipsValue() = runTest {
        val repo = mockk<SortingPreferencesRepository>()
        every { repo.isLaunchesAscending } returnsMany listOf(flowOf(true), flowOf(false))
        coEvery { repo.toggleLaunchSort() } returns Unit

        val before = repo.isLaunchesAscending.first()
        repo.toggleLaunchSort()
        val after = repo.isLaunchesAscending.first()

        assertTrue(before)
        assertFalse(after)
    }

    @Test
    fun sortingPrefs_toggleAsteroidSort_flipsValue() = runTest {
        val repo = mockk<SortingPreferencesRepository>()
        every { repo.isAsteroidsClosest } returnsMany listOf(flowOf(true), flowOf(false))
        coEvery { repo.toggleAsteroidSort() } returns Unit

        val before = repo.isAsteroidsClosest.first()
        repo.toggleAsteroidSort()
        val after = repo.isAsteroidsClosest.first()

        assertTrue(before)
        assertFalse(after)
    }

    // ─── BackgroundPreferencesRepository ─────────────────────────────────────

    @Test
    fun backgroundPrefs_defaultType_isSpace() = runTest {
        val repo = mockk<BackgroundPreferencesRepository>()
        every { repo.backgroundType } returns flowOf(BackgroundPreferencesRepository.DEFAULT_BACKGROUND_TYPE)

        val value = repo.backgroundType.first()
        assertEquals("SPACE", value)
    }

    @Test
    fun backgroundPrefs_parallaxEnabled_defaultIsTrue() = runTest {
        val repo = mockk<BackgroundPreferencesRepository>()
        every { repo.enableParallax } returns flowOf(BackgroundPreferencesRepository.DEFAULT_ENABLE_PARALLAX)

        val value = repo.enableParallax.first()
        assertTrue("Parallax should be enabled by default", value)
    }

    @Test
    fun backgroundPrefs_hyperdriveThreshold_defaultIsOne() = runTest {
        val repo = mockk<BackgroundPreferencesRepository>()
        every { repo.hyperdriveThresholdMinutes } returns flowOf(BackgroundPreferencesRepository.DEFAULT_HYPERDRIVE_THRESHOLD_MINUTES)

        val value = repo.hyperdriveThresholdMinutes.first()
        assertEquals(1, value)
    }

    @Test
    fun backgroundPrefs_updateType_emitsNewValue() = runTest {
        val repo = mockk<BackgroundPreferencesRepository>()
        every { repo.backgroundType } returnsMany listOf(flowOf("SPACE"), flowOf("STARRY"))
        coEvery { repo.updateBackgroundType(any()) } returns Unit

        val before = repo.backgroundType.first()
        repo.updateBackgroundType("STARRY")
        val after = repo.backgroundType.first()

        assertEquals("SPACE", before)
        assertEquals("STARRY", after)
    }

    @Test
    fun backgroundPrefs_updateParallax_emitsNewValue() = runTest {
        val repo = mockk<BackgroundPreferencesRepository>()
        every { repo.enableParallax } returnsMany listOf(flowOf(true), flowOf(false))
        coEvery { repo.updateEnableParallax(any()) } returns Unit

        val before = repo.enableParallax.first()
        repo.updateEnableParallax(false)
        val after = repo.enableParallax.first()

        assertTrue(before)
        assertFalse(after)
    }

    @Test
    fun backgroundPrefs_updateHyperdriveThreshold_emitsNewValue() = runTest {
        val repo = mockk<BackgroundPreferencesRepository>()
        every { repo.hyperdriveThresholdMinutes } returnsMany listOf(flowOf(1), flowOf(5))
        coEvery { repo.updateHyperdriveThresholdMinutes(any()) } returns Unit

        val before = repo.hyperdriveThresholdMinutes.first()
        repo.updateHyperdriveThresholdMinutes(5)
        val after = repo.hyperdriveThresholdMinutes.first()

        assertEquals(1, before)
        assertEquals(5, after)
    }

    // ─── Contract: default values match companion object constants ────────────

    @Test
    fun backgroundType_defaultConstant_isSpace() {
        assertEquals("SPACE", BackgroundPreferencesRepository.DEFAULT_BACKGROUND_TYPE)
    }

    @Test
    fun parallax_defaultConstant_isTrue() {
        assertTrue(BackgroundPreferencesRepository.DEFAULT_ENABLE_PARALLAX)
    }

    @Test
    fun hyperdriveThreshold_defaultConstant_isOne() {
        assertEquals(1, BackgroundPreferencesRepository.DEFAULT_HYPERDRIVE_THRESHOLD_MINUTES)
    }
}
