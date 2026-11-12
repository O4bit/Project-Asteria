package space.o4bit.projectasteria.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.data.local.AsteroidEntity
import space.o4bit.projectasteria.data.model.AsteroidSortBy
import space.o4bit.projectasteria.data.model.SortDirection
import space.o4bit.projectasteria.data.repository.AsteroidRepository

sealed interface AsteroidUiState {
    data object Loading : AsteroidUiState
    data class Success(val asteroids: List<AsteroidEntity>) : AsteroidUiState
    data class Error(val message: String) : AsteroidUiState
}

private data class AsteroidFilterOptions(
    val sortBy: AsteroidSortBy,
    val sortDirection: SortDirection,
    val hazardousOnly: Boolean
)

class AsteroidViewModel(
    private val asteroidRepository: AsteroidRepository
) : ViewModel() {
    private val _sortBy = MutableStateFlow(AsteroidSortBy.DISTANCE)
    val sortBy: StateFlow<AsteroidSortBy> = _sortBy.asStateFlow()
    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()
    private val _hazardousOnly = MutableStateFlow(false)
    val hazardousOnly: StateFlow<Boolean> = _hazardousOnly.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()
    private val _initialSyncAttempted = MutableStateFlow(false)

    private val filterOptionsFlow: Flow<AsteroidFilterOptions> = combine(
        _sortBy,
        _sortDirection,
        _hazardousOnly
    ) { by, dir, hazardous ->
        AsteroidFilterOptions(by, dir, hazardous)
    }

    val uiState: StateFlow<AsteroidUiState> = combine(
        asteroidRepository.allAsteroids,
        filterOptionsFlow,
        _initialSyncAttempted,
        _refreshError
    ) { list, options, syncAttempted, error ->
        if (list.isEmpty() && syncAttempted && error != null) {
            AsteroidUiState.Error(error)
        } else {
            val filtered = if (options.hazardousOnly) list.filter { it.isPotentiallyHazardous } else list
            val sorted = when (options.sortBy) {
                AsteroidSortBy.DISTANCE -> {
                    val (valid, invalid) = filtered.partition { it.missDistanceKm >= 0.0 }
                    val sortedValid = if (options.sortDirection == SortDirection.ASCENDING) {
                        valid.sortedBy { it.missDistanceKm }
                    } else {
                        valid.sortedByDescending { it.missDistanceKm }
                    }
                    sortedValid + invalid
                }
                AsteroidSortBy.SPEED -> {
                    val sortedList = filtered.sortedBy { it.relativeVelocityKmh.toDoubleOrNull() ?: 0.0 }
                    if (options.sortDirection == SortDirection.DESCENDING) sortedList.reversed() else sortedList
                }
                AsteroidSortBy.SIZE -> {
                    val sortedList = filtered.sortedBy { it.estimatedDiameterMaxKm }
                    if (options.sortDirection == SortDirection.DESCENDING) sortedList.reversed() else sortedList
                }
                AsteroidSortBy.NAME -> {
                    val sortedList = filtered.sortedBy { it.name }
                    if (options.sortDirection == SortDirection.DESCENDING) sortedList.reversed() else sortedList
                }
            }
            AsteroidUiState.Success(sorted)
        }
    }.catch { e ->
        emit(AsteroidUiState.Error(e.message ?: "Failed to load asteroids"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AsteroidUiState.Loading
    )

    fun setSortBy(sortBy: AsteroidSortBy) {
        _sortBy.update { sortBy }
    }

    fun toggleDirection() {
        _sortDirection.update { it.toggle() }
    }

    fun toggleHazardousOnly() {
        _hazardousOnly.update { !it }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.update { true }
            _refreshError.update { null }
            try {
                asteroidRepository.refreshTodaysAsteroids()
            } catch (e: Exception) {
                _refreshError.update { e.message ?: "Failed to refresh asteroids" }
            } finally {
                _initialSyncAttempted.update { true }
                _isRefreshing.update { false }
            }
        }
    }

    class Factory(private val asteroidRepository: AsteroidRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AsteroidViewModel(asteroidRepository) as T
        }
    }
}