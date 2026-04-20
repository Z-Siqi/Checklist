package com.sqz.checklist.presentation.task.info.type.detail

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.common.media.openVideoBySystem
import com.sqz.checklist.presentation.task.info.type.InfoDialogScaffold
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.ProcessingDialog
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.info.TaskInfo

/**
 * This method expected to be called only within this package and its sub-packages.
 *
 * @throws ClassCastException if the detail type is not [TaskInfo.DetailInfoState.DetailType.Video].
 */
@Composable
internal fun VideoDetailDialog(
    detail: TaskInfo.DetailInfoState,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    view: android.view.View = LocalView.current
) {
    val videoType = detail.detailType as TaskInfo.DetailInfoState.DetailType.Video

    val onOpenExternal = rememberSaveable { mutableStateOf(false) }
    InfoDialogScaffold(
        onDismissRequest = onDismissRequest,
        isSmallScreenSize = isSmallScreenSize,
        modifier = modifier
    ) {
        InfoDetailDialogTitle(
            detailTitle = stringResource(R.string.video),
            detailDescription = detail.detailDescription
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 18.dp))
        VideoCard(
            videoUri = Uri.fromFile((videoType.path.toPath()).toFile()),
            context = view.context
        )
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
            openVideoBySystem(
                videoName = videoType.fileName,
                videoPath = videoType.path,
                context = view.context
            )
            onOpenExternal.value = false
        }
    }
}

@Composable
private fun VideoCard(videoUri: Uri, context: android.content.Context) {
    val isUriAccessible: Boolean = remember(videoUri) {
        try {
            context.contentResolver.openInputStream(videoUri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }
    OutlinedCard(
        modifier = Modifier
            .requiredHeightIn(min = 100.dp)
            .height(mediaDialogContentHeight()),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.inverseSurface)
    ) {
        val playerHost = remember {
            MediaPlayerHost(
                mediaUrl = videoUri.toString(),
                isLooping = false,
                initialVideoFitMode = ScreenResize.FIT
            )
        }
        val playerConfig = VideoPlayerConfig(
            isZoomEnabled = false,
            isFullScreenEnabled = false,
            showVideoQualityOptions = false,
            enableFullEdgeToEdge = false,
            enablePIPControl = false,
            fastForwardBackwardIntervalSeconds = 5F,
        )
        if (isUriAccessible) VideoPlayerComposable(
            modifier = Modifier.fillMaxSize(),
            playerHost = playerHost,
            playerConfig = playerConfig
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
private fun VideoDetailDialogPreview() {
    VideoDetailDialog(
        detail = TaskInfo.DetailInfoState(
            detailDescription = "Test Video",
            detailType = TaskInfo.DetailInfoState.DetailType.Video(
                fileName = "Test", path = "",
            )
        ),
        onDismissRequest = {},
        isSmallScreenSize = false,
        feedback = AndroidEffectFeedback(LocalView.current)
    )
}
