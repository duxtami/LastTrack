package LastTrack.duxtami.app.ui.home

import LastTrack.duxtami.app.core.model.RouteRepository
import LastTrack.duxtami.app.core.model.RouteSummary
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(repository: RouteRepository) : ViewModel() {
    /** Null while loading; routes are already sorted newest first. */
    val recentRoutes: StateFlow<List<RouteSummary>?> = repository.observeRoutes()
        .map<List<RouteSummary>, List<RouteSummary>?> { it.take(RECENT_COUNT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private companion object {
        const val RECENT_COUNT = 5
    }
}
