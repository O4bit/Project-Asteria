package space.o4bit.projectasteria.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.data.api.SpaceLaunchService
import space.o4bit.projectasteria.data.local.LaunchDao
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.preferences.ReminderPreferencesRepository
import space.o4bit.projectasteria.data.preferences.SortingPreferencesRepository
import space.o4bit.projectasteria.data.worker.LaunchReminderScheduler
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchRepository(
    private val launchDao: LaunchDao,
    private val sortingPreferences: SortingPreferencesRepository,
    private val launchService: SpaceLaunchService = SpaceLaunchService.create(),
    private val reminderScheduler: LaunchReminderScheduler? = null,
    private val reminderPreferences: ReminderPreferencesRepository? = null,
    private val backgroundPreferences: space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository? = null
) {
    private val refreshMutex = Mutex()

    val allLaunches: Flow<List<LaunchEntity>> = launchDao.getAllLaunches()
    val launches: Flow<List<LaunchEntity>> = sortingPreferences.isLaunchesAscending.flatMapLatest { isAscending ->
        if (isAscending) {
            launchDao.getOldestLaunches()
        } else {
            launchDao.getNewestLaunches()
        }
    }

    suspend fun refreshUpcomingLaunches(limit: Int = 15) = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val response = launchService.getUpcomingLaunches(limit = limit)
            val entities = response.results.map { launch ->
                val netInstant = parseLaunchInstant(launch.net)
                val netMillis = netInstant?.toEpochMilli() ?: -1L
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
            require(entities.all { it.id.isNotBlank() }) {
                "Launch response contains an invalid identifier"
            }
            val pinnedIds = backgroundPreferences?.pinnedLaunchIds?.first() ?: emptySet()
            val remindedIds = reminderPreferences?.remindedLaunchIds?.first() ?: emptySet()
            val preservedIds = (pinnedIds + remindedIds).toList()
            launchDao.replaceAll(entities, preservedIds)
            if (reminderScheduler != null && reminderPreferences != null) {
                reminderScheduler.reconcile(entities, remindedIds)
            }
        }
    }

    companion object {
        fun parseLaunchInstant(raw: String?): Instant? {
            if (raw.isNullOrBlank()) return null
            return runCatching { Instant.parse(raw) }.getOrNull()
                ?: runCatching { java.time.OffsetDateTime.parse(raw).toInstant() }.getOrNull()
                ?: runCatching { java.time.ZonedDateTime.parse(raw).toInstant() }.getOrNull()
        }
    }
}