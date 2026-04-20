package com.sqz.checklist.presentation.task.info.type.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.ui.common.AdaptiveTieredFlowLayout
import com.sqz.checklist.ui.common.unit.isSmallScreenSizeForDialog
import com.sqz.checklist.ui.common.unit.pxToDp
import com.sqz.checklist.ui.common.unit.pxToDpInt
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.info.TaskInfo

@Composable
fun InfoDetailDialogUI(
    details: List<TaskInfo.DetailInfoState>,
    onDismissRequest: () -> Unit,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val isSmallScreenSize = isSmallScreenSizeForDialog()
    if (details.size == 1) {
        InfoDetailDialog(
            detail = details.first(),
            onDismissRequest = onDismissRequest,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback,
        )
    } else {
        val selectedIndex = rememberSaveable {
            mutableStateOf<Int?>(null)
        }
        if (selectedIndex.value == null) {
            InfoDetailList(
                details = details,
                onSelected = { selectedIndex.value = it },
                onDismissRequest = onDismissRequest,
                isSmallScreenSize = isSmallScreenSize,
                feedback = feedback,
                modifier = modifier,
            )
        } else {
            InfoDetailDialog(
                detail = details[selectedIndex.value!!],
                onDismissRequest = { selectedIndex.value = null },
                isSmallScreenSize = isSmallScreenSize,
                feedback = feedback,
            )
        }
    }
}

@Composable
private fun InfoDetailDialog(
    detail: TaskInfo.DetailInfoState,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val type = detail.detailType
    when (type) {
        is TaskInfo.DetailInfoState.DetailType.Text -> TextDetailDialog(
            detail = detail,
            onDismissRequest = onDismissRequest,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback,
            modifier = modifier,
        )

        is TaskInfo.DetailInfoState.DetailType.URL -> URLDetailDialog(
            detail = detail,
            onDismissRequest = onDismissRequest,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback,
            modifier = modifier,
            view = LocalView.current,
        )

        is TaskInfo.DetailInfoState.DetailType.Application -> AppDetailDialog(
            detail = detail,
            onDismissRequest = onDismissRequest,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback,
            modifier = modifier,
            view = LocalView.current,
        )

        is TaskInfo.DetailInfoState.DetailType.Picture -> PictureDetailDialog(
            detail = detail,
            onDismissRequest = onDismissRequest,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback,
            modifier = modifier,
            view = LocalView.current,
        )

        is TaskInfo.DetailInfoState.DetailType.Video -> VideoDetailDialog(
            detail = detail,
            onDismissRequest = onDismissRequest,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback,
            modifier = modifier,
            view = LocalView.current,
        )

        is TaskInfo.DetailInfoState.DetailType.Audio -> AudioDetailDialog(
            detail = detail,
            onDismissRequest = onDismissRequest,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback,
            modifier = modifier,
            view = LocalView.current,
        )
    }
}

/** This method expected to be called only within this package and its sub-packages. **/
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun InfoDetailDialogTitle(
    detailTitle: String,
    detailDescription: String?,
) = AdaptiveTieredFlowLayout(
    mergeWhenPossible = true,
    sectionGap = 3.dp,
    topContent = {
        val windowSize = LocalWindowInfo.current.containerSize
        val titleStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        Text(
            text = detailTitle,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .requiredWidthIn(max = (windowSize.width.pxToDpInt() / 2.1).dp)
                .requiredHeightIn(max = (windowSize.height.pxToDp() / 5)),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = titleStyle.fontSize)
        )
        if (!detailDescription.isNullOrEmpty()) {
            Spacer(Modifier.width(4.dp))
        }
    },
    bottomContent = {
        if (!detailDescription.isNullOrEmpty()) {
            Text(
                text = detailDescription,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(5.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
)

/** This method expected to be called only within this package and its sub-packages. **/
@ReadOnlyComposable
@Composable
internal fun mediaDialogContentHeight(): Dp {
    val containerSize = LocalWindowInfo.current.containerSize
    val heightPx: Int = containerSize.height
    val widthPx: Int = containerSize.width
    val heightDp = heightPx.pxToDp()
    val widthDp = widthPx.pxToDp()
    return when {
        heightDp >= 700.dp -> (heightPx / 4.168f).pxToDp().let { if (it > widthDp) widthDp else it }
        heightDp < (widthPx / 1.2f).pxToDp() -> (heightPx / 3.2f).pxToDp()
        else -> (heightPx / 5.1f).pxToDp()
    }
}
