package space.o4bit.projectasteria.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.data.api.SpaceLaunchService
import space.o4bit.projectasteria.data.local.LaunchDao
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.preferences.SortingPreferencesRepository
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchRepository(
    private val launchDao: LaunchDao,
    private val sortingPreferences: SortingPreferencesRepository,
    private val launchService: SpaceLaunchService = SpaceLaunchService.create()
) {
    // Single Source of Truth: Observe the database sorted by the user's preference
    val launches: Flow<List<LaunchEntity>> = sortingPreferences.isLaunchesAscending.flatMapLatest { isAscending ->
        if (isAscending) {
            launchDao.getOldestLaunches()
        } else {
            launchDao.getNewestLaunches()
        }
    }

    suspend fun refreshUpcomingLaunches(limit: Int = 15) = withContext(Dispatchers.IO) {
        try {
            val response = launchService.getUpcomingLaunches(limit = limit)
            val entities = response.results.map { launch ->
                // Parse the ISO date to timestamp for the Room query
                val netMillis = try {
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    format.parse(launch.net)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }

                LaunchEntity(
                    id = launch.id,
                    name = launch.name,
                    statusName = launch.status.name,
                    statusDescription = launch.status.description,
                    net = launch.net,
                    providerName = launch.launchServiceProvider?.name,
                    padName = launch.pad?.name,
                    locationName = launch.pad?.location?.name,
                    missionName = launch.mission?.name,
                    missionDescription = launch.mission?.description,
                    image = launch.image,
                    netMillis = netMillis
                )
            }
            
            // Replaces the old cache with the newest data
            launchDao.clearLaunches()
            launchDao.insertLaunches(entities)
        } catch (e: Exception) {
            throw e // the UI wrapper will catch and display error state or read from cache
        }
    }
}