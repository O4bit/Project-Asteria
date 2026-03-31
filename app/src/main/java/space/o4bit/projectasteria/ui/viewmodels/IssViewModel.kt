package space.o4bit.projectasteria.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import space.o4bit.projectasteria.data.model.iss.IssPosition
import space.o4bit.projectasteria.data.repository.IssRepository

data class IssUiState(
    val location: IssPosition? = null,
    val isLive: Boolean = false,
    val lastUpdateTime: Long = 0L,
    val errorMessage: String? = null
)

class IssViewModel(
    private val repository: IssRepository = IssRepository()
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    
    init {
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<IssUiState> = refreshTrigger.flatMapLatest {
        flow {
            var currentDelay = 3000L
            val maxDelay = 60000L
            var lastLocation: IssPosition? = null

            // First emit a loading state if we have no prior data
            if (lastLocation == null) {
                emit(IssUiState(location = null, isLive = false, errorMessage = null))
            }

            while (currentCoroutineContext().isActive) {
                try {
                    val location = repository.getIssPosition()
                    
                    if (hasMeaningfulChange(lastLocation, location)) {
                        emit(IssUiState(
                            location = location,
                            isLive = true,
                            lastUpdateTime = System.currentTimeMillis(),
                            errorMessage = null
                        ))
                        lastLocation = location
                    } else {
                        // Even if it didn't change meaningfully, we might want to update the 'lastUpdateTime' optionally,
                        // but sticking to the last meaningful change makes it jitter less.
                    }
                    currentDelay = 3000L // Reset backoff on success
                } catch (e: Exception) {
                    emit(IssUiState(
                        location = lastLocation, // Keep last known point
                        isLive = false,
                        lastUpdateTime = System.currentTimeMillis(),
                        errorMessage = "Signal lost. Retrying in ${currentDelay/1000}s..."
                    ))
                    currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) // Exponential backoff
                }
                delay(currentDelay)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        // Stop the loop completely when UI is hidden longer than 5 seconds
        started = SharingStarted.WhileSubscribed(5000), 
        initialValue = IssUiState()
    )

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    private fun hasMeaningfulChange(old: IssPosition?, new: IssPosition): Boolean {
        if (old == null) return true
        val latDiff = Math.abs(old.latitude - new.latitude)
        val lonDiff = Math.abs(old.longitude - new.longitude)
        // 0.001 degrees is roughly 111 meters. Perfect for avoiding unneeded map redraws.
        return latDiff > 0.001 || lonDiff > 0.001
    }
}
