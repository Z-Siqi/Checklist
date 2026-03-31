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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.coroutineScope
import com.sqz.checklist.cache.ONE_DAY_LONG
import com.sqz.checklist.cache.deleteAllFileWhichInProcessFilesPath
import com.sqz.checklist.cache.dropEmptyInProcessFilesPath
import sqz.checklist.data.database.repository.DatabaseRepository
import com.sqz.checklist.ui.MainLayout
import com.sqz.checklist.ui.common.unit.isGestureNavigationMode
import com.sqz.checklist.ui.theme.ChecklistTheme
import com.sqz.checklist.ui.theme.SystemBarsColor
import com.sqz.checklist.ui.theme.Theme
import com.sqz.checklist.ui.theme.UISizeLimit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.getDatabaseBuilder
import sqz.checklist.data.preferences.PrimaryPreferences
import sqz.checklist.data.storage.AppDirType
import sqz.checklist.data.storage.initInternalDirPath
import sqz.checklist.data.storage.manager.StorageManager

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var taskDatabase: DatabaseProvider
    }

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        initInternalDirPath(applicationContext) // load app dir location
        super.onCreate(savedInstanceState)
        // load database
        taskDatabase = DatabaseProvider(getDatabaseBuilder(applicationContext))
        setContent {
            ChecklistTheme {
                SystemBarsColor.CreateSystemBars(window) {
                    Theme.SetSystemBarsColorByPreference()
                }
                val windowInsetsPadding = if (isGestureNavigationMode()) {
                    Modifier
                } else {
                    Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                }
                val statusBarsInsetsPadding =
                    Modifier.windowInsetsPadding(WindowInsets.statusBars)
                Surface(
                    modifier = Modifier.fillMaxSize()
                            then statusBarsInsetsPadding // Do not override state bar area
                            then windowInsetsPadding,
                    color = MaterialTheme.colorScheme.background,
                ) { // Main app UI
                    UISizeLimit {
                        MainLayout(
                            modifier = Modifier,
                            context = applicationContext,
                            view = window.decorView,
                        )
                    }
                }
            }
            val storageManager = StorageManager.provider()
            LaunchedEffect(storageManager.getTempFiles.collectAsState()) {
                this@MainActivity.clearUnusedTemp(storageManager)
            }
        }
        if (savedInstanceState == null) {
            super.lifecycle.coroutineScope.launch(Dispatchers.IO) {
                deleteAllFileWhichInProcessFilesPath(applicationContext)
            }
        }
    }

    private suspend fun clearUnusedTemp(storageManager: StorageManager) {
        try {
            val allTemp = storageManager.getDirResList(AppDirType.Temp)
            val knownTemp = storageManager.getTempFiles.value.map { it.first }
            allTemp.fastForEach {
                if (!knownTemp.any { known -> it == known }) {
                    if (it.endsWith(".tmp")) return@fastForEach
                    val delMode = StorageManager.DeleteMode.FilePath(it)
                    storageManager.deleteTempFile(delMode)
                }
            }
        } catch (e: Exception) {
            Log.w("clearUnusedTemp", "Failed to clear unused temp files: $e")
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
            DatabaseRepository(taskDatabase.getDatabase()).deleteAllHistory()
            Log.d("clearHistoryWhenLeaved", "Clear all history")
        }
    }

    @Override
    override fun onStop() {
        super.onStop()
        val scope = CoroutineScope(SupervisorJob())
        scope.launch {
            // Clear old cache files that created one day before
            val delMode = StorageManager.DeleteMode.BeforeTime(ONE_DAY_LONG)
            StorageManager.provider().deleteCacheFile(delMode)
        }
    }
}
