package com.sqz.checklist

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.coroutineScope
import com.sqz.checklist.cache.clearExpiredCache
import com.sqz.checklist.cache.clearOldCacheIfNeeded
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.TaskDatabase
import com.sqz.checklist.database.buildDatabase
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.MainLayout
import com.sqz.checklist.ui.main.appScreenSizeLimit
import com.sqz.checklist.ui.theme.ChecklistTheme
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
            var getNavHeight by remember { mutableIntStateOf(0) }
            val navigationBars = WindowInsets.navigationBars.toString()
            try {
                getNavHeight = navigationBars.replace("0", "").replace(Regex("\\D"), "").toInt()
            } catch (e: NumberFormatException) {
                val report =
                    "Report this error to developer, if this log happened frequently, especially when not rotate the screen."
                Log.w("MainActivity", "Failed to get navigation bar height. $report")
                getNavHeight = 0
            }
            ChecklistTheme {
                val stateBarColor = MaterialTheme.colorScheme.secondary
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Spacer( // Add state bar for Android 15
                    modifier = Modifier.fillMaxSize() then Modifier.background(stateBarColor)
                )
                val windowInsetsPadding = if (getNavHeight > 100) // if nav mode is not gesture mode
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
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // set state bar color for low android version
                    window.statusBarColor = stateBarColor.toArgb()
                    window.navigationBarColor = if (isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.onSecondary.toArgb()
                    } else MaterialTheme.colorScheme.secondary.toArgb()
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

    @Override
    override fun onStop() {
        super.onStop()
        clearExpiredCache(applicationContext)
        clearOldCacheIfNeeded(applicationContext)
    }

    private fun clearHistoryWhenLeaved() {
        if (PrimaryPreferences(applicationContext).clearHistoryWhenLeaved()) super.lifecycle.coroutineScope.launch {
            DatabaseRepository(taskDatabase).deleteAllHistory()
            Log.d("clearHistoryWhenLeaved", "Clear all history")
        }
    }
}
