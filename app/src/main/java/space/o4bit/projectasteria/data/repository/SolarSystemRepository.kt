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
        if (type.equals("All", ignoreCase = true)) {
            celestialBodyDao.getAllBodies()
        } else if (type.equals("Planets", ignoreCase = true)) {
            celestialBodyDao.getPlanets()
        } else {
            celestialBodyDao.getBodiesByType(type)
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
                val entities = response.bodies.map { CelestialBodyEntity.fromDto(it) }
                celestialBodyDao.insertBodies(entities)
            }
        } catch (e: Exception) {
            if (celestialBodyDao.countBodies() == 0) {
                celestialBodyDao.insertBodies(DefaultSolarSystemData.initialBodies)
            }
        }
    }
}
