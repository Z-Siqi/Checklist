package com.sqz.checklist.database

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.asLiveData
import com.sqz.checklist.MainActivity.Companion.taskDatabase
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotifyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DatabaseIO private constructor() {
    companion object {
        @Volatile
        private var instance: DatabaseIO? = null
        fun instance(): DatabaseIO = instance ?: synchronized(this) {
            instance ?: DatabaseIO().also { instance = it }
        }
    }

    private var _loadingState = MutableStateFlow(0) // 0 to 100; Err: -1
    private var _dbState = MutableStateFlow(IOdbState.Default)

    fun setLoading(setter: Int) {
        if (setter > 100 || setter <= -1) throw IllegalArgumentException("Value cannot more than 100 or less than 0!")
        else _loadingState.update { setter }
    }

    private fun setIOdbState(state: IOdbState) {
        this._dbState.update { state }
    }

    fun getIOdbState(): Flow<IOdbState> {
        return this._dbState
    }

    fun getLoadingState(): Flow<Int> {
        return this._loadingState
    }

    fun exportDatabase(
        uri: Uri?, coroutineScope: CoroutineScope, context: Context
    ) = try {
        if (_loadingState.value == 0) coroutineScope.launch {
            setIOdbState(IOdbState.Processing)
            setLoading(1) // close database
            taskDatabase.close()
            setLoading(5) // merge database checkpoint ("PRAGMA wal_checkpoint(FULL)")
            mergeDatabaseCheckpoint(taskDatabase)
            setLoading(10) // re-open database
            taskDatabase = buildDatabase(context = context)
            setLoading(20) // start backup
            val dbPath = context.getDatabasePath(taskDatabaseName).absolutePath
            val mediaDir = File(context.filesDir, "media/")
            val zipFile = File(context.cacheDir, "backup.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                setLoading(50) // backup database
                File(dbPath).inputStream().use { input ->
                    zipOut.putNextEntry(ZipEntry("$taskDatabaseName.db"))
                    input.copyTo(zipOut)
                    zipOut.closeEntry()
                }
                setLoading(70) // backup media files
                if (mediaDir.exists()) {
                    addFolderToZip(zipOut, mediaDir, "media/")
                }
            }
            setLoading(90) // open export
            if (uri != null) {
                setLoading(95) // when rememberLauncherForActivityResult
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    zipFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                val intent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                        context, "${context.packageName}.provider", zipFile)
                    )
                    type = "application/octet-stream"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val shareIntent = Intent.createChooser(intent, null)
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
            }
            setLoading(100) // finished
            setIOdbState(IOdbState.Finished)
        } else if (_dbState.value != IOdbState.Processing) {
            Log.d("DatabaseIO", "Note: reset before next run")
        } else {
            Log.d("ChecklistDatabase", "Exporting...")
        }
    } catch (e: Exception) {
        _loadingState.update { -1 }
        Log.e("ChecklistDatabase", "ERROR: $e")
        setIOdbState(IOdbState.Error)
    }

    private fun addFolderToZip(zipOut: ZipOutputStream, folder: File, basePath: String) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                addFolderToZip(zipOut, file, "$basePath${file.name}/")
            } else {
                file.inputStream().use { input ->
                    zipOut.putNextEntry(ZipEntry("$basePath${file.name}"))
                    input.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
    }

    fun importDatabase(
        uri: Uri, coroutineScope: CoroutineScope, context: Context
    ) = try {
        if (_loadingState.value == 0) coroutineScope.launch {
            setIOdbState(IOdbState.Processing)
            setLoading(1) // getting backup file
            val dbPath = context.getDatabasePath(taskDatabaseName).absolutePath
            val mediaDir = File(context.filesDir, "media/")
            val zipFile = File(context.cacheDir, "restore.zip")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                zipFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            }
            setLoading(20) // cancel all notification
            cancelAllNotification(taskDatabase, context)
            setLoading(25) // close database
            taskDatabase.close()
            setLoading(30) // merge database checkpoint ("PRAGMA wal_checkpoint(FULL)")
            mergeDatabaseCheckpoint(taskDatabase)
            setLoading(35) // remove media files
            clearMediaFolder(mediaDir)
            setLoading(50) // import backup
            ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name
                    val outputFile = if (entryName == "$taskDatabaseName.db") {
                        File(dbPath)
                    } else {
                        File(mediaDir, entryName.removePrefix("media/"))
                    }
                    outputFile.parentFile?.mkdirs()
                    if (entry!!.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.outputStream().use { output ->
                            zipIn.copyTo(output)
                        }
                    }
                    zipIn.closeEntry()
                }
            }
            setLoading(80) // re-open database
            taskDatabase = buildDatabase(context)
            setLoading(85) // check database
            if (!isDatabaseValid(dbPath)) {
                setIOdbState(IOdbState.Error)
                Log.e("ChecklistDatabase", "Failed to import database: Invalid database!")
                Toast.makeText(context, "Invalid database!", Toast.LENGTH_SHORT).show()
                taskDatabase.clearAllTables()
            }
            setLoading(90) // restore notification
            restoreNotification(taskDatabase, context)
            setLoading(100) // finished
            setIOdbState(IOdbState.Finished)
        } else if (_dbState.value != IOdbState.Processing) {
            Log.d("DatabaseIO", "Note: reset before next run")
        } else {
            Log.d("ChecklistDatabase", "Importing...")
        }
    } catch (e: Exception) {
        _loadingState.update { -1 }
        setIOdbState(IOdbState.Error)
        Log.e("ChecklistDatabase", "ERROR: $e")
    }

    private fun clearMediaFolder(mediaDir: File) {
        if (mediaDir.exists()) {
            mediaDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    clearMediaFolder(file)
                }
                file.delete()
            }
        }
    }

    fun releaseMemory() {
        this._loadingState.update { 0 }
        this._dbState.update { IOdbState.Default }
    }
}

