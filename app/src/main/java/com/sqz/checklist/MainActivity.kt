package com.sqz.checklist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.coroutineScope
import com.sqz.checklist.cache.clearExpiredCache
import com.sqz.checklist.cache.clearOldCacheIfNeeded
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.TaskDatabase
import com.sqz.checklist.database.buildDatabase
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.MainLayout
import com.sqz.checklist.ui.theme.SystemBarsColor
import com.sqz.checklist.ui.main.appScreenSizeLimit
import com.sqz.checklist.ui.theme.ChecklistTheme
import com.sqz.checklist.ui.theme.Theme
import com.sqz.checklist.ui.theme.ThemePreference
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var taskDatabase: TaskDatabase
        lateinit var appDir: String
    }

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskDatabase = buildDatabase(applicationContext) // load database
        appDir = applicationContext.filesDir.absolutePath // load app dir location
        setContent {
            val notButtonNav = WindowInsets.navigationBars.getBottom(LocalDensity.current) < 100
            ChecklistTheme {
                SystemBarsColor.CreateSystemBars(window)
                val windowInsetsPadding = if (!notButtonNav) // if nav mode is not gesture mode
                    Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier
                if (!appScreenSizeLimit()) Surface(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars) // Do not override state bar area
                        .fillMaxSize() then windowInsetsPadding,
                    color = MaterialTheme.colorScheme.background
                ) { // Main app UI
                    MainLayout(
                        context = applicationContext,
                        view = window.decorView,
                    )
                }
                val theme = Theme.color
                SystemBarsColor.current.setNavBarColor(theme.navBarBgColor)
                SystemBarsColor.current.setStateBarColor(theme.sysStateBarBgColor)
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
    }

    @Override
    override fun onDestroy() {
        super.onDestroy()
        clearHistoryWhenLeaved()
    }

    @Override
    override fun onStart() {
        super.onStart()
        clearHistoryWhenLeaved()
    }

    private fun clearHistoryWhenLeaved() {
        if (PrimaryPreferences(applicationContext).clearHistoryWhenLeaved()) super.lifecycle.coroutineScope.launch {
            DatabaseRepository(taskDatabase).deleteAllHistory()
            Log.d("clearHistoryWhenLeaved", "Clear all history")
        }
    }

    @Override
    override fun onStop() {
        super.onStop()
        clearExpiredCache(applicationContext)
        clearOldCacheIfNeeded(applicationContext)
    }
}
