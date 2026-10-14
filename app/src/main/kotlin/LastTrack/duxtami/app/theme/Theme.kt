package LastTrack.duxtami.app.theme

import LastTrack.duxtami.app.core.model.ThemeMode
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A60), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF74F8E5), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A6360), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E3), onSecondaryContainer = Color(0xFF05201D),
    tertiary = Color(0xFF456179), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF), onTertiaryContainer = Color(0xFF001E31),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4FBF9), onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF4FBF9), onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E1), onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977), outlineVariant = Color(0xFFBEC9C5),
    inverseSurface = Color(0xFF2B3231), inverseOnSurface = Color(0xFFECF2F0), inversePrimary = Color(0xFF53DBC9),
    surfaceTint = Color(0xFF006A60),
    surfaceDim = Color(0xFFD5DBD9), surfaceBright = Color(0xFFF4FBF9),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFEEF5F3),
    surfaceContainer = Color(0xFFE8F0ED), surfaceContainerHigh = Color(0xFFE3EAE8),
    surfaceContainerHighest = Color(0xFFDDE4E2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF53DBC9), onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048), onPrimaryContainer = Color(0xFF74F8E5),
    secondary = Color(0xFFB1CCC7), onSecondary = Color(0xFF1C3532),
    secondaryContainer = Color(0xFF324B48), onSecondaryContainer = Color(0xFFCCE8E3),
    tertiary = Color(0xFFAECAE6), onTertiary = Color(0xFF153349),
    tertiaryContainer = Color(0xFF2D4961), onTertiaryContainer = Color(0xFFCCE5FF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1514), onBackground = Color(0xFFDDE4E2),
    surface = Color(0xFF0E1514), onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4947), onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF899390), outlineVariant = Color(0xFF3F4947),
    inverseSurface = Color(0xFFDDE4E2), inverseOnSurface = Color(0xFF2B3231), inversePrimary = Color(0xFF006A60),
    surfaceTint = Color(0xFF53DBC9),
    surfaceDim = Color(0xFF0E1514), surfaceBright = Color(0xFF343B3A),
    surfaceContainerLowest = Color(0xFF090F0F), surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120), surfaceContainerHigh = Color(0xFF252B2B),
    surfaceContainerHighest = Color(0xFF303736),
)

/** Pure-black surfaces for OLED panels, keeping the container hierarchy readable by darkening proportionally. */
private fun ColorScheme.toAmoled(): ColorScheme {
    val black = Color.Black
    return copy(
        background = black,
        surface = black,
        surfaceDim = black,
        surfaceContainerLowest = black,
        surfaceContainerLow = lerp(surfaceContainerLow, black, 0.75f),
        surfaceContainer = lerp(surfaceContainer, black, 0.6f),
        surfaceContainerHigh = lerp(surfaceContainerHigh, black, 0.45f),
        surfaceContainerHighest = lerp(surfaceContainerHighest, black, 0.3f),
    )
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun LastTrackTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    amoled: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme(themeMode)
    val context = LocalContext.current
    var scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    if (dark && amoled) scheme = scheme.toAmoled()
    MaterialTheme(colorScheme = scheme, typography = LastTrackTypography, content = content)
}
