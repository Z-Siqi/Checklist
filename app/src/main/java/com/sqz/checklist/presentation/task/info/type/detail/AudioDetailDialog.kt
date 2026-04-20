package com.sqz.checklist.presentation.task.info.type.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Mode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.common.media.openAudioBySystem
import com.sqz.checklist.common.media.toMinute
import com.sqz.checklist.common.media.toStringSecond
import com.sqz.checklist.presentation.task.info.type.InfoDialogScaffold
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.ProcessingDialog
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import com.sqz.checklist.ui.theme.extraSmallInSmallestEdgeSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.info.TaskInfo

/**
 * This method expected to be called only within this package and its sub-packages.
 *
 * @throws ClassCastException if the detail type is not [TaskInfo.DetailInfoState.DetailType.Audio].
 */
@Composable
internal fun AudioDetailDialog(
    detail: TaskInfo.DetailInfoState,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    view: android.view.View = LocalView.current
) {
    val audioType = detail.detailType as TaskInfo.DetailInfoState.DetailType.Audio

    val onOpenExternal = rememberSaveable { mutableStateOf(false) }
    InfoDialogScaffold(
        onDismissRequest = onDismissRequest,
        isSmallScreenSize = isSmallScreenSize,
        modifier = modifier
    ) {
        InfoDetailDialogTitle(
            detailTitle = stringResource(R.string.audio),
            detailDescription = detail.detailDescription
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 18.dp))
        AudioCard {
            AudioPlayer(
                uri = Uri.fromFile((audioType.path.toPath()).toFile()),
                audioName = audioType.fileName,
                context = view.context,
            )
        }
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 20.dp))
        ThisDialogButtons(
            onOpenExternalClick = {
                onOpenExternal.value = true
                feedback.onClickEffect()
            },
            onCancelClick = { onDismissRequest().also { feedback.onClickEffect() } }
        )
    }

    val coroutineScope = rememberCoroutineScope()
    if (onOpenExternal.value) ProcessingDialog {
        coroutineScope.launch {
            openAudioBySystem(
                audioName = audioType.fileName,
                audioPath = audioType.path,
                context = view.context,
            )
            onOpenExternal.value = false
        }
    }
}

@Composable
private fun AudioCard(
    audioContent: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
    val widthDp = LocalWindowInfo.current.containerSize.width.pxToDpInt()
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(mediaDialogContentHeight())
            .verticalColumnScrollbar(
                scrollState = scrollState, endPadding = 24f, topBottomPadding = 50f,
                scrollBarCornerRadius = 12f,
                scrollBarTrackColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                    0.8f
                ), scrollBarColor = MaterialTheme.colorScheme.secondary.copy(0.7f),
                showScrollBar = showScrollBar && widthDp > extraSmallInSmallestEdgeSize
            ),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
            content = audioContent,
        )
    }
}

@Composable
private fun AudioPlayer(
    uri: Uri, audioName: String, context: android.content.Context
) = Row {
    @Composable
    fun AlbumArtCard(modifier: Modifier = Modifier, size: Int = 80) {
        var albumArt by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(uri) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val artBytes = retriever.embeddedPicture
                albumArt = if (artBytes != null) BitmapFactory.decodeByteArray(
                    artBytes, 0, artBytes.size
                ) else null
            } catch (e: Exception) {
                albumArt = null
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
        val contentDescription = stringResource(R.string.album_art)
        Card(modifier.size(size.dp, size.dp)) {
            if (albumArt != null) Image(
                albumArt!!.asImageBitmap(), contentDescription
            ) else Image(
                painterResource(R.drawable.music_note), modifier = Modifier.fillMaxSize(),
                contentDescription = contentDescription
            )
        }
    }
    val containerSize = LocalWindowInfo.current.containerSize
    val isLandScape =
        containerSize.width > containerSize.height && containerSize.width.pxToDpInt() > 400
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
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(500L)
        }
    }
    DisposableEffect(exoPlayer) {
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
    if (isLandScape) AlbumArtCard(Modifier.padding(16.dp))
    Column(modifier = Modifier.padding(16.dp)) {
        if (!isLandScape) AlbumArtCard(Modifier.align(Alignment.CenterHorizontally))
        Slider(value = currentPosition.toFloat(), onValueChange = { value ->
            currentPosition = value.toLong()
            exoPlayer.seekTo(currentPosition)
        }, valueRange = 0f..duration.toFloat(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            val curText = "${currentPosition.toMinute()}:${currentPosition.toStringSecond()}"
            val durationText = "${duration.toMinute()}:${duration.toStringSecond()}"
            Text(text = "$curText / $durationText")
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                } else {
                    exoPlayer.play()
                    isPlaying = true
                }
            }) {
                val buttonText = if (exoPlayer.isPlaying) {
                    stringResource(R.string.pause)
                } else {
                    stringResource(R.string.play)
                }
                Text(
                    text = buttonText,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 5.sp,
                        maxFontSize = LocalTextStyle.current.fontSize
                    )
                )
            }
        }
        Text(
            text = audioName,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 10.dp, bottom = 4.dp),
            autoSize = TextAutoSize.StepBased(maxFontSize = LocalTextStyle.current.fontSize),
            style = TextStyle(
                lineHeightStyle = LineHeightStyle.Default.copy(mode = Mode.Minimum)
            ),
            maxLines = 2
        )
    }
}

@Composable
private fun ThisDialogButtons(onOpenExternalClick: () -> Unit, onCancelClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextTooltipBox(R.string.open_with) {
            IconButton(onClick = onOpenExternalClick) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.open_in_new),
                        contentDescription = stringResource(R.string.open_with),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = onCancelClick,
            modifier = Modifier,
        ) {
            Text(
                text = stringResource(R.string.cancel),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview
@Composable
private fun AudioDetailDialogPreview() {
    AudioDetailDialog(
        detail = TaskInfo.DetailInfoState(
            detailDescription = "Test",
            detailType = TaskInfo.DetailInfoState.DetailType.Audio(
                fileName = "",
                path = ""
            )
        ),
        onDismissRequest = {},
        isSmallScreenSize = false,
        feedback = AndroidEffectFeedback(LocalView.current)
    )
}
