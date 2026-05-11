package com.sqz.checklist.presentation.task.modify.dialog.detail.set

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.common.fileUriDecoder
import com.sqz.checklist.common.media.getAlbumArt
import com.sqz.checklist.common.media.toMinute
import com.sqz.checklist.common.media.toStringSecond
import com.sqz.checklist.ui.common.unit.pxToDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import sqz.checklist.common.EffectFeedback
import sqz.checklist.data.storage.StorageHelper.isMediaPath
import sqz.checklist.data.storage.StorageHelper.isTempPath
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.data.storage.openUriSource
import sqz.checklist.task.api.TaskModify

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun AudioTypeBoard(
    view: View,
    audioState: TaskModify.Detail.TypeState.Audio,
    onStateChange: (TaskModify.Detail.TypeState.Audio) -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback
) {
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            try {
                uri?.let {
                    view.context.fileUriDecoder(uri)?.let { data ->
                        if (data.sizeToMB() > 55) {
                            throw ArrayIndexOutOfBoundsException()
                        }
                        val storageManager = StorageManager.provider()
                        coroutineScope.launch {
                            isLoading = true
                            val toTemp = storageManager.copyFileToTemp(
                                inputSource = {
                                    view.context.contentResolver.openUriSource(data.uri)
                                },
                                originalFileName = data.name
                            )
                            val change = audioState.copy(
                                fileName = toTemp.second, path = toTemp.first
                            )
                            onStateChange(change)
                            isLoading = false
                        }
                    }
                }
            } catch (_: ArrayIndexOutOfBoundsException) {
                Toast.makeText(
                    view.context, R.string.audio_size_limit, Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("AudioTypeBoard", "Failed to select a audio: $e")
                Toast.makeText(
                    view.context, view.context.getString(R.string.report_normal_file_size, "55"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 38.dp),
            shape = RoundedCornerShape(
                topStart = 15.dp, topEnd = 15.dp, bottomStart = 4.dp, bottomEnd = 4.dp
            )
        ) {
            if (audioState.path.isBlank()) {
                NonAudioText(isLoading)
            } else {
                AudioPreviewPlayer(
                    audioState = audioState,
                    context = view.context,
                    isSmallScreenSize = isSmallScreenSize,
                    feedback = feedback
                )
            }
        }
        OutlinedButton(
            onClick = {
                feedback.onClickEffect()
                launcher.launch("audio/*")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                topStart = 3.dp, topEnd = 3.dp, bottomStart = 15.dp, bottomEnd = 15.dp
            ),
            enabled = !isLoading
        ) {
            val buttonText = audioState.let {
                if (it.path.isBlank()) {
                    stringResource(R.string.select_audio)
                } else {
                    stringResource(R.string.select_new_audio)
                }
            }
            Text(text = buttonText)
        }
    }
}

@Composable
private fun NonAudioText(isLoading: Boolean) = Column(
    modifier = Modifier.heightIn(min = 50.dp),
    verticalArrangement = Arrangement.Center
) {
    val text = isLoading.let {
        if (it) stringResource(R.string.loading)
        else stringResource(R.string.no_audio_selected)
    }
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AudioPreviewPlayer(
    audioState: TaskModify.Detail.TypeState.Audio,
    context: Context,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
) {
    audioState.let {
        if (it.path.isBlank() || !it.path.isTempPath() && !it.path.isMediaPath()) {
            val errMessage = "Audio path is unexpected to preview in task detail selecting!"
            throw RuntimeException(errMessage + "\n" + it.path)
        }
    }

    val windowSize = LocalWindowInfo.current.containerSize
    val uri = Uri.fromFile((audioState.path.toPath()).toFile())

    @Composable
    fun AlbumArtImage() {
        var albumArt by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(uri) {
            albumArt = context.getAlbumArt(uri)
        }
        val contentDescription = stringResource(R.string.album_art)
        val size = windowSize.width.pxToDp().let {
            val goldenP = it / 1.618f
            val width = it - goldenP
            if (width > 168.dp) return@let 168.dp
            if (width < 38.dp) 38.dp else width
        }
        Card(
            modifier = Modifier
                .sizeIn(maxWidth = size, maxHeight = size)
                .padding(4.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            if (albumArt != null) Image(
                bitmap = albumArt!!.asImageBitmap(),
                contentDescription = contentDescription, modifier = Modifier.fillMaxSize()
            ) else Image(
                painterResource(R.drawable.music_note), modifier = Modifier.fillMaxSize(),
                contentDescription = contentDescription
            )
        }
    }

    @Composable
    fun AudioPlayer() = Column {
        val exoPlayer = remember(uri) {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(uri)
                setMediaItem(mediaItem)
                prepare()
            }
        }
        var isPlaying by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var rememberUri by remember { mutableStateOf(uri) }
        if (uri != rememberUri) LaunchedEffect(Unit) {
            rememberUri = uri
            currentPosition = 0
            duration = 0
            isPlaying = false
        }
        LaunchedEffect(isPlaying) { // update current playing progress
            while (isPlaying) {
                currentPosition = exoPlayer.currentPosition
                delay(500L)
            }
        }
        DisposableEffect(exoPlayer) { // update total progress
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) duration = exoPlayer.duration
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
        FlowRow {
            val curText = "${currentPosition.toMinute()}:${currentPosition.toStringSecond()}"
            val durationText = "${duration.toMinute()}:${duration.toStringSecond()}"
            Text("$curText / $durationText")
            Spacer(modifier = Modifier.size(16.dp))
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { value ->
                    currentPosition = value.toLong()
                    exoPlayer.seekTo(currentPosition)
                }, valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        val buttonText = exoPlayer.isPlaying.let {
            if (it) stringResource(R.string.pause) else stringResource(R.string.play)
        }
        Button(onClick = {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause().also { isPlaying = false }
            } else {
                exoPlayer.play().also { isPlaying = true }
            }
            feedback.onTapEffect()
        }) {
            Text(
                text = buttonText,
                maxLines = 1,
                style = LocalTextStyle.current,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 5.sp,
                    maxFontSize = LocalTextStyle.current.fontSize
                )
            )
        }
        val isWindowFocused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(isWindowFocused) {
            if (!isWindowFocused) exoPlayer.pause()
        }
    }

    @Composable
    fun AudioTitle() {
        val audioName = audioState.fileName.let {
            it.ifBlank { stringResource(R.string.audio) }
        }
        val audioTextStyle = MaterialTheme.typography.titleSmall.copy(
            lineHeight = TextUnit.Unspecified
        )
        Text(
            text = audioName,
            maxLines = 2,
            style = audioTextStyle,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 5.sp,
                maxFontSize = audioTextStyle.fontSize
            )
        )
    }

    Column(
        modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.Center
    ) {
        if (isSmallScreenSize) {
            AlbumArtImage()
            AudioTitle()
            Spacer(modifier = Modifier.size(4.dp))
            AudioPlayer()
        } else Row {
            AlbumArtImage()
            Spacer(modifier = Modifier.size(8.dp))
            Column {
                AudioTitle()
                HorizontalDivider()
                AudioPlayer()
            }
        }
    }
}

@Preview
@Composable
private fun AudioTypeBoardPreview() {
    val v = LocalView.current
    Surface {
        AudioTypeBoard(
            view = v,
            audioState = TaskModify.Detail.TypeState.Audio(),
            onStateChange = {},
            isSmallScreenSize = false,
            feedback = AndroidEffectFeedback(v)
        )
    }
}
