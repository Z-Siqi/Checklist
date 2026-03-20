package com.sqz.checklist.ui.common.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.sqz.checklist.R
import com.sqz.checklist.cache.deleteCacheFileByName
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.PrimaryDialog
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import sqz.checklist.data.preferences.PreferencesInCache
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.math.max

/**
 * A dialog that displays a picture from a byte array.
 * The picture can be opened in an external application by clicking on it.
 *
 * @param onDismissRequest Callback to be invoked when the dialog is dismissed.
 * @param byteArray The byte array of the image to be displayed.
 * @param imageName The name of the image, used for display and when opening externally.
 * @param title The title of the dialog.
 * @param modifier The modifier to be applied to the dialog.
 */
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
    val scope = rememberCoroutineScope()
    // zoom image
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val animatableScale = remember { Animatable(scale) }
    val animatableOffset = remember { Animatable(offset, Offset.VectorConverter) }
    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale *= zoomChange
        scale = scale.coerceIn(0.5f, 5f) // Limit scale
        val newOffset = offset + panChange
        offset = newOffset.coerceIn(size, scale) // Coerce offset during gesture
        scope.launch { // Use scope to update animates for smooth visual feedback
            // Snap to the new values without animation
            launch { animatableScale.snapTo(scale) }
            launch { animatableOffset.snapTo(offset) }
        }
    }
    LaunchedEffect(state.isTransformInProgress) {
        if (!state.isTransformInProgress) {
            // When the gesture ends, animate scale back to a minimum of 1f
            if (scale < 1f) {
                scope.launch {
                    launch { animatableScale.animateTo(1f, spring()) }
                    launch { animatableOffset.animateTo(Offset.Zero, spring()) }
                }
                scale = 1f
            }

            // Animate offset back within bounds
            val boundOffset = offset.coerceIn(size, scale)
            scope.launch { animatableOffset.animateTo(boundOffset, spring()) }
            offset = boundOffset
        }
    }
    // dialog
    PrimaryDialog(
        onDismissRequest = onDismissRequest, actionButton = {
            TextTooltipBox(R.string.open_with) {
                IconButton(onClick = {
                    openImageBySystem(imageName, byteArray, view.context)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painterResource(R.drawable.open_in_new),
                            stringResource(R.string.open_with)
                        )
                    }
                }
            }
            Spacer(modifier.weight(1f))
            TextButton(onClick = {
                onDismissRequest()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) { Text(text = stringResource(R.string.cancel)) }
        }, content = {
            OutlinedCard(
                modifier = modifier
                    .requiredHeightIn(min = 100.dp)
                    .fillMaxWidth()
                    .height(mediaDialogContentHeight()),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.inverseSurface)
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .transformable(state = state)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scope.launch {
                                        launch { animatableScale.animateTo(1f, spring()) }
                                        launch { animatableOffset.animateTo(Offset.Zero, spring()) }
                                    }
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            )
                        }
                        .onSizeChanged { size = it },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = animatableScale.value,
                                scaleY = animatableScale.value,
                                translationX = animatableOffset.value.x,
                                translationY = animatableOffset.value.y
                            ),
                        painter = rememberAsyncImagePainter(Uri.fromFile((byteArray.decodeToString().toPath()).toFile())),
                        contentDescription = imageName
                    )
                }
                Log.i("TEST", byteArray.toUri().toString())
            }
        }, title = { Text(title) },
        modifier = Modifier.widthIn(max = mediaDialogWidth()),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}


/** Coerces the offset to keep the scaled image within the view bounds **/
private fun Offset.coerceIn(size: IntSize, scale: Float): Offset {
    val imageWidth = size.width * scale
    val imageHeight = size.height * scale
    val maxTx = max(0f, (imageWidth - size.width) / 2f + (size.width / 2f * (1 - 1 / scale)))
    val maxTy = max(0f, (imageHeight - size.height) / 2f + (size.height / 2f * (1 - 1 / scale)))
    return Offset(
        x = x.coerceIn(-maxTx, maxTx),
        y = y.coerceIn(-maxTy, maxTy)
    )
}

/**
 * Opens an image using an external application via an Intent.
 * It creates a temporary cache file to share the image with other apps.
 *
 * @param imageName The name for the temporary image file.
 * @param byteArray The image data.
 * @param context The application context.
 */
fun openImageBySystem(imageName: String, byteArray: ByteArray, context: Context) {
    val convertUri = Uri.fromFile((byteArray.decodeToString().toPath()).toFile())
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
