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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.sqz.checklist.MainActivity.Companion.taskDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseIO(
    private val dbPath: String,
    private val context: Context,
    private val preBackupFileName: String = "__pre-backup"
) {
    private var _dbState by mutableStateOf(IOdbState.Default)

    fun setIOdbState(state: IOdbState) {
        this._dbState = state
    }

    fun exportDatabase(
        exportName: String, uri: Uri?, useChooser: Boolean,
        checkpoint: () -> Unit // merge database checkpoint ("PRAGMA wal_checkpoint(FULL)")
    ): Exception? {
        _dbState = IOdbState.Processing
        try {
            checkpoint()
            if (useChooser) {
                val exportFile = File(context.cacheDir, "$exportName.db")
                val intent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, exportDatabaseToCache(exportFile))
                    type = "application/octet-stream"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val shareIntent = Intent.createChooser(intent, null)
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
            } else uri?.let {
                val dbFile = File(dbPath)
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    dbFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            _dbState = IOdbState.Finished
        } catch (e: Exception) {
            _dbState = IOdbState.Error
            Log.e("ChecklistDatabase", "ERROR: $e")
            return e
        }
        return null
    }

    private fun exportDatabaseToCache(file: File): Uri {
        FileInputStream(dbPath).use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
            }
        }
        return FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
    }

    var preBackupFileUri: Uri? = null

    fun importDatabase(
        uri: Uri?, closeDatabase: () -> Unit, reOpenDatabase: () -> Boolean,
        importState: (state: IOdbState) -> Unit = {}
    ): Exception? {
        _dbState = IOdbState.Processing
        // Import
        uri?.let { url ->
            try {
                context.contentResolver.openInputStream(url)?.use { input ->
                    closeDatabase()
                    if (preBackupFileName == "__pre-backup") { // Backup before import
                        val exportFile = File(context.cacheDir, "$preBackupFileName.db")
                        preBackupFileUri = exportDatabaseToCache(exportFile)
                    }
                    FileOutputStream(dbPath).use { output ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (input.read(buffer).also { length = it } > 0) {
                            output.write(buffer, 0, length)
                            importState(IOdbState.Processing)
                        }
                    }
                    if (reOpenDatabase()) importState(IOdbState.Finished)
                }
            } catch (e: Exception) {
                importState(IOdbState.Error)
                Log.e("ChecklistDatabase", "ERROR: $e")
                _dbState = IOdbState.Error
                return e
            }
        }
        _dbState = IOdbState.Finished
        return null
    }

    fun getIOdbState(): IOdbState {
        return this._dbState
    }
}

@Composable
fun GetUri(uri: (Uri?) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri: Uri? ->
        uri(selectedUri)
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("application/octet-stream"))
    }
}

private const val timeFormat = "yyyyMMdd_HHmm" + "ss" // No Android Studio grammar checking this!

@Composable
fun ExportTaskDatabase(
    state: Boolean, useChooser: Boolean, view: View,
    dbPath: String = view.context.getDatabasePath(taskDatabaseName).absolutePath,
    dbState: (state: IOdbState) -> Unit = {}
) {
    val exportName = "Checklist-Task_Backup"
    val databaseIO = remember { DatabaseIO(dbPath, view.context) }
    val coroutineScope = rememberCoroutineScope()
    val exportDatabase: (useChooser: Boolean, uri: Uri?) -> Unit = { chooser, uri ->
        databaseIO.exportDatabase(exportName, uri, chooser) {
            taskDatabase.close()
            coroutineScope.launch {
                mergeDatabaseCheckpoint(taskDatabase)
            }
            taskDatabase = buildDatabase(context = view.context)
        }.let {
            if (it != null) {
                Toast.makeText(view.context, "Export failed: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val launcher = rememberLauncherForActivityResult( // Init function
        contract = ActivityResultContracts.CreateDocument("db/sqlite")
    ) { selectedUri: Uri? ->
        exportDatabase(false, selectedUri).also {
            if (databaseIO.getIOdbState() != IOdbState.Error) databaseIO.setIOdbState(IOdbState.Finished)
        }
    }
    if (state) { // Export actions
        if (useChooser) exportDatabase(true, null) else {
            val currentTime = remember {
                val sdf = SimpleDateFormat(timeFormat, Locale.getDefault())
                sdf.format(Date())
            }
            launcher.launch("${exportName}_$currentTime.db").also {
                databaseIO.setIOdbState(IOdbState.Processing)
            }
        }
    }
    if (databaseIO.getIOdbState() != IOdbState.Default) dbState(databaseIO.getIOdbState())
}

@Composable
fun ImportTaskDatabaseAction(
    uri: Uri?, view: View,
    dbState: (state: IOdbState) -> Unit = {}
) {
    val dbPath = view.context.getDatabasePath(taskDatabaseName).absolutePath
    val databaseIO = DatabaseIO(dbPath, view.context)
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(true) {
        databaseIO.importDatabase(
            uri = uri,
            closeDatabase = {
                taskDatabase.close()
                coroutineScope.launch { // merge database checkpoint ("PRAGMA wal_checkpoint(FULL)")
                    mergeDatabaseCheckpoint(taskDatabase)
                }
            },
            reOpenDatabase = {
                taskDatabase = buildDatabase(context = view.context.applicationContext)
                if (!isDatabaseValid(dbPath)) {
                    Log.e("ChecklistDatabase", "Failed to import database: Invalid file!")
                    dbState(IOdbState.Error)
                    Log.w("ChecklistDatabase", "Trying to restore to backup..")
                    DatabaseIO(dbPath, view.context, "").importDatabase(
                        databaseIO.preBackupFileUri, { taskDatabase.close() }, {
                            taskDatabase = buildDatabase(context = view.context.applicationContext)
                            true
                        }
                    )
                }
                true
            },
            importState = { dbState(it) }
        )
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
