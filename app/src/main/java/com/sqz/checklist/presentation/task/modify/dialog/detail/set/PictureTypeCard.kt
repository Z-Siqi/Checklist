package com.sqz.checklist.presentation.task.modify.dialog.detail.set

import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
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
import sqz.checklist.task.api.modify.TaskModify

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun PictureTypeCard(
    view: View,
    pictureState: TaskModify.Detail.TypeState.Picture,
    onStateChange: (TaskModify.Detail.TypeState.Picture) -> Unit,
    feedback: EffectFeedback,
) {
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            try {
                if (uri == null) return@rememberLauncherForActivityResult
                view.context.fileUriDecoder(uri)?.let { data ->
                    if (data.sizeToMB() > 50) {
                        throw ArrayIndexOutOfBoundsException()
                    }
                    val storageManager = StorageManager.provider()
                    coroutineScope.launch {
                        isLoading = true
                        val toTemp = storageManager.copyFileToTemp(
                            inputSource = { view.context.contentResolver.openUriSource(data.uri) },
                            originalFileName = data.name,
                        )
                        val change = pictureState.copy(
                            fileName = toTemp.second, path = toTemp.first
                        )
                        onStateChange(change)
                        isLoading = false
                    }
                }
            } catch (_: ArrayIndexOutOfBoundsException) {
                Toast.makeText(
                    view.context, R.string.picture_size_limit, Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("PictureHelper", "Failed to select a picture: $e")
                Toast.makeText(
                    view.context, view.context.getString(R.string.report_normal_file_size, "50"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    Column(
        modifier = Modifier.animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val onSelectClick = {
            feedback.onClickEffect()
            launcher.launch("image/*")
        }
        if (pictureState.path.isBlank()) {
            EmptySelectCard(
                isLoading = isLoading,
                onSelectClick = onSelectClick,
            )
        } else {
            val windowSize = LocalWindowInfo.current.containerSize
            PicturePreviewCard(
                pictureState = pictureState,
                feedback = feedback,
                windowSize = windowSize,
                onSelectClick = onSelectClick
            )
        }
    }
}

@Composable
private fun PicturePreviewCard(
    pictureState: TaskModify.Detail.TypeState.Picture,
    feedback: EffectFeedback,
    windowSize: IntSize,
    onSelectClick: () -> Unit,
) {
    var hideButton by remember { mutableStateOf(false) }
    val pressModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(onPress = {
            feedback.onPressEffect()
            hideButton = true
            try {
                awaitRelease()
            } finally {
                hideButton = false
                feedback.onTapEffect()
            }
        })
    }
    val cardColor = MaterialTheme.colorScheme.surfaceBright.let {
        val darkColor = MaterialTheme.colorScheme.scrim
        it.copy(0.4f, darkColor.red, darkColor.green, darkColor.blue)
    }
    Card(
        modifier = pressModifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = (windowSize.height / 4).pxToDp().let {
                if (it < 168.dp) 168.dp else it
            }),
        colors = CardDefaults.cardColors(cardColor)
    ) {
        pictureState.let {
            if (it.path.isBlank() || !it.path.isTempPath() && !it.path.isMediaPath()) {
                val errMessage = "Picture path is unexpected to preview in task detail selecting!"
                throw RuntimeException(errMessage + "\n" + it.path)
            }
        }
        val uri = Uri.fromFile((pictureState.path.toPath()).toFile())
        var rememberUri by remember { mutableStateOf<Uri?>(uri) }
        LaunchedEffect(uri) {
            // fix incorrect image size when selected a new picture
            if (uri != rememberUri) {
                rememberUri = null
                delay(17)
                rememberUri = uri
            }
        }
        Box(modifier = Modifier.heightIn(min = 80.dp), contentAlignment = Alignment.Center) {
            rememberUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = stringResource(R.string.picture),
                    modifier = Modifier.fillMaxWidth()
                )
            } ?: Text(
                text = stringResource(R.string.loading),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            SelectNewButton(
                visible = !hideButton,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .padding(start = 4.dp),
                onSelectClick = onSelectClick
            )
        }
    }
}

@Composable
private fun SelectNewButton(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onSelectClick: () -> Unit,
) = AnimatedVisibility(
    visible = visible,
    modifier = modifier,
    enter = expandVertically(
        expandFrom = Alignment.Bottom
    ) + fadeIn(
        initialAlpha = 0.3f
    ),
    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
) {
    FilledTonalButton(onClick = onSelectClick) {
        Text(text = stringResource(R.string.select_new_picture))
    }
}

@Composable
private fun EmptySelectCard(
    isLoading: Boolean,
    onSelectClick: () -> Unit,
) {
    Card(
        modifier = Modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(
            topStart = 15.dp, topEnd = 15.dp, bottomStart = 4.dp, bottomEnd = 4.dp
        )
    ) {
        val text = isLoading.let {
            if (it) stringResource(R.string.loading)
            else stringResource(R.string.no_picture_selected)
        }
        Column(
            modifier = Modifier.heightIn(min = 50.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
    OutlinedButton(
        onClick = onSelectClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 3.dp, topEnd = 3.dp, bottomStart = 15.dp, bottomEnd = 15.dp
        ),
        enabled = !isLoading
    ) { Text(text = stringResource(R.string.select_picture)) }
}

@Preview
@Composable
private fun PictureTypeCardPreview() {
    val v = LocalView.current
    Surface {
        PictureTypeCard(
            view = v,
            pictureState = TaskModify.Detail.TypeState.Picture(),
            onStateChange = {},
            feedback = AndroidEffectFeedback(v)
        )
    }
}
