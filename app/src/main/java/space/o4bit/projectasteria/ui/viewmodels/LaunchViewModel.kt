package space.o4bit.projectasteria.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.model.LaunchSortBy
import space.o4bit.projectasteria.data.model.SortDirection
import space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository
import space.o4bit.projectasteria.data.repository.LaunchRepository

sealed interface LaunchUiState {
    data object Loading : LaunchUiState
    data class Success(
        val launches: List<LaunchEntity>,
        val launchSpeedMultiplier: Float = 1f
    ) : LaunchUiState
    data class Error(val message: String) : LaunchUiState
}

class LaunchViewModel(
    private val launchRepository: LaunchRepository,
    private val backgroundPrefs: BackgroundPreferencesRepository
) : ViewModel() {

    private val _sortBy = MutableStateFlow(LaunchSortBy.DATE)
    val sortBy: StateFlow<LaunchSortBy> = _sortBy.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val uiState: StateFlow<LaunchUiState> = combine(
        launchRepository.allLaunches,
        backgroundPrefs.hyperdriveThresholdMinutes,
        tickerFlow,
        _sortBy,
        _sortDirection
    ) { launches, thresholdMinutes, now, by, dir ->
        if (launches.isEmpty()) {
            LaunchUiState.Loading
        } else {
            val isLaunchActive = launches.any { launchEntity ->
                launchEntity.statusName.equals("In Flight", ignoreCase = true) ||
                        (now >= launchEntity.netMillis - thresholdMinutes * 60 * 1000L &&
                                now - launchEntity.netMillis < 15 * 60 * 1000L)
            }
            val speed = if (isLaunchActive) 15f else 1f

            val sorted = when (by) {
                LaunchSortBy.DATE -> launches.sortedBy { it.netMillis }
                LaunchSortBy.NAME -> launches.sortedBy { it.name }
            }
            val finalResult = if (dir == SortDirection.DESCENDING) sorted.reversed() else sorted

            LaunchUiState.Success(finalResult, speed)
        }
    }.catch { e ->
        emit(LaunchUiState.Error(e.message ?: "Failed to load launches"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LaunchUiState.Loading
    )

    fun setSortBy(sortBy: LaunchSortBy) {
        _sortBy.value = sortBy
    }

    fun toggleDirection() {
        _sortDirection.value = _sortDirection.value.toggle()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                launchRepository.refreshUpcomingLaunches()
            } catch (_: Exception) {
                // Room Flow will surface cached data
            }
        }
    }

    class Factory(
        private val launchRepository: LaunchRepository,
        private val backgroundPrefs: BackgroundPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LaunchViewModel(launchRepository, backgroundPrefs) as T
        }
    }
}
