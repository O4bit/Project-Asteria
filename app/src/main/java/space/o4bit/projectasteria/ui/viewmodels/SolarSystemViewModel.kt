package space.o4bit.projectasteria.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.data.local.CelestialBodyEntity
import space.o4bit.projectasteria.data.repository.SolarSystemRepository

sealed interface SolarSystemUiState {
    data object Loading : SolarSystemUiState
    data class Success(
        val bodies: List<CelestialBodyEntity>,
        val selectedFilter: String,
        val searchQuery: String,
        val isRefreshing: Boolean = false
    ) : SolarSystemUiState
    data class Error(val message: String) : SolarSystemUiState
}

class SolarSystemViewModel(
    private val repository: SolarSystemRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SolarSystemUiState>(SolarSystemUiState.Loading)
    val uiState: StateFlow<SolarSystemUiState> = _uiState.asStateFlow()

    private var currentFilter = "All"
    private var currentSearch = ""
    private var observationJob: Job? = null

    init {
        loadBodies()
    }

    fun loadBodies() {
        viewModelScope.launch {
            _uiState.update { SolarSystemUiState.Loading }
            repository.initializeDatabase()
            startObserving()
            try {
                repository.refreshBodies()
            } catch (_: Exception) {
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { current ->
                if (current is SolarSystemUiState.Success) {
                    current.copy(isRefreshing = true)
                } else {
                    current
                }
            }
            try {
                repository.refreshBodies()
            } catch (_: Exception) {
            } finally {
                _uiState.update { current ->
                    if (current is SolarSystemUiState.Success) {
                        current.copy(isRefreshing = false)
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentSearch = query
        startObserving()
    }

    fun onFilterSelected(filter: String) {
        currentFilter = filter
        startObserving()
    }

    private fun startObserving() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            val flow = if (currentSearch.isNotBlank()) {
                repository.searchBodies(currentSearch)
            } else {
                repository.getBodiesByType(currentFilter)
            }
            flow.catch { e ->
                _uiState.update { SolarSystemUiState.Error(e.message ?: "Failed to load solar system bodies") }
            }.collectLatest { list ->
                val filtered = if (currentSearch.isNotBlank() && !currentFilter.equals("All", ignoreCase = true)) {
                    when {
                        currentFilter.equals("Planets", ignoreCase = true) || currentFilter.equals("Planet", ignoreCase = true) ->
                            list.filter { it.isPlanet || it.bodyType.equals("Planet", ignoreCase = true) }
                        currentFilter.equals("Moons", ignoreCase = true) || currentFilter.equals("Moon", ignoreCase = true) ->
                            list.filter { it.bodyType.equals("Moon", ignoreCase = true) || it.bodyType.equals("Moons", ignoreCase = true) }
                        currentFilter.equals("Stars", ignoreCase = true) || currentFilter.equals("Star", ignoreCase = true) ->
                            list.filter { it.bodyType.equals("Star", ignoreCase = true) || it.bodyType.equals("Stars", ignoreCase = true) }
                        currentFilter.equals("Exoplanets", ignoreCase = true) || currentFilter.equals("Exoplanet", ignoreCase = true) ->
                            list.filter { it.bodyType.equals("Exoplanet", ignoreCase = true) || it.bodyType.equals("Exoplanets", ignoreCase = true) }
                        currentFilter.equals("Deep Sky", ignoreCase = true) || currentFilter.equals("Deep Sky Object", ignoreCase = true) || currentFilter.equals("Deep Sky Objects", ignoreCase = true) ->
                            list.filter { it.bodyType.equals("Deep Sky", ignoreCase = true) || it.bodyType.equals("Deep Sky Object", ignoreCase = true) || it.bodyType.equals("Deep Sky Objects", ignoreCase = true) }
                        currentFilter.equals("Asteroids", ignoreCase = true) || currentFilter.equals("Asteroid", ignoreCase = true) ->
                            list.filter { it.bodyType.equals("Asteroid", ignoreCase = true) || it.bodyType.equals("Asteroids", ignoreCase = true) || it.bodyType.equals("Dwarf Planet", ignoreCase = true) || it.bodyType.equals("Comet", ignoreCase = true) }
                        else ->
                            list.filter { it.bodyType.equals(currentFilter, ignoreCase = true) }
                    }
                } else {
                    list
                }
                val isRefreshing = (_uiState.value as? SolarSystemUiState.Success)?.isRefreshing ?: false
                _uiState.update {
                    SolarSystemUiState.Success(
                        bodies = filtered,
                        selectedFilter = currentFilter,
                        searchQuery = currentSearch,
                        isRefreshing = isRefreshing
                    )
                }
            }
        }
    }

    class Factory(private val repository: SolarSystemRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SolarSystemViewModel(repository) as T
        }
    }
}
