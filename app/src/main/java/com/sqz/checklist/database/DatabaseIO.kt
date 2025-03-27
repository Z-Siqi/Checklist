package com.sqz.checklist.database

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import android.view.View
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
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity.Companion.taskDatabase
import com.sqz.checklist.R
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.ui.main.task.layout.function.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

class DatabaseIO private constructor(application: Application) : AndroidViewModel(application) {
    companion object {
        @Volatile
        private var instance: DatabaseIO? = null
        fun instance(application: Application): DatabaseIO = instance ?: synchronized(this) {
            instance ?: DatabaseIO(application).also { instance = it }
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

    private suspend fun removeInvalidFile(context: Context) {
        val mediaRoot = File(context.filesDir, "media")
        if (!mediaRoot.exists() || !mediaRoot.isDirectory) return
        val allFiles = mediaRoot.walk()
            .filter { it.isFile }
            .toList()
        var dataList: List<Uri> = listOf()
        for (data in taskDatabase.taskDao().getTaskDetail()) {
            when (data.type) {
                TaskDetailType.Text -> {}
                TaskDetailType.URL -> {}
                TaskDetailType.Application -> {}
                else -> data.dataByte?.let {
                    dataList = dataList + it.toUri(context.filesDir.absolutePath)
                }
            }
        }
        for (file in allFiles) {
            if (file.toUri() !in dataList.toSet()) try {
                file.delete()
                Log.d("removeInvalidFile", "Deleted orphan file: ${file.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportDatabase(
        uri: Uri?, context: Context
    ) = try {
        if (_loadingState.value == 0) viewModelScope.launch(Dispatchers.IO) {
            setIOdbState(IOdbState.Processing)
            setLoading(1) // remove invalid file
            removeInvalidFile(context)
            setLoading(3) // close database
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
                    putExtra(
                        Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", zipFile
                        )
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

    private var restoreJob: Job? = null
    fun importDatabase(
        uri: Uri, context: Context
    ) = try {
        if (restoreJob == null && _loadingState.value == 0) restoreJob =
            viewModelScope.launch(Dispatchers.IO) {
                setIOdbState(IOdbState.Processing)
                setLoading(1) // checking backup file
                if (!verifyImportZip(context, uri)) {
                    setIOdbState(IOdbState.Error)
                    _loadingState.update { -1 }
                    restoreJob?.cancel()
                } else {
                    setLoading(10) // getting backup file
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
                    //val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
                    taskDatabase.close()
                    //db.close()
                    Thread.sleep(500)
                    setLoading(30) // merge database checkpoint ("PRAGMA wal_checkpoint(FULL)")
                    mergeDatabaseCheckpoint(taskDatabase)
                    setLoading(35) // remove old data
                    deleteDbFiles(File(dbPath))
                    clearMediaFolder(mediaDir)
                    setLoading(50) // import backup
                    ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                        var entry: ZipEntry?
                        while (zipIn.nextEntry.also { entry = it } != null) {
                            val entryName = entry!!.name
                            val outputFile = if (entryName == "$taskDatabaseName.db") {
                                setLoading(55)
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
                    setLoading(75) // delete cache
                    zipFile.delete()
                    setLoading(80) // re-open database
                    taskDatabase = buildDatabase(context)
                    setLoading(85) // check database
                    if (!isDatabaseValid(dbPath)) {
                        setIOdbState(IOdbState.Error)
                        Log.e("ChecklistDatabase", "Failed to import database: Invalid database!")
                        deleteDbFiles(File(dbPath))
                    }
                    setLoading(90) // restore notification
                    restoreNotification(taskDatabase, context)
                    setLoading(95) // remove invalid file
                    removeInvalidFile(context)
                    setLoading(100) // finished
                    setIOdbState(IOdbState.Finished)
                    restoreJob?.cancel()
                }
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

    private fun deleteDbFiles(dbFile: File) {
        if (dbFile.exists()) {
            val dbDirectory = dbFile.parentFile
            val dbName = dbFile.nameWithoutExtension
            dbDirectory?.listFiles()?.forEach { file ->
                if (file.name.startsWith(dbName)) {
                    file.delete()
                    Log.d("DB_DELETE", "deleted file: ${file.absolutePath}")
                }
            }
        }
    }

    private fun verifyImportZip(context: Context, uri: Uri): Boolean {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry: ZipEntry?
                    var hasDatabase = false
                    var rootFileCount = 0
                    var rootFolderCount = 0
                    while (zipIn.nextEntry.also { entry = it } != null) {
                        val entryName = entry!!.name
                        // check zip work
                        if (entry!!.method == ZipEntry.DEFLATED && entry!!.extra != null) {
                            Log.e("DatabaseIO", "Cannot unzip!")
                            return false
                        }
                        // check database file
                        if (entryName == "$taskDatabaseName.db") {
                            hasDatabase = true
                        }
                        // check directory
                        if (!entryName.contains("/")) {
                            if (entry!!.isDirectory) {
                                rootFolderCount++
                            } else {
                                rootFileCount++
                            }
                        }
                        // closeEntry
                        zipIn.closeEntry()
                    }
                    return when {
                        !hasDatabase -> {
                            Log.e("DatabaseIO", "Database file not found!")
                            false
                        }

                        rootFolderCount > 1 || rootFileCount > 1 -> {
                            val errText = "Zip type incorrect: $rootFolderCount, $rootFileCount"
                            Log.e("DatabaseIO", errText)
                            false
                        }

                        else -> true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DatabaseIO", "Zip File ERR: $e")
            return false
        }
        return false
    }

    fun releaseMemory() {
        this._loadingState.update { 0 }
        this._dbState.update { IOdbState.Default }
        this.restoreJob = null
    }
}

private const val timeFormat = "yyyyMMdd_HHmm" + "ss" // No Android Studio grammar checking this!

@Composable
fun ExportTaskDatabase(
    state: Boolean, useChooser: Boolean, view: View,
    dbState: (state: IOdbState, loading: Int) -> Unit = { _, _ -> }
) {
    val exportName = "Checklist_Backup"
    val databaseIO = DatabaseIO.instance(Application())
    var canceled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { selectedUri: Uri? ->
        selectedUri?.let {
            databaseIO.exportDatabase(selectedUri, view.context)
        }
        coroutineScope.launch {
            if (databaseIO.getIOdbState().asLiveData(this.coroutineContext).value
                == IOdbState.Default
            ) {
                databaseIO.setLoading(100)
                Log.d("ExportTaskDatabase", "Export canceled")
                canceled = true
            }
        }
    }
    if (state) { // Export actions
        if (useChooser) databaseIO.exportDatabase(null, view.context) else {
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
    if (canceled || databaseIO.getIOdbState().collectAsState(IOdbState.Default).value
        == IOdbState.Finished && state
    ) {
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
    val databaseIO = DatabaseIO.instance(Application())
    val getIOdbState = databaseIO.getIOdbState().collectAsState(IOdbState.Default).value
    if (importClicked && uri != null) {
        databaseIO.importDatabase(uri!!, view.context)
        if (getIOdbState == IOdbState.Finished) {
            databaseIO.releaseMemory()
            uri = null
        }
        dbState(getIOdbState, databaseIO.getLoadingState().collectAsState(0).value)
    } else if (uri == null) {
        dbState(IOdbState.Default, 100)
    }
}

private suspend fun restoreNotification(dbInstance: TaskDatabase, context: Context) {
    Log.d("RestoreReminder", "trying to restore all reminder")
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
    Log.d("CancelReminder", "trying to cancel all reminder")
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