private const val timeFormat = "yyyyMMdd_HHmm" + "ss" // No Android Studio grammar checking this!

@Composable
fun ExportTaskDatabase(
    state: Boolean, useChooser: Boolean, view: View,
    dbState: (state: IOdbState, loading: Int) -> Unit = { _, _ -> }
) {
    val exportName = "Checklist_Backup"
    val databaseIO = DatabaseIO.instance()
    var canceled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("db/sqlite")
    ) { selectedUri: Uri? ->
        selectedUri?.let {
            databaseIO.exportDatabase(selectedUri, coroutineScope, view.context)
        }
        coroutineScope.launch {
            if (databaseIO.getIOdbState().asLiveData(this.coroutineContext).value == IOdbState.Default) {
                databaseIO.setLoading(100)
                Log.d("ExportTaskDatabase", "Export canceled")
                canceled = true
            }
        }
    }
    if (state) { // Export actions
        if (useChooser) databaseIO.exportDatabase(null, coroutineScope, view.context) else {
            val currentTime = remember {
                val sdf = SimpleDateFormat(timeFormat, Locale.getDefault())
                sdf.format(Date())
            }
            LaunchedEffect(Unit) {
                launcher.launch("${exportName}_$currentTime.zip")
            }
        }
    }
    dbState(
        databaseIO.getIOdbState().collectAsState(IOdbState.Default).value,
        databaseIO.getLoadingState().collectAsState(0).value
    )
    if (canceled || databaseIO.getIOdbState().collectAsState(IOdbState.Default).value == IOdbState.Finished) {
        databaseIO.releaseMemory()
        canceled = false
    }
}

@Composable
fun ImportTaskDatabase(
    selectClicked: Boolean, importClicked: Boolean,
    selected: (text: String?) -> Unit, dbState: (IOdbState, loading: Int) -> Unit,
    view: View
) {
    val coroutineScope = rememberCoroutineScope()
    var uri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { selectedUri: Uri? ->
        uri = selectedUri
        val text = if (uri == null) null else try {
            uri!!.path.toString().replace("/document/primary:", "")
        } catch (e: Exception) {
            Log.w("ChecklistDataImport", "Failed to show selected file: $e")
            ""
        }
        selected(text)
    }
    if (selectClicked) LaunchedEffect(Unit) {
        launcher.launch("application/zip")
    }
    val databaseIO = DatabaseIO.instance()
    val getIOdbState = databaseIO.getIOdbState().collectAsState(IOdbState.Default).value
    if (importClicked) {
        if (uri != null) uri?.let {
            databaseIO.importDatabase(uri!!, coroutineScope, view.context)
            if (getIOdbState == IOdbState.Finished) {
                databaseIO.releaseMemory()
                uri = null
            }
        } else Toast.makeText(
            view.context, view.context.getString(R.string.select_file_to_import),
            Toast.LENGTH_SHORT
        ).show()
        dbState(getIOdbState, databaseIO.getLoadingState().collectAsState(0).value)
    }
}

private suspend fun restoreNotification(dbInstance: TaskDatabase, context: Context) {
    val notificationManager = MutableStateFlow(NotifyManager())
    notificationManager.value.requestPermission(context)
    val databaseRepository = DatabaseRepository(dbInstance)
    for (data in dbInstance.taskReminderDao().getAll()) {
        if (!data.isReminded) try {
            val restore = notificationManager.value.createNotification(
                channelId = context.getString(R.string.tasks),
                channelName = context.getString(R.string.task_reminder),
                channelDescription = context.getString(R.string.description),
                description = data.description, notifyId = data.id,
                delayDuration = data.reminderTime,
                timeUnit = TimeUnit.MILLISECONDS, context = context
            ).also { Log.d("RestoreReminder", "Restore NotifyId: ${data.id}") }
            val mode =
                if (notificationManager.value.getAlarmPermission()) ReminderModeType.AlarmManager else {
                    ReminderModeType.Worker
                }
            dbInstance.taskReminderDao().updateMode(data.id, mode, restore)
            if (data.reminderTime < System.currentTimeMillis()) {
                databaseRepository.setIsReminded(data.id, true)
            }
        } catch (e: Exception) {
            Log.w("RestoreReminder", "Exception: $e")
        }
    }
}

private suspend fun cancelAllNotification(dbInstance: TaskDatabase, context: Context) {
    try {
        val notificationManager = MutableStateFlow(NotifyManager())
        notificationManager.value.requestPermission(context)
        for (data in dbInstance.taskReminderDao().getAll()) {
            when (data.mode) {
                ReminderModeType.AlarmManager -> notificationManager.value.cancelNotification(
                    data.id.toString(), context, data.id
                )

                ReminderModeType.Worker -> notificationManager.value.cancelNotification(
                    data.extraData!!, context, data.id, true
                )
            }
        }
    } catch (e: Exception) {
        Log.w("CancelReminder", "Exception: $e")
    }
}

private fun isDatabaseValid(databasePath: String): Boolean {
    return try {
        val db = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = db.rawQuery("PRAGMA integrity_check;", null)
        cursor.use {
            if (it.moveToFirst()) {
                val result = it.getString(0)
                db.close()
                result == "ok"
            } else {
                db.close()
                false
            }
        }
    } catch (e: Exception) {
        Log.e("ChecklistDatabase", "ERROR: $e")
        false
    }
}

enum class IOdbState {
    Default, Processing, Finished, Error
}
