package com.sqz.checklist.ui.common.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.cache.deleteCacheFileByName
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.common.unit.pxToDpInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.IllegalStateException

@Composable
fun PictureSelector(
    selector: PictureSelector,
    view: View,
    modifier: Modifier = Modifier,
) {
    val dataUri by selector.dataUri.collectAsState()
    var checkSize by remember { mutableStateOf<Long?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            try {
                uri?.let {
                    view.context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            checkSize = cursor.getLong(sizeIndex)
                        }
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            selector.setPictureName(cursor.getString(nameIndex))
                        }
                    }
                    selector.setDataUri(uri)
                }
            } catch (e: Exception) {
                selector.clear()
                Log.e("PictureHelper", "Failed to select a picture: $e")
                Toast.makeText(
                    view.context, view.context.getString(R.string.failed_large_file_size, "50"),
                    Toast.LENGTH_LONG
                ).show()
                Toast.makeText(
                    view.context, view.context.getString(R.string.report_normal_file_size, "50"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                launcher.launch("image/*")
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (dataUri != null) Image(
            rememberAsyncImagePainter(dataUri),
            stringResource(R.string.selected_picture), modifier.fillMaxSize()
        ) else Text(
            stringResource(R.string.click_select_picture), color = MaterialTheme.colorScheme.outline
        )
    }
    val pictureSizeLimitStr = stringResource(R.string.picture_size_limit)
    if (checkSize != null) LaunchedEffect(Unit) {
        val size = checkSize ?: 1
        if ((size / 1024 / 1024) > 50) {
            selector.clear()
            Toast.makeText(
                view.context, pictureSizeLimitStr, Toast.LENGTH_SHORT
            ).show()
        }
        checkSize = null
    }
}

class PictureSelector {
    constructor() // default constructor

    constructor(uriIn: Uri?, nameIn: String?) { // constructor with parameters
        this._dataUri.value = uriIn
        this._pictureName.value = nameIn
    }

    private var _dataUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val dataUri = _dataUri.asStateFlow()

    fun setDataUri(uri: Uri) {
        this._dataUri.update { uri }
    }

    private var _pictureName: MutableStateFlow<String?> = MutableStateFlow(null)
    val pictureName = _pictureName.asStateFlow()

    fun setPictureName(string: String) {
        this._pictureName.update { string }
    }

    fun clear() {
        this._dataUri.value = null
        this._pictureName.value = null
    }
}

@Composable
fun PictureViewDialog(
    onDismissRequest: () -> Unit,
    byteArray: ByteArray,
    imageName: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    if (byteArray.size <= 1) throw IllegalStateException("Invalid byteArray data!")
    val view = LocalView.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenHeightDp = containerSize.height.pxToDpInt()
    val height = when {
        screenHeightDp >= 700 -> (screenHeightDp / 5.8).toInt()
        screenHeightDp < (containerSize.width.pxToDpInt() / 1.2) -> (screenHeightDp / 3.2).toInt()
        else -> (screenHeightDp / 5.1).toInt()
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDismissRequest()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        text = {
            OutlinedCard(
                modifier = modifier.fillMaxWidth() then modifier.height(height.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .clickable { openImageBySystem(imageName, byteArray, view.context) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        rememberAsyncImagePainter(byteArray.toUri(MainActivity.appDir)),
                        imageName
                    )
                }
            }
        },
        title = { Text(title) },
    )
}

fun openImageBySystem(imageName: String, byteArray: ByteArray, context: Context) {
    val convertUri = byteArray.toUri(MainActivity.appDir)
    val name = if (imageName == "") "unknown_name" else {
        if (byteArray.toUri().path.toString().endsWith("jpg")
        ) imageName.replace(imageName.substringAfterLast('.', ""), "jpg") else imageName
    }
    val cache = PreferencesInCache(context)
    val getCacheName = cache.waitingDeletedCacheName()
    if (getCacheName != null && getCacheName != name) {
        deleteCacheFileByName(context, getCacheName)
        cache.waitingDeletedCacheName(null)
    }
    try {
        val file = File(context.cacheDir, name)
        fun uri(file: File): Uri {
            val saved = File(convertUri.path!!)
            val inputStream = FileInputStream(saved)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri(file), "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)))
        cache.waitingDeletedCacheName(name)
    } catch (_: FileNotFoundException) {
        Toast.makeText(context, context.getString(R.string.failed_open), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun insertPicture(context: Context, uri: Uri, compression: Int): Uri? {
    val mediaDir = File(context.filesDir, pictureMediaPath)
    if (!mediaDir.exists()) mediaDir.mkdirs()
    val toCompression = compression in 1..100
    val fileName = when {
        uri.path?.endsWith(".jpg") ?: false -> "IMG_${System.currentTimeMillis()}.jpg"
        toCompression -> "IMG_${System.currentTimeMillis()}.jpg"
        else -> "IMG_${System.currentTimeMillis()}"
    }
    val cache = PreferencesInCache(context)
    fun errFileNameSaver(name: String?) { // clear invalid file
        cache.errFileNameSaver()?.let {
            val file = File(mediaDir, it)
            if (file.exists()) file.delete()
        }
        cache.errFileNameSaver(name)
    }
    errFileNameSaver(fileName)
    val file = File(mediaDir, fileName)
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (!toCompression) {
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            cache.errFileNameSaver(null)
        } else {
            val quality = 100 - compression
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            cache.errFileNameSaver(null)
        }
        Uri.fromFile(file)
    } catch (_: FileNotFoundException) {
        Toast.makeText(
            context, context.getString(R.string.detail_file_not_found), Toast.LENGTH_LONG
        ).show()
        errFileNameSaver(null)
        errUri
    } catch (e: Exception) {
        e.printStackTrace()
        errFileNameSaver(null)
        errUri
    }
}

@Composable
fun insertPicture(context: Context, uri: Uri, ignoreCompressSettings: Boolean = false): Uri? {
    val coroutineScope = rememberCoroutineScope()
    var rememberUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val preference = PrimaryPreferences(context)
    val compression = if (ignoreCompressSettings) 0 else preference.pictureCompressionRate()
    if (rememberUri == null) {
        ProcessingDialog {
            coroutineScope.launch(Dispatchers.IO) {
                rememberUri = insertPicture(context, uri, compression)
            }
        }
    }
    if (rememberUri == errUri) Toast.makeText(
        context, stringResource(R.string.failed_add_picture), Toast.LENGTH_LONG
    ).show()
    return rememberUri
}

@Composable
private fun ProcessingDialog(run: () -> Unit) {
    AlertDialog(onDismissRequest = {}, confirmButton = {}, text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.padding(8.dp))
            CircularProgressIndicator()
            Text(stringResource(R.string.processing))
        }
    })
    run()
}
