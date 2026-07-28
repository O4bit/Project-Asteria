package space.o4bit.projectasteria.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.repository.SpaceRepository
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ApodUiState {
    data object Loading : ApodUiState
    data class Success(val picture: EnhancedAstronomyPicture) : ApodUiState
    /**
     * @param message A user-facing message (never a raw exception string).
     * @param cachedPicture If non-null, the UI should show the cache underneath the error banner.
     */
    data class Error(
        val message: String,
        val cachedPicture: EnhancedAstronomyPicture? = null
    ) : ApodUiState
}

class ApodViewModel(
    private val spaceRepository: SpaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ApodUiState>(ApodUiState.Loading)
    val uiState: StateFlow<ApodUiState> = _uiState.asStateFlow()

    init {
        loadTodayApod()
    }

    fun loadTodayApod() {
        viewModelScope.launch {
            _uiState.value = ApodUiState.Loading
            // Try to load fresh data; fall back to the cache on failure.
            val cached = safeGetCached()
            try {
                val picture = spaceRepository.getTodaysAstronomyPicture()
                _uiState.value = ApodUiState.Success(picture)
            } catch (e: Exception) {
                _uiState.value = ApodUiState.Error(
                    message = toUserMessage(e),
                    cachedPicture = cached
                )
            }
        }
    }

    fun loadApodForDate(dateString: String) {
        viewModelScope.launch {
            _uiState.value = ApodUiState.Loading
            try {
                val picture = spaceRepository.getApodByDate(dateString)
                if (picture != null) {
                    _uiState.value = ApodUiState.Success(picture)
                } else {
                    _uiState.value = ApodUiState.Error(
                        message = "No picture found for $dateString"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ApodUiState.Error(message = toUserMessage(e))
            }
        }
    }

    fun toggleFavorite(date: String, isFavorite: Boolean) {
        viewModelScope.launch {
            spaceRepository.setFavorite(date, isFavorite)
            val currentState = _uiState.value
            if (currentState is ApodUiState.Success && currentState.picture.astronomyPicture.date == date) {
                _uiState.value = ApodUiState.Success(
                    currentState.picture.copy(isFavorite = isFavorite)
                )
            } else if (currentState is ApodUiState.Error && currentState.cachedPicture?.astronomyPicture?.date == date) {
                _uiState.value = currentState.copy(
                    cachedPicture = currentState.cachedPicture.copy(isFavorite = isFavorite)
                )
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Return whatever is in the Room cache for today without hitting the network. */
    private suspend fun safeGetCached(): EnhancedAstronomyPicture? = try {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        spaceRepository.getApodByDate(today)
    } catch (_: Exception) {
        null
    }

    /**
     * Map exceptions to three friendly categories so raw HTTP messages, stack
     * traces, and network timeouts never reach the user.
     */
    private fun toUserMessage(e: Exception): String = when {
        e is IOException -> "You appear to be offline. Showing cached content."
        e is HttpException && e.code() in 500..599 ->
            "The NASA servers are temporarily unavailable. Try again in a moment."
        else -> "Couldn't load today's space picture. Tap Retry to try again."
    }

    class Factory(private val spaceRepository: SpaceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApodViewModel(spaceRepository) as T
        }
    }
}
