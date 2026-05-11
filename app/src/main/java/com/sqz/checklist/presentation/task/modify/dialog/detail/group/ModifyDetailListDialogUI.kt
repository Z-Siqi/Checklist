package com.sqz.checklist.presentation.task.modify.dialog.detail.group

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.modify.dialog.detail.DetailModifyDialogScaffold
import com.sqz.checklist.ui.common.unit.isSmallScreenSizeForDialog
import com.sqz.checklist.ui.common.unit.pxToDp
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.TaskModify.Detail

@Composable
fun ModifyDetailListDialogUI(
    allowAddToList: Boolean,
    isModified: Boolean,
    onDismissRequest: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onAddNewDetail: () -> Unit,
    onListItemSelect: (index: Int) -> Unit,
    onDescriptionChanged: (index: Int, name: String?) -> Unit,
    onItemMoved: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemRemove: (index: Int) -> Unit,
    detailState: List<Detail.UIState>,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val isSmallScreenSize = isSmallScreenSizeForDialog()
    val clearFocusState = remember { mutableStateOf(false) }
    var dragMode by rememberSaveable { mutableStateOf(false) }

    DetailModifyDialogScaffold(
        onDismissRequest = onDismissRequest,
        onDialogBackgroundClick = {},
        isSmallScreenSize = isSmallScreenSize,
        isModified = isModified,
        modifier = modifier,
    ) {
        ThisTitle()
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 18.dp))
        DetailList(
            clearFocusState = clearFocusState,
            dragMode = dragMode,
            onItemMoved = onItemMoved,
            onDescriptionChanged = onDescriptionChanged,
            onItemRemove = onItemRemove,
            detailState = detailState,
            onListItemSelect = onListItemSelect,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 8.dp else 15.dp))
        FuncButtons(
            allowAddToList = allowAddToList,
            onAddClick = { onAddNewDetail().also { feedback.onClickEffect() } },
            onDragClick = {
                dragMode = it
                feedback.onClickEffect()
            },
            dragState = dragMode,
        )
        if (dragMode) {
            Spacer(modifier = Modifier.height(if (isSmallScreenSize) 8.dp else 15.dp))
        }
        ThisDialogFuncButton(
            onDismiss = { onDismiss().also { feedback.onClickEffect() } },
            onConfirm = { onConfirm().also { feedback.onClickEffect() } },
            detailState = detailState,
        )
    }
    LaunchedEffect(detailState.size) {
        if (detailState.isEmpty()) onConfirm()
    }
}


@Composable
private fun DetailList(
    clearFocusState: MutableState<Boolean>,
    dragMode: Boolean,
    onDescriptionChanged: (index: Int, name: String?) -> Unit,
    onListItemSelect: (index: Int) -> Unit,
    onItemMoved: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemRemove: (index: Int) -> Unit,
    detailState: List<Detail.UIState>,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
) {
    val windowSize = LocalWindowInfo.current.containerSize
    val heightLimit = windowSize.height.pxToDp().let {
        if (isSmallScreenSize) it else (it / 3f).let { dvd ->
            if (dvd < 200.dp) 210.dp else dvd
        }
    }
    Card(
        modifier = Modifier.heightIn(max = heightLimit),
        colors = CardDefaults.cardColors(
            MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = ShapeDefaults.Medium
    ) {
        DetailDraggableList(
            items = detailState,
            draggable = dragMode,
            onMove = onItemMoved,
        ) {
            DetailListItem(
                itemContentData = it,
                onDescriptionChanged = onDescriptionChanged,
                onListItemSelect = onListItemSelect,
                onRemove = onItemRemove,
                dragMode = dragMode,
                clearFocusState = clearFocusState,
                isSmallScreenSize = isSmallScreenSize,
                feedback = feedback
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FuncButtons(
    allowAddToList: Boolean,
    onAddClick: () -> Unit,
    onDragClick: (Boolean) -> Unit,
    dragState: Boolean,
) = Row {
    Spacer(modifier = Modifier.weight(0.01f))
    OutlinedToggleButton(
        checked = dragState,
        onCheckedChange = onDragClick,
        modifier = Modifier.weight(1.168f)
    ) {
        Text(text = stringResource(R.string.sort))
    }
    Spacer(modifier = Modifier.weight(0.1f))
    OutlinedButton(
        onClick = onAddClick,
        modifier = Modifier.weight(1.618f),
        enabled = allowAddToList
    ) {
        Text(text = stringResource(R.string.add))
    }
    Spacer(modifier = Modifier.weight(0.01f))
}

@Composable
private fun ThisTitle() {
    val windowSize = LocalWindowInfo.current.containerSize
    val titleStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold, lineHeight = TextUnit.Unspecified
    )
    Text(
        text = stringResource(R.string.detail_list),
        style = titleStyle,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.requiredHeightIn(max = (windowSize.height.pxToDp() / 5)),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = titleStyle.fontSize)
    )
}

@Composable
private fun ThisDialogFuncButton(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    detailState: List<Detail.UIState>,
) = Row {
    Spacer(modifier = Modifier.weight(1f))
    val dismissTextRid = detailState.let {
        if (it.size == 1) R.string.dismiss else R.string.back
    }
    if (detailState.isNotEmpty()) {
        TextButton(onClick = onDismiss) {
            Text(
                text = stringResource(dismissTextRid),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    TextButton(onClick = onConfirm) {
        Text(
            text = stringResource(R.string.confirm),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
private fun ModifyDetailListDialogUIPreview() {
    val detailState = List(5) {
        Detail.UIState(
            itemDescription = "",
            typeState = Detail.TypeState.Text("Text")
        )
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        ModifyDetailListDialogUI(
            allowAddToList = true,
            isModified = false,
            onDismissRequest = {},
            onDismiss = {},
            onConfirm = {},
            onAddNewDetail = {},
            onListItemSelect = {},
            onDescriptionChanged = { _, _ -> },
            onItemMoved = { _, _ -> },
            onItemRemove = {},
            feedback = AndroidEffectFeedback(androidx.compose.ui.platform.LocalView.current),
            detailState = detailState,
        )
    }
}
