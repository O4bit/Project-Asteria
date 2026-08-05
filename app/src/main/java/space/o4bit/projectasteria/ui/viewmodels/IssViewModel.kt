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
    val errorMessage: String? = null,
    /** Last N positions for orbit trail — newest first */
    val orbitTrail: List<IssPosition> = emptyList()
)

/** Maximum orbit trail length (one position every ~3 s → ~60 points ≈ ~3 min of trail) */
private const val MAX_TRAIL = 60

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
            var currentDelay = 60000L
            val maxDelay = 300000L
            var lastLocation: IssPosition? = null
            val trail = ArrayDeque<IssPosition>(MAX_TRAIL)

            if (lastLocation == null) {
                emit(IssUiState(location = null, isLive = false, errorMessage = null))
            }

            while (currentCoroutineContext().isActive) {
                try {
                    val location = repository.getIssPosition()

                    if (hasMeaningfulChange(lastLocation, location)) {
                        // Prepend new position to trail, trim to MAX_TRAIL
                        trail.addFirst(location)
                        if (trail.size > MAX_TRAIL) trail.removeLast()

                        emit(IssUiState(
                            location = location,
                            isLive = true,
                            lastUpdateTime = System.currentTimeMillis(),
                            errorMessage = null,
                            orbitTrail = trail.toList()
                        ))
                        lastLocation = location
                    }
                    currentDelay = 60000L
                } catch (e: Exception) {
                    emit(IssUiState(
                        location = lastLocation,
                        isLive = false,
                        lastUpdateTime = System.currentTimeMillis(),
                        errorMessage = "Signal lost. Retrying in ${currentDelay/1000}s...",
                        orbitTrail = trail.toList()
                    ))
                    currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                }
                delay(currentDelay)
            }
        }
    }.stateIn(
        scope = viewModelScope,
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
        return latDiff > 0.001 || lonDiff > 0.001
    }
}
