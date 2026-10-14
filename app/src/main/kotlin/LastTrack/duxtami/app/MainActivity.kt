package LastTrack.duxtami.app

import LastTrack.duxtami.app.core.model.AppPreferences
import LastTrack.duxtami.app.core.model.PreferencesRepository
import LastTrack.duxtami.app.navigation.LastTrackApp
import LastTrack.duxtami.app.theme.LastTrackTheme
import LastTrack.duxtami.app.theme.isDarkTheme
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(preferences: PreferencesRepository) : ViewModel() {
    /** Null until the first DataStore read completes, so the first frame never flashes the wrong theme. */
    val preferences: StateFlow<AppPreferences?> = preferences.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Root() }
    }

    @Composable
    private fun Root(viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
        val prefs by viewModel.preferences.collectAsStateWithLifecycle()
        val p = prefs ?: return
        val dark = isDarkTheme(p.themeMode)
        // The in-app theme choice can differ from the system's, so system bar icon colours follow it explicitly.
        DisposableEffect(dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                navigationBarStyle = SystemBarStyle.auto(
                    lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                    darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                ) { dark },
            )
            onDispose {}
        }
        LastTrackTheme(p.themeMode, p.dynamicColor, p.amoled) { LastTrackApp() }
    }
}
