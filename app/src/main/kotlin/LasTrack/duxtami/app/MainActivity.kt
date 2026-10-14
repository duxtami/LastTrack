package LasTrack.duxtami.app

import LasTrack.duxtami.app.core.model.AppPreferences
import LasTrack.duxtami.app.core.model.PreferencesRepository
import LasTrack.duxtami.app.navigation.LasTrackApp
import LasTrack.duxtami.app.theme.LastTrackTheme
import LasTrack.duxtami.app.theme.isDarkTheme
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(preferences: PreferencesRepository) : ViewModel() {
    /** Null until the first DataStore read completes, so the first frame never flashes the wrong theme. */
    val preferences: StateFlow<AppPreferences?> = preferences.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _sharedPlaceInput = MutableStateFlow<String?>(null)
    val sharedPlaceInput: StateFlow<String?> = _sharedPlaceInput

    fun setSharedPlaceInput(input: String?) {
        _sharedPlaceInput.value = input
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent { Root() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val text = when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            Intent.ACTION_VIEW -> {
                intent.dataString
            }
            else -> null
        }
        if (!text.isNullOrBlank()) {
            mainViewModel.setSharedPlaceInput(text)
        }
    }

    @Composable
    private fun Root() {
        val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()
        val sharedPlace by mainViewModel.sharedPlaceInput.collectAsStateWithLifecycle()
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
        LastTrackTheme(p.themeMode, p.dynamicColor, p.amoled) {
            LasTrackApp(
                sharedPlaceInput = sharedPlace,
                onConsumeSharedPlace = { mainViewModel.setSharedPlaceInput(null) },
            )
        }
    }
}
