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
import androidx.room.Room
import com.sqz.checklist.database.TaskDatabase
import com.sqz.checklist.database.taskDatabaseName
import com.sqz.checklist.ui.MainLayout
import com.sqz.checklist.ui.theme.ChecklistTheme

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var taskDatabase: TaskDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskDatabase = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java, taskDatabaseName
        ).build()
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
                Surface(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars) // Do not override state bar area
                        .fillMaxSize() then windowInsetsPadding,
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLayout(
                        context = applicationContext,
                        view = window.decorView,
                    )
                }
                window.statusBarColor = stateBarColor.toArgb()
                window.navigationBarColor = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.onSecondary.toArgb()
                } else {
                    MaterialTheme.colorScheme.secondary.toArgb()
                }
            }
        }
    }
}
