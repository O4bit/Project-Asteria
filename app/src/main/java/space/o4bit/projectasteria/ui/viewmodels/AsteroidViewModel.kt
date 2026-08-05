package space.o4bit.projectasteria.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

class AsteroidViewModel(
    private val asteroidRepository: AsteroidRepository
) : ViewModel() {

    private val _sortBy = MutableStateFlow(AsteroidSortBy.DISTANCE)
    val sortBy: StateFlow<AsteroidSortBy> = _sortBy.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _hazardousOnly = MutableStateFlow(false)
    val hazardousOnly: StateFlow<Boolean> = _hazardousOnly.asStateFlow()

    val uiState: StateFlow<AsteroidUiState> = combine(
        asteroidRepository.allAsteroids,
        _sortBy,
        _sortDirection,
        _hazardousOnly
    ) { list, by, dir, hazardous ->
        val filtered = if (hazardous) list.filter { it.isPotentiallyHazardous } else list
        val sorted = when (by) {
            AsteroidSortBy.DISTANCE -> filtered.sortedBy { it.missDistanceKm }
            AsteroidSortBy.SPEED -> filtered.sortedBy { it.relativeVelocityKmh.toDoubleOrNull() ?: 0.0 }
            AsteroidSortBy.SIZE -> filtered.sortedBy { it.estimatedDiameterMaxKm }
            AsteroidSortBy.NAME -> filtered.sortedBy { it.name }
        }
        val finalResult = if (dir == SortDirection.DESCENDING) sorted.reversed() else sorted
        AsteroidUiState.Success(finalResult) as AsteroidUiState
    }
    .catch { e ->
        emit(AsteroidUiState.Error(e.message ?: "Failed to load asteroids"))
    }
    .stateIn(
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
        viewModelScope.launch {
            try {
                asteroidRepository.refreshTodaysAsteroids()
            } catch (_: Exception) {
                // Flow from Room will reflect any cached results
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
