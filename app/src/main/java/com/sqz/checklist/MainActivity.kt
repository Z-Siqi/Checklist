package com.sqz.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.room.Room
import com.sqz.checklist.database.TaskDatabase
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
            TaskDatabase::class.java, "task-database"
        ).build()
        setContent {
            ChecklistTheme {
                window.statusBarColor = MaterialTheme.colorScheme.secondary.toArgb()
                window.navigationBarColor = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.onSecondary.toArgb()
                } else MaterialTheme.colorScheme.secondary.toArgb()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLayout(
                        context = applicationContext,
                        view = window.decorView
                    )
                }
            }
        }
    }
}
