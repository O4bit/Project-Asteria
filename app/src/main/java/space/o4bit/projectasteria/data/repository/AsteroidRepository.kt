package space.o4bit.projectasteria.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.data.api.NasaNeoWsService
import space.o4bit.projectasteria.data.local.AsteroidDao
import space.o4bit.projectasteria.data.local.AsteroidEntity
import space.o4bit.projectasteria.data.preferences.SortingPreferencesRepository
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class AsteroidRepository(
    private val asteroidDao: AsteroidDao,
    private val sortingPreferences: SortingPreferencesRepository,
    private val neoWsService: NasaNeoWsService = NasaNeoWsService.create()
) {
    private val refreshMutex = Mutex()

    val allAsteroids: Flow<List<AsteroidEntity>> = asteroidDao.getAllAsteroids()
    val asteroids: Flow<List<AsteroidEntity>> = sortingPreferences.isAsteroidsClosest.flatMapLatest { isClosest ->
        if (isClosest) {
            asteroidDao.getClosestAsteroids()
        } else {
            asteroidDao.getFarthestAsteroids()
        }
    }

    suspend fun refreshTodaysAsteroids() = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val todayStr = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val response = neoWsService.getNearEarthObjects(
                startDate = todayStr,
                endDate = todayStr
            )
            val networkAsteroids = response.nearEarthObjects.values.flatten()
            val entities = networkAsteroids.map { asteroid ->
                val approachData = asteroid.closeApproachData.firstOrNull()
                AsteroidEntity(
                    id = asteroid.id,
                    name = asteroid.name,
                    nasaJplUrl = if (asteroid.nasaJplUrl.startsWith("http://", ignoreCase = true)) {
                        asteroid.nasaJplUrl.replaceFirst("http://", "https://", ignoreCase = true)
                    } else {
                        asteroid.nasaJplUrl
                    },
                    absoluteMagnitudeH = asteroid.absoluteMagnitudeH,
                    estimatedDiameterMinKm = asteroid.estimatedDiameter.kilometers.min,
                    estimatedDiameterMaxKm = asteroid.estimatedDiameter.kilometers.max,
                    isPotentiallyHazardous = asteroid.isPotentiallyHazardous,
                    isSentryObject = asteroid.isSentryObject,
                    closeApproachDate = approachData?.closeApproachDate ?: "",
                    relativeVelocityKms = approachData?.relativeVelocity?.kilometersPerSecond ?: "",
                    relativeVelocityKmh = approachData?.relativeVelocity?.kilometersPerHour ?: "",
                    missDistanceKm = approachData?.missDistance?.kilometers?.toDoubleOrNull() ?: -1.0,
                    missDistanceLunar = approachData?.missDistance?.lunar ?: ""
                )
            }
            require(entities.all { it.id.isNotBlank() }) {
                "Asteroid response contains an invalid identifier"
            }
            asteroidDao.replaceAll(entities)
        }
    }
}