package com.sqz.checklist.presentation.task.modify.dialog.detail.set

import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.common.fileUriDecoder
import com.sqz.checklist.ui.common.unit.pxToDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.ArrayIndexOutOfBoundsException
import okio.Path.Companion.toPath
import sqz.checklist.common.EffectFeedback
import sqz.checklist.data.storage.StorageHelper.isMediaPath
import sqz.checklist.data.storage.StorageHelper.isTempPath
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.data.storage.openUriSource
import sqz.checklist.task.api.TaskModify

@Composable
internal fun VideoTypeBoard(
    view: View,
    videoState: TaskModify.Detail.TypeState.Video,
    onStateChange: (TaskModify.Detail.TypeState.Video) -> Unit,
    feedback: EffectFeedback,
) {
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            try {
                if (uri == null) return@rememberLauncherForActivityResult
                view.context.fileUriDecoder(uri)?.let { data ->
                    if (data.sizeToMB() > 350) {
                        throw ArrayIndexOutOfBoundsException()
                    }
                    val storageManager = StorageManager.provider()
                    coroutineScope.launch {
                        isLoading = true
                        val toTemp = storageManager.copyFileToTemp(
                            inputSource = { view.context.contentResolver.openUriSource(data.uri) },
                            originalFileName = data.name,
                        )
                        val change = videoState.copy(
                            fileName = toTemp.second, path = toTemp.first
                        )
                        onStateChange(change)
                        isLoading = false
                    }
                }
            } catch (_: ArrayIndexOutOfBoundsException) {
                Toast.makeText(
                    view.context, R.string.video_size_limit, Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("PictureHelper", "Failed to select a video: $e")
                Toast.makeText(
                    view.context, view.context.getString(R.string.report_normal_file_size, "350"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val cardColor = videoState.path.isBlank().let { isNoVideo ->
            if (isNoVideo) Color.Unspecified else MaterialTheme.colorScheme.surfaceBright.let {
                val darkColor = MaterialTheme.colorScheme.scrim
                it.copy(0.4f, darkColor.red, darkColor.green, darkColor.blue)
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 38.dp),
            colors = CardDefaults.cardColors(cardColor),
            shape = RoundedCornerShape(
                topStart = 15.dp, topEnd = 15.dp, bottomStart = 4.dp, bottomEnd = 4.dp
            )
        ) {
            if (videoState.path.isBlank()) {
                NonVideoText(isLoading = isLoading)
            } else {
                VideoPreviewPlayer(videoState = videoState)
            }
        }
        OutlinedButton(
            onClick = {
                feedback.onClickEffect()
                launcher.launch("video/*")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                topStart = 3.dp, topEnd = 3.dp, bottomStart = 15.dp, bottomEnd = 15.dp
            ),
            enabled = !isLoading
        ) {
            val buttonText = videoState.let {
                if (it.path.isBlank()) {
                    stringResource(R.string.select_video)
                } else {
                    stringResource(R.string.select_new_video)
                }
            }
            Text(text = buttonText)
        }
    }
}

@Composable
private fun NonVideoText(isLoading: Boolean) = Column(
    modifier = Modifier.heightIn(min = 50.dp),
    verticalArrangement = Arrangement.Center
) {
    val text = isLoading.let {
        if (it) stringResource(R.string.loading)
        else stringResource(R.string.no_video_selected)
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
private fun VideoPreviewPlayer(videoState: TaskModify.Detail.TypeState.Video) {
    val windowSize = LocalWindowInfo.current.containerSize
    videoState.let {
        if (it.path.isBlank() || !it.path.isTempPath() && !it.path.isMediaPath()) {
            val errMessage = "Audio path is unexpected to preview in task detail selecting!"
            throw RuntimeException(errMessage + "\n" + it.path)
        }
    }
    val uri = Uri.fromFile((videoState.path.toPath()).toFile())
    var rememberUri by remember { mutableStateOf<Uri?>(uri) }
    LaunchedEffect(uri) {
        // fix incorrect image size when selected a new picture
        if (uri != rememberUri) {
            rememberUri = null
            delay(17)
            rememberUri = uri
        }
    }
    val heightModifier = Modifier.height(
        (windowSize.height / 4.1f).pxToDp().let { if (it < 168.dp) 168.dp else it }
    )
    rememberUri?.let {
        val playerHost = remember {
            MediaPlayerHost(
                mediaUrl = uri.toString(),
                isMuted = true,
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
        VideoPlayerComposable(
            modifier = heightModifier.fillMaxWidth(),
            playerHost = playerHost,
            playerConfig = playerConfig
        )
    } ?: Text(
        text = stringResource(R.string.loading),
        modifier = heightModifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun VideoTypeBoardPreview() {
    val v = LocalView.current
    Surface {
        VideoTypeBoard(
            view = v,
            videoState = TaskModify.Detail.TypeState.Video(),
            onStateChange = {},
            feedback = AndroidEffectFeedback(v)
        )
    }
}
