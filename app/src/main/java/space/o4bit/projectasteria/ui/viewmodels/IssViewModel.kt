package space.o4bit.projectasteria.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import space.o4bit.projectasteria.data.model.iss.IssPosition
import space.o4bit.projectasteria.data.repository.IssRepository

enum class IssUpdateInterval(val label: String, val intervalMs: Long) {
    TEN_SEC("10 sec", 10_000L),
    THIRTY_SEC("30 sec", 30_000L),
    ONE_MIN("1 min", 60_000L),
    FIVE_MIN("5 min", 300_000L)
}

data class IssUiState(
    val location: IssPosition? = null,
    val isLive: Boolean = false,
    val lastUpdateTime: Long = 0L,
    val errorMessage: String? = null,
    /** Last N positions for orbit trail — newest first */
    val orbitTrail: List<IssPosition> = emptyList(),
    val updateInterval: IssUpdateInterval = IssUpdateInterval.ONE_MIN
)

/** Maximum orbit trail length (one position every ~3 s → ~60 points ≈ ~3 min of trail) */
private const val MAX_TRAIL = 60

class IssViewModel(
    private val repository: IssRepository = IssRepository()
) : ViewModel() {

    private val _ticker = MutableStateFlow(0)
    private val _intervalFlow = MutableStateFlow(IssUpdateInterval.ONE_MIN)

    val updateInterval: StateFlow<IssUpdateInterval> = _intervalFlow

    fun refresh() {
        _ticker.update { it + 1 }
    }

    fun setUpdateInterval(interval: IssUpdateInterval) {
        _intervalFlow.value = interval
        _ticker.update { it + 1 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<IssUiState> = combine(_intervalFlow, _ticker) { interval, _ -> interval }
        .flatMapLatest { interval ->
            flow {
                var backoffDelay = interval.intervalMs
                val maxDelay = 300_000L
                var lastLocation: IssPosition? = null
                val trail = ArrayDeque<IssPosition>(MAX_TRAIL)

                emit(IssUiState(location = null, isLive = false, errorMessage = null, updateInterval = interval))

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
                                orbitTrail = trail.toList(),
                                updateInterval = interval
                            ))
                            lastLocation = location
                        }
                        backoffDelay = interval.intervalMs
                    } catch (e: Exception) {
                        emit(IssUiState(
                            location = lastLocation,
                            isLive = false,
                            lastUpdateTime = System.currentTimeMillis(),
                            errorMessage = "Signal lost. Retrying in ${backoffDelay / 1000}s...",
                            orbitTrail = trail.toList(),
                            updateInterval = interval
                        ))
                        backoffDelay = (backoffDelay * 2).coerceAtMost(maxDelay)
                    }
                    delay(backoffDelay)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = IssUiState()
        )

    private fun hasMeaningfulChange(old: IssPosition?, new: IssPosition): Boolean {
        if (old == null) return true
        val latDiff = Math.abs(old.latitude - new.latitude)
        val lonDiff = Math.abs(old.longitude - new.longitude)
        return latDiff > 0.001 || lonDiff > 0.001
    }
}
