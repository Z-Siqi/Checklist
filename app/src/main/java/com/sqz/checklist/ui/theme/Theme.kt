package com.sqz.checklist.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sqz.checklist.preferences.PrimaryPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

@OptIn(ExperimentalMaterial3Api::class)
class Theme private constructor(preference: Int) {
    private val localPreference = preference

    companion object {
        val color: Theme @Composable get() = Theme(ThemePreference.preference())

        @Composable
        fun SetSystemBarsColorByPreference() {
            SystemBarsColor.current.setNavBarColor(color.navBarBgColor)
            SystemBarsColor.current.setStateBarColor(color.sysStateBarBgColor)
            if (ThemePreference.preference() == 0) {
                SystemBarsColor.current.setLightBars(
                    lightNav = isSystemInDarkTheme(), lightState = isSystemInDarkTheme()
                )
            } else {
                SystemBarsColor.current.setLightBars(
                    lightNav = true, lightState = !isSystemInDarkTheme()
                )
            }
        }
    }

    val sysStateBarBgColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val backgroundColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surface
        }
    val pinnedBackgroundColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.background
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        }
    val remindedBackgroundColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        }
    val remindedBorderColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.tertiary
        }
    val taskBackgroundColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        }
    val taskBorderColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.surfaceDim
            else -> MaterialTheme.colorScheme.let {
                if (isSystemInDarkTheme()) it.surfaceContainerHighest
                else it.surfaceDim
            }
        }
    val navBarBgColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        }
    val navBarContentColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }
    val navBarItemColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.inversePrimary
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    val navBarSelectedIconColor: Color
        @Composable get() = when (localPreference) {
            1 -> MaterialTheme.colorScheme.inverseSurface
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        }
    val navBarDisabledIconColor: Color @Composable get() = MaterialTheme.colorScheme.primary

    @Composable
    fun topBarBgColors(topBarForFixedSize: Boolean): TopAppBarColors {
        return when (localPreference) {
            1 -> TopAppBarDefaults.topAppBarColors(
                containerColor = if (topBarForFixedSize) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            )

            else -> TopAppBarDefaults.topAppBarColors(
                containerColor = if (topBarForFixedSize) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                titleContentColor = MaterialTheme.colorScheme.secondary,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }
}

object ThemePreference {
    private var _init = false
    private val localPreference = MutableStateFlow(0)
    private val preference: StateFlow<Int> = localPreference

    fun updatePreference(value: Int): Int {
        localPreference.update { value }
        return this.localPreference.value
    }

    fun init(value: Int? = null): Boolean {
        if (value != null) {
            if (_init) throw RuntimeException("Already init!")
            _init = true
            this.updatePreference(value)
        }
        return _init
    }

    @Composable
    fun preference(): Int {
        return preference.collectAsState().value
    }
}

@Composable
fun ChecklistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!ThemePreference.init()) {
        val context = LocalContext.current
        val primaryPreferences = PrimaryPreferences(context)
        ThemePreference.init(primaryPreferences.appTheme())
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            //window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
