package space.o4bit.projectasteria.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.data.api.SolarSystemService
import space.o4bit.projectasteria.data.local.CelestialBodyDao
import space.o4bit.projectasteria.data.local.CelestialBodyEntity
import space.o4bit.projectasteria.data.model.solarsystem.DefaultSolarSystemData

class SolarSystemRepository(
    private val celestialBodyDao: CelestialBodyDao,
    private val solarSystemService: SolarSystemService = SolarSystemService.create()
) {
    val allBodies: Flow<List<CelestialBodyEntity>> = celestialBodyDao.getAllBodies()

    fun searchBodies(query: String): Flow<List<CelestialBodyEntity>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return celestialBodyDao.getAllBodies()
        return celestialBodyDao.searchBodies(trimmed)
    }

    fun getBodiesByType(type: String): Flow<List<CelestialBodyEntity>> =
        when {
            type.equals("All", ignoreCase = true) -> celestialBodyDao.getAllBodies()
            type.equals("Planets", ignoreCase = true) || type.equals("Planet", ignoreCase = true) -> celestialBodyDao.getPlanets()
            type.equals("Moons", ignoreCase = true) || type.equals("Moon", ignoreCase = true) -> celestialBodyDao.getBodiesByTypes(listOf("Moon", "Moons"))
            type.equals("Stars", ignoreCase = true) || type.equals("Star", ignoreCase = true) -> celestialBodyDao.getBodiesByTypes(listOf("Star", "Stars"))
            type.equals("Exoplanets", ignoreCase = true) || type.equals("Exoplanet", ignoreCase = true) -> celestialBodyDao.getBodiesByTypes(listOf("Exoplanet", "Exoplanets"))
            type.equals("Deep Sky", ignoreCase = true) || type.equals("Deep Sky Object", ignoreCase = true) -> celestialBodyDao.getBodiesByTypes(listOf("Deep Sky", "Deep Sky Object"))
            type.equals("Asteroids", ignoreCase = true) || type.equals("Asteroid", ignoreCase = true) -> celestialBodyDao.getBodiesByTypes(listOf("Asteroid", "Asteroids", "Dwarf Planet", "Comet"))
            else -> celestialBodyDao.getBodiesByType(type)
        }

    suspend fun getBodyById(id: String): CelestialBodyEntity? = withContext(Dispatchers.IO) {
        celestialBodyDao.getBodyById(id)
    }

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        if (celestialBodyDao.countBodies() == 0) {
            celestialBodyDao.insertBodies(DefaultSolarSystemData.initialBodies)
        }
    }

    suspend fun refreshBodies(apiKey: String? = null) = withContext(Dispatchers.IO) {
        initializeDatabase()
        try {
            val authHeader = apiKey?.let { if (it.isNotBlank()) "Bearer $it" else null }
            val response = solarSystemService.getBodies(authHeader = authHeader)
            if (response.bodies.isNotEmpty()) {
                val defaultById = DefaultSolarSystemData.initialBodies.associateBy { it.id }
                val entities = response.bodies.map { dto ->
                    val resolvedId = dto.id ?: dto.name ?: dto.englishName ?: ""
                    CelestialBodyEntity.fromDto(dto, defaultById[resolvedId])
                }
                celestialBodyDao.insertBodies(entities)
            }
        } catch (e: Exception) {
            if (celestialBodyDao.countBodies() == 0) {
                celestialBodyDao.insertBodies(DefaultSolarSystemData.initialBodies)
            }
        }
    }
}
