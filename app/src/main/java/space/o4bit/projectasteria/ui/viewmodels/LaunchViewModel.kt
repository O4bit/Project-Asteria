package space.o4bit.projectasteria.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()
    private val _initialSyncAttempted = MutableStateFlow(false)

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private val speedMultiplierFlow: Flow<Float> = combine(
        launchRepository.allLaunches,
        backgroundPrefs.hyperdriveThresholdMinutes,
        tickerFlow
    ) { launches, thresholdMinutes, now ->
        val isLaunchActive = launches.any { launchEntity ->
            launchEntity.statusName.equals("In Flight", ignoreCase = true) ||
                    (now >= launchEntity.netMillis - thresholdMinutes * 60 * 1000L &&
                            now - launchEntity.netMillis < 15 * 60 * 1000L)
        }
        if (isLaunchActive) 15f else 1f
    }.distinctUntilChanged()

    private val sortedLaunchesFlow: Flow<List<LaunchEntity>> = combine(
        launchRepository.allLaunches,
        _sortBy,
        _sortDirection
    ) { launches, by, dir ->
        when (by) {
            LaunchSortBy.DATE -> {
                val (valid, invalid) = launches.partition { it.netMillis > 0L }
                val sortedValid = if (dir == SortDirection.ASCENDING) {
                    valid.sortedBy { it.netMillis }
                } else {
                    valid.sortedByDescending { it.netMillis }
                }
                sortedValid + invalid
            }
            LaunchSortBy.NAME -> {
                if (dir == SortDirection.ASCENDING) {
                    launches.sortedBy { it.name }
                } else {
                    launches.sortedByDescending { it.name }
                }
            }
        }
    }

    val uiState: StateFlow<LaunchUiState> = combine(
        sortedLaunchesFlow,
        speedMultiplierFlow,
        _initialSyncAttempted,
        _refreshError
    ) { launches, speed, syncAttempted, refreshError ->
        if (launches.isEmpty()) {
            if (!syncAttempted) {
                LaunchUiState.Loading
            } else if (refreshError != null) {
                LaunchUiState.Error(refreshError)
            } else {
                LaunchUiState.Success(emptyList(), speed)
            }
        } else {
            LaunchUiState.Success(launches, speed)
        }
    }.catch { e ->
        emit(LaunchUiState.Error(e.message ?: "Failed to load launches"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LaunchUiState.Loading
    )

    fun setSortBy(sortBy: LaunchSortBy) {
        _sortBy.update { sortBy }
    }

    fun toggleDirection() {
        _sortDirection.update { it.toggle() }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.update { true }
            _refreshError.update { null }
            try {
                launchRepository.refreshUpcomingLaunches()
            } catch (e: Exception) {
                _refreshError.update { e.message ?: "Failed to refresh launches" }
            } finally {
                _initialSyncAttempted.update { true }
                _isRefreshing.update { false }
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