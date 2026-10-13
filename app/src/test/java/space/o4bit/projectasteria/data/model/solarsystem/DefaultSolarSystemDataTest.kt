package space.o4bit.projectasteria.data.model.solarsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSolarSystemDataTest {

    @Test
    fun initialBodies_containsRequiredNotableStars() {
        val bodies = DefaultSolarSystemData.initialBodies
        val starIds = listOf("sirius", "betelgeuse", "vega", "proximacentauri", "polaris")

        starIds.forEach { id ->
            val star = bodies.find { it.id == id }
            assertNotNull("Missing star with id $id", star)
            assertEquals("Star", star?.bodyType)
            assertFalse(star?.constellation.isNullOrBlank())
            assertFalse(star?.description.isNullOrBlank())
            assertTrue(star?.avgTemp ?: 0.0 > 0.0)
        }
    }

    @Test
    fun initialBodies_containsRequiredExoplanets() {
        val bodies = DefaultSolarSystemData.initialBodies
        val exoplanetIds = listOf("proximab", "trappist1e", "kepler452b")

        exoplanetIds.forEach { id ->
            val planet = bodies.find { it.id == id }
            assertNotNull("Missing exoplanet with id $id", planet)
            assertEquals("Exoplanet", planet?.bodyType)
            assertFalse(planet?.constellation.isNullOrBlank())
            assertFalse(planet?.description.isNullOrBlank())
            assertFalse(planet?.aroundPlanetName.isNullOrBlank())
            assertTrue(planet?.sideralOrbit ?: 0.0 > 0.0)
            assertTrue(planet?.distanceLightYears ?: 0.0 > 0.0)
        }
    }

    @Test
    fun initialBodies_containsRequiredDeepSkyObjects() {
        val bodies = DefaultSolarSystemData.initialBodies
        val deepSkyIds = listOf("andromeda", "orionnebula", "pleiades")

        deepSkyIds.forEach { id ->
            val dso = bodies.find { it.id == id }
            assertNotNull("Missing deep sky object with id $id", dso)
            assertEquals("Deep Sky", dso?.bodyType)
            assertFalse(dso?.constellation.isNullOrBlank())
            assertFalse(dso?.description.isNullOrBlank())
            assertFalse(dso?.dimension.isNullOrBlank())
            assertTrue(dso?.distanceLightYears ?: 0.0 > 0.0)
        }
    }

    @Test
    fun initialBodies_allIdsAreUnique() {
        val bodies = DefaultSolarSystemData.initialBodies
        val ids = bodies.map { it.id }
        val uniqueIds = ids.toSet()

        assertEquals(ids.size, uniqueIds.size)
    }

    @Test
    fun initialBodies_allEntriesHaveValidNamesAndTypes() {
        val bodies = DefaultSolarSystemData.initialBodies

        bodies.forEach { body ->
            assertTrue(body.id.isNotBlank())
            assertTrue(body.englishName.isNotBlank())
            assertTrue(body.bodyType.isNotBlank())
        }
    }

    @Test
    fun initialBodies_containsEnrichedSun() {
        val sun = DefaultSolarSystemData.initialBodies.find { it.id == "soleil" }
        assertNotNull(sun)
        assertEquals("Sun", sun?.englishName)
        assertEquals("Star", sun?.bodyType)
        assertEquals("G2V", sun?.spectralType)
        assertFalse(sun?.description.isNullOrBlank())
    }

    @Test
    fun fromDto_preservesFallbackTelemetry_whenDtoFieldsAreNull() {
        val fallback = DefaultSolarSystemData.initialBodies.first { it.id == "soleil" }
        val dto = CelestialBodyDto(
            id = "soleil",
            name = "Soleil",
            englishName = "Sun",
            isPlanet = false,
            bodyType = "Star"
        )
        val entity = space.o4bit.projectasteria.data.local.CelestialBodyEntity.fromDto(dto, fallback)
        assertEquals("G2V", entity.spectralType)
        assertEquals(fallback.description, entity.description)
        assertEquals(fallback.distanceLightYears, entity.distanceLightYears, 0.0001)
    }
}
