package com.sqz.checklist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.coroutineScope
import com.sqz.checklist.cache.clearExpiredCache
import com.sqz.checklist.cache.clearOldCacheIfNeeded
import com.sqz.checklist.cache.deleteAllFileWhichInProcessFilesPath
import com.sqz.checklist.cache.dropEmptyInProcessFilesPath
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.TaskDatabase
import com.sqz.checklist.database.buildDatabase
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.MainLayout
import com.sqz.checklist.ui.common.unit.isGestureNavigationMode
import com.sqz.checklist.ui.main.appScreenSizeLimit
import com.sqz.checklist.ui.theme.ChecklistTheme
import com.sqz.checklist.ui.theme.SystemBarsColor
import com.sqz.checklist.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
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
            ChecklistTheme {
                SystemBarsColor.CreateSystemBars(window) {
                    Theme.SetSystemBarsColorByPreference()
                }
                val windowInsetsPadding = if (isGestureNavigationMode()) Modifier else {
                    Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                }
                val statusBarsInsetsPadding = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                if (!appScreenSizeLimit()) Surface(
                    modifier = Modifier.fillMaxSize()
                            then statusBarsInsetsPadding // Do not override state bar area
                            then windowInsetsPadding,
                    color = MaterialTheme.colorScheme.background,
                ) { // Main app UI
                    MainLayout(
                        modifier = Modifier,
                        context = applicationContext,
                        view = window.decorView,
                    )
                }
            }
        }
        if (savedInstanceState == null) {
            super.lifecycle.coroutineScope.launch(Dispatchers.IO) {
                deleteAllFileWhichInProcessFilesPath(applicationContext)
            }
        }
    }

    @Override
    override fun onDestroy() {
        super.onDestroy()
        this.clearHistoryWhenLeaved()
    }

    @Override
    override fun onStart() {
        super.onStart()
        super.lifecycle.coroutineScope.launch(Dispatchers.IO) {
            dropEmptyInProcessFilesPath(applicationContext)
        }
        this.clearHistoryWhenLeaved()
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
