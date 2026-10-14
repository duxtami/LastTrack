package LasTrack.duxtami.app.ui.importflow

import LasTrack.duxtami.app.core.model.ImportError
import LasTrack.duxtami.app.core.model.ImportProgress
import LasTrack.duxtami.app.core.model.PreferencesRepository
import LasTrack.duxtami.app.core.model.RouteRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ImportStage { READING, PARSING, PROCESSING, STATISTICS, PREPARING }

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class InProgress(val stage: ImportStage, val pointsParsed: Long = 0) : ImportUiState
    data class Failed(val error: ImportError) : ImportUiState
}

sealed interface ImportEvent {
    /** Import succeeded and the user wants routes opened automatically. */
    data class OpenRoute(val routeId: String) : ImportEvent

    /** Import succeeded; offer an "Open" action instead of navigating. */
    data class Imported(val routeId: String) : ImportEvent
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: RouteRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state

    private val _events = Channel<ImportEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var job: Job? = null

    fun import(uri: String) {
        job?.cancel()
        _state.value = ImportUiState.InProgress(ImportStage.READING)
        job = viewModelScope.launch {
            repository.importRoute(uri).collect { progress ->
                when (progress) {
                    ImportProgress.Reading -> _state.value = ImportUiState.InProgress(ImportStage.READING)
                    is ImportProgress.Parsing -> _state.value = ImportUiState.InProgress(ImportStage.PARSING, progress.pointsParsed)
                    ImportProgress.Processing -> _state.value = ImportUiState.InProgress(ImportStage.PROCESSING)
                    ImportProgress.CalculatingStatistics -> _state.value = ImportUiState.InProgress(ImportStage.STATISTICS)
                    ImportProgress.Preparing -> _state.value = ImportUiState.InProgress(ImportStage.PREPARING)
                    is ImportProgress.Completed -> {
                        _state.value = ImportUiState.Idle
                        val auto = preferences.preferences.first().autoOpenImported
                        _events.send(if (auto) ImportEvent.OpenRoute(progress.routeId) else ImportEvent.Imported(progress.routeId))
                    }
                    is ImportProgress.Failed -> _state.value = ImportUiState.Failed(progress.error)
                }
            }
        }
    }

    /** Cancels a running import (its partial files are removed by the repository) or clears an error. */
    fun dismiss() {
        job?.cancel()
        job = null
        _state.value = ImportUiState.Idle
    }
}
