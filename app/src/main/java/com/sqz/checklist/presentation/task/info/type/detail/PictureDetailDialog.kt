package com.sqz.checklist.presentation.task.info.type.detail

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.common.media.openPictureBySystem
import com.sqz.checklist.presentation.task.info.type.InfoDialogScaffold
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.ProcessingDialog
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.info.TaskInfo
import kotlin.math.max

/**
 * This method expected to be called only within this package and its sub-packages.
 *
 * @throws ClassCastException if the detail type is not [TaskInfo.DetailInfoState.DetailType.Picture].
 */
@Composable
internal fun PictureDetailDialog(
    detail: TaskInfo.DetailInfoState,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    view: android.view.View = LocalView.current
) {
    val pictureType = detail.detailType as TaskInfo.DetailInfoState.DetailType.Picture

    val pictureToUri = Uri.fromFile((pictureType.path.toPath()).toFile())
    val onOpenExternal = rememberSaveable { mutableStateOf(false) }
    InfoDialogScaffold(
        onDismissRequest = onDismissRequest,
        isSmallScreenSize = isSmallScreenSize,
        modifier = modifier
    ) {
        InfoDetailDialogTitle(
            detailTitle = stringResource(R.string.picture),
            detailDescription = detail.detailDescription
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 18.dp))
        PictureCard(
            pictureUri = pictureToUri,
            pictureName = pictureType.fileName,
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
            openPictureBySystem(
                pictureName = pictureType.fileName,
                picturePath = pictureType.path,
                context = view.context,
            )
            onOpenExternal.value = false
        }
    }
}

@Composable
private fun PictureCard(
    pictureUri: Uri,
    pictureName: String,
    modifier: Modifier = Modifier
) {
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
                painter = rememberAsyncImagePainter(pictureUri),
                contentDescription = pictureName
            )
        }
    }
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
private fun PictureDetailDialogPreview() {
    PictureDetailDialog(
        detail = TaskInfo.DetailInfoState(
            detailDescription = "test",
            detailType = TaskInfo.DetailInfoState.DetailType.Picture(
                fileName = "test.jpg",
                path = "",
            )
        ),
        onDismissRequest = {},
        isSmallScreenSize = false,
        feedback = AndroidEffectFeedback(LocalView.current)
    )
}
