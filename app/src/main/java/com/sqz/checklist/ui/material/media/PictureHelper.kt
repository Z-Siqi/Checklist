package com.sqz.checklist.ui.material.media

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sqz.checklist.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalStateException

@Composable
fun PictureSelector(
    pictureValues: (title: String?, picture: Bitmap?) -> Unit,
    view: View,
    modifier: Modifier = Modifier,
    getByteArray: ByteArray?,
) {
    var picture by remember { mutableStateOf<Bitmap?>(null) }
    var title by rememberSaveable { mutableStateOf<String?>(null) }
    var checkSize by rememberSaveable { mutableStateOf(false) }
    if (picture == null && getByteArray != null) {
        picture = BitmapFactory.decodeByteArray(getByteArray, 0, getByteArray.size)
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            try {
                uri?.let {
                    view.context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            title = cursor.getString(nameIndex)
                        }
                    }
                    val inputStream = view.context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    picture = bitmap
                    checkSize = true
                }
            } catch (e: Exception) {
                picture = null
                title = null
                Log.e("PictureHelper", "Failed to select a picture: $e")
                Toast.makeText(
                    view.context, view.context.getString(R.string.failed_large_file_size),
                    Toast.LENGTH_LONG
                ).show()
                Toast.makeText(
                    view.context, view.context.getString(R.string.report_normal_file_size),
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
        if (picture != null) Image(
            picture!!.asImageBitmap(), stringResource(R.string.selected_picture)
        ) else Text(
            stringResource(R.string.click_select_picture), color = MaterialTheme.colorScheme.outline
        )
    }
    if (checkSize) {
        val size = picture?.toByteArray()?.size ?: 0
        if ((size * 1024 * 1024) > 25) {
            picture = null
            title = null
            Toast.makeText(
                view.context, stringResource(R.string.picture_size_limit), Toast.LENGTH_SHORT
            ).show()
        }
        checkSize = false
    }
    pictureValues(title, picture)
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
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val height = when {
        screenHeightDp >= 700 -> (screenHeightDp / 5.8).toInt()
        screenHeightDp < (LocalConfiguration.current.screenWidthDp / 1.2) -> (screenHeightDp / 3.2).toInt()
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
            val bitmap = byteArrayToBitmap(byteArray)
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
                    if (bitmap == null) Text(
                        stringResource(R.string.loading), color = MaterialTheme.colorScheme.outline
                    ) else Image(bitmap.asImageBitmap(), imageName)
                }
            }
        },
        title = { Text(title) },
    )
}

@Composable
fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    if (bitmap == null) LaunchedEffect(Unit) {
        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    return bitmap
}

fun Bitmap.toByteArray(
    quality: Int = 80, type: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
): ByteArray {
    val outputStream = ByteArrayOutputStream()
    this.compress(type, quality, outputStream)
    val byteArray = outputStream.toByteArray()
    return byteArray
}

fun openImageBySystem(
    imageName: String, byteArray: ByteArray, context: Context
) {
    val name = if (imageName == "") "unknown_name" else imageName
    val file = File(context.cacheDir, name)
    fun uri(file: File): Uri {
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(byteArray)
        fileOutputStream.close()
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
}
