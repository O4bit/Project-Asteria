package space.o4bit.projectasteria.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.data.api.NasaNeoWsService
import space.o4bit.projectasteria.data.local.AsteroidDao
import space.o4bit.projectasteria.data.local.AsteroidEntity
import space.o4bit.projectasteria.data.preferences.SortingPreferencesRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class AsteroidRepository(
    private val asteroidDao: AsteroidDao,
    private val sortingPreferences: SortingPreferencesRepository,
    private val neoWsService: NasaNeoWsService = NasaNeoWsService.create()
) {
    val allAsteroids: Flow<List<AsteroidEntity>> = asteroidDao.getAllAsteroids()

    val asteroids: Flow<List<AsteroidEntity>> = sortingPreferences.isAsteroidsClosest.flatMapLatest { isClosest ->
        if (isClosest) {
            asteroidDao.getClosestAsteroids()
        } else {
            asteroidDao.getFarthestAsteroids()
        }
    }

    suspend fun refreshTodaysAsteroids() = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val todayStr = dateFormat.format(Date())

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
                    nasaJplUrl = asteroid.nasaJplUrl,
                    absoluteMagnitudeH = asteroid.absoluteMagnitudeH,
                    estimatedDiameterMinKm = asteroid.estimatedDiameter.kilometers.min,
                    estimatedDiameterMaxKm = asteroid.estimatedDiameter.kilometers.max,
                    isPotentiallyHazardous = asteroid.isPotentiallyHazardous,
                    isSentryObject = asteroid.isSentryObject,
                    closeApproachDate = approachData?.closeApproachDate ?: "",
                    relativeVelocityKms = approachData?.relativeVelocity?.kilometersPerSecond ?: "",
                    relativeVelocityKmh = approachData?.relativeVelocity?.kilometersPerHour ?: "",
                    missDistanceKm = approachData?.missDistance?.kilometers?.toDoubleOrNull() ?: Double.MAX_VALUE,
                    missDistanceLunar = approachData?.missDistance?.lunar ?: ""
                )
            }

            asteroidDao.clearAsteroids()
            asteroidDao.insertAsteroids(entities)
        } catch (e: Exception) {
            throw e
        }
    }
}
