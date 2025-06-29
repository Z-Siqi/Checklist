package com.sqz.checklist.ui.theme

import android.os.Build
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SystemBarsColor private constructor() {
    private var _init = false

    private data class BarsColor(
        val navBgColor: Color,
        val stateBgColor: Color,
        val lightStateBar: Boolean = true,
        val lightNavBar: Boolean = true,
    )

    private val barsColorFlow = MutableStateFlow(BarsColor(Color.Transparent, Color.Transparent))
    private val barsColor: StateFlow<BarsColor> = barsColorFlow

    companion object {
        val current: SystemBarsColor = SystemBarsColor()

        @Composable
        fun CreateSystemBars(window: Window) {
            if (!current._init) current.barsColorFlow.update {
                current._init = true
                val color = MaterialTheme.colorScheme
                it.copy(navBgColor = color.secondary, stateBgColor = color.secondary)
            }
            current.SetSystemBars(window)
        }
    }

    fun setStateBarColor(color: Color) {
        this.barsColorFlow.update {
            it.copy(stateBgColor = color)
        }
    }

    fun setNavBarColor(color: Color) {
        this.barsColorFlow.update {
            it.copy(navBgColor = color)
        }
    }

    fun setLightBars(lightState: Boolean, lightNav: Boolean) {
        this.barsColorFlow.update {
            it.copy(lightStateBar = lightState, lightNavBar = lightNav)
        }
    }

    @Composable
    private fun SetSystemBars(window: Window) {
        // Set system bars background colors
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.SetSystemBarsColor(window)
        } else {
            this.DrawSystemBarsBgColor(window)
        }
        // Set system bars UI light or dark mode
        val barsColor = barsColor.collectAsState().value
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !barsColor.lightStateBar
        controller.isAppearanceLightNavigationBars = !barsColor.lightNavBar
    }

    @RequiresApi(35)
    @Composable
    private fun DrawSystemBarsBgColor(window: Window) {
        val barsColor = barsColor.collectAsState().value
        Spacer( // Add state bar for Android 15
            modifier = Modifier.fillMaxSize() then Modifier.background(barsColor.stateBgColor)
        )
        @Suppress("DEPRECATION") // Set nav bar color for non-gesture nav mode
        window.navigationBarColor = barsColor.navBgColor.toArgb()
    }

    @Suppress("DEPRECATION")
    @Composable
    private fun SetSystemBarsColor(window: Window) {
        // set state bar color for low android version
        val barsColor = barsColor.collectAsState().value
        window.statusBarColor = barsColor.stateBgColor.toArgb()
        window.navigationBarColor = barsColor.navBgColor.toArgb()
    }
}
