package com.sqz.checklist.presentation.task.modify.dialog.detail.set

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.MutableState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.modify.TaskModifyViewModel
import com.sqz.checklist.presentation.task.modify.dialog.detail.DetailModifyDialogScaffold
import com.sqz.checklist.presentation.task.modify.dialog.detail.getTypeTextRid
import com.sqz.checklist.ui.common.AdaptiveTieredFlowLayout
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.TieredFlowAlignment
import com.sqz.checklist.ui.common.unit.isSmallScreenSizeForDialog
import com.sqz.checklist.ui.common.unit.pxToDp
import com.sqz.checklist.ui.common.unit.pxToDpInt
import sqz.checklist.common.EffectFeedback
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.database.model.Platform
import sqz.checklist.data.database.repository.task.TaskRepositoryFake
import sqz.checklist.data.storage.manager.StorageManagerFake
import sqz.checklist.task.api.modify.TaskModify

@Composable
fun ModifyDetailDialogUI(
    onDismissRequest: () -> Unit,
    isModified: Boolean,
    onTypeStateChange: (TaskModify.Detail.TypeState) -> Unit,
    onSelectChange: (TaskDetailType) -> Unit,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onConfirm: () -> Unit,
    onListConfirm: () -> Unit,
    modifyType: TaskModify.Task.ModifyType,
    isListConfirm: Boolean,
    detailUIState: TaskModify.Detail.UIState,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
) {
    val isSmallScreenSize = isSmallScreenSizeForDialog()
    val focus = LocalFocusManager.current
    val clearFocusState = remember { mutableStateOf(false) }
    DetailModifyDialogScaffold(
        onDismissRequest = onDismissRequest,
        onDialogBackgroundClick = {
            focus.clearFocus()
            clearFocusState.value = true
        },
        isSmallScreenSize = isSmallScreenSize,
        isModified = isModified,
        modifier = modifier
    ) {
        ThisDialogTitle(
            itemDescription = detailUIState.itemDescription,
            modifyType = modifyType,
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 5.dp else 10.dp))
        DetailTypeMenu(
            onRemove = onRemove,
            onSelectChange = onSelectChange,
            detailType = detailUIState.typeState,
            feedback = feedback,
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 4.dp else 8.dp))
        TypeInfoModifyBoard(
            view = LocalView.current,
            clearFocusState = clearFocusState,
            onTypeStateChange = onTypeStateChange,
            detailUIState = detailUIState,
            isSmallScreenSize = isSmallScreenSize,
            feedback = feedback,
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 24.dp))
        ThisDialogFuncButton(
            isSmallScreenSize = isSmallScreenSize,
            onDismiss = { onDismiss().also { feedback.onClickEffect() } },
            onConfirm = { onConfirm().also { feedback.onClickEffect() } },
            onListConfirm = { onListConfirm().also { feedback.onClickEffect() } },
            detailUIState = detailUIState,
            isListConfirm = isListConfirm,
        )
    }
}

@Composable
private fun TypeInfoModifyBoard(
    view: android.view.View,
    clearFocusState: MutableState<Boolean>,
    onTypeStateChange: (TaskModify.Detail.TypeState) -> Unit,
    detailUIState: TaskModify.Detail.UIState,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
) = when (detailUIState.typeState) {

    is TaskModify.Detail.TypeState.Text -> TextTypeStateCard(
        clearFocusState = clearFocusState,
        textState = detailUIState.typeState as TaskModify.Detail.TypeState.Text,
        onStateChange = onTypeStateChange,
        isSmallScreenSize = isSmallScreenSize,
    )

    is TaskModify.Detail.TypeState.URL -> URLTypeStateCard(
        clearFocusState = clearFocusState,
        textState = detailUIState.typeState as TaskModify.Detail.TypeState.URL,
        onStateChange = onTypeStateChange,
        isSmallScreenSize = isSmallScreenSize,
    )

    is TaskModify.Detail.TypeState.Application -> AppTypeStateBoard(
        view = view,
        clearFocusState = clearFocusState,
        appState = detailUIState.typeState as TaskModify.Detail.TypeState.Application,
        fromPlatform = Platform.Android, //TODO: imp this
        onStateChange = onTypeStateChange,
        isSmallScreenSize = isSmallScreenSize,
        feedback = feedback,
    )

    is TaskModify.Detail.TypeState.Picture -> PictureTypeCard(
        view = view,
        pictureState = detailUIState.typeState as TaskModify.Detail.TypeState.Picture,
        onStateChange = onTypeStateChange,
        feedback = feedback
    )

    is TaskModify.Detail.TypeState.Video -> VideoTypeBoard(
        view = view,
        videoState = detailUIState.typeState as TaskModify.Detail.TypeState.Video,
        onStateChange = onTypeStateChange,
        feedback = feedback,
    )

    is TaskModify.Detail.TypeState.Audio -> AudioTypeBoard(
        view = LocalView.current,
        audioState = detailUIState.typeState as TaskModify.Detail.TypeState.Audio,
        onStateChange = onTypeStateChange,
        isSmallScreenSize = isSmallScreenSize,
        feedback = feedback,
    )

    else -> EmptyTypeStateCard(isSmallScreenSize = isSmallScreenSize)
}

@Composable
private fun EmptyTypeStateCard(
    isSmallScreenSize: Boolean
) = Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    )
) {
    val heightModifier = Modifier.let {
        if (isSmallScreenSize) it else it.heightIn(min = 56.dp)
    }
    Column(
        modifier = heightModifier.padding(5.dp),
        verticalArrangement = Arrangement.Center
    ) {
        val menuButtonText = stringResource(R.string.click_select_detail_type)
        val emptyText = stringResource(R.string.select_detail_type, menuButtonText)
        val textStyle = MaterialTheme.typography.labelLarge
        Text(
            text = buildAnnotatedString {
                val texts = emptyText.split(menuButtonText)
                if (texts.size == 2) {
                    append(texts.first())
                    withStyle(
                        style = SpanStyle(textDecoration = TextDecoration.Underline)
                    ) { append(menuButtonText) }
                    append(texts.last())
                } else {
                    append(emptyText)
                }
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = textStyle.copy(color = MaterialTheme.colorScheme.outline)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailTypeMenu(
    onRemove: () -> Unit,
    onSelectChange: (TaskDetailType) -> Unit,
    detailType: TaskModify.Detail.TypeState?,
    feedback: EffectFeedback,
) = Row {
    Spacer(modifier = Modifier.weight(1f))
    BoxWithConstraints {
        var menuExpanded by remember { mutableStateOf(false) }
        val mWidth = maxWidth
        Column {
            val density = LocalDensity.current
            var parentWidthDp by remember { mutableStateOf(0.dp) }
            OutlinedButton(
                onClick = {
                    menuExpanded = !menuExpanded
                    feedback.onClickEffect()
                },
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .widthIn(min = mWidth / 1.618f)
                    .onGloballyPositioned { layoutCoordinates ->
                        val widthPx = layoutCoordinates.size.width
                        parentWidthDp = with(density) { widthPx.toDp() }
                    },
                enabled = true,
            ) {
                val typeTextRid: Int =
                    getTypeTextRid(detailType) ?: R.string.click_select_detail_type
                Text(
                    text = stringResource(typeTextRid),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 5.sp, maxFontSize = LocalTextStyle.current.fontSize
                    ),
                    maxLines = 1
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }
            val focusManager = LocalFocusManager.current
            DropdownMenuPopup(
                modifier = Modifier
                    .widthIn(min = parentWidthDp)
                    .clip(ShapeDefaults.Large),
                expanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                    focusManager.clearFocus()
                }
            ) {
                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                    TaskDetailType.entries.fastForEach {
                        val textRid = when (it) {
                            TaskDetailType.Text -> R.string.text
                            TaskDetailType.URL -> R.string.url
                            TaskDetailType.Application -> R.string.application
                            TaskDetailType.Picture -> R.string.picture
                            TaskDetailType.Video -> R.string.video
                            TaskDetailType.Audio -> R.string.audio
                        }
                        DropdownMenuItem(
                            onClick = {
                                onSelectChange(it)
                                focusManager.clearFocus()
                                menuExpanded = false
                                feedback.onClickEffect()
                            },
                            text = { Text(text = stringResource(textRid)) },
                        )
                    }
                }
            }
        }
    }
    TextTooltipBox(textRid = R.string.delete) {
        FilledTonalIconButton(onClick = { onRemove().also { feedback.onClickEffect() } }) {
            Icon(
                painter = painterResource(R.drawable.delete),
                contentDescription = stringResource(R.string.delete)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThisDialogTitle(
    itemDescription: String?,
    modifyType: TaskModify.Task.ModifyType,
) = AdaptiveTieredFlowLayout(
    mergeWhenPossible = true,
    sectionGap = 3.dp,
    topContent = {
        val windowSize = LocalWindowInfo.current.containerSize
        val title = when (modifyType) {
            is TaskModify.Task.ModifyType.NewTask -> stringResource(R.string.create_task_detail)
            is TaskModify.Task.ModifyType.EditTask -> stringResource(R.string.edit_task_detail)
            else -> "?"
        }
        val titleStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .requiredWidthIn(max = (windowSize.width.pxToDpInt() / 2.1).dp)
                .requiredHeightIn(max = (windowSize.height.pxToDp() / 5)),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = titleStyle.fontSize)
        )
        if (!itemDescription.isNullOrEmpty()) {
            Spacer(Modifier.width(4.dp))
        }
    },
    bottomContent = {
        if (!itemDescription.isNullOrEmpty()) {
            Text(
                text = itemDescription,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThisDialogFuncButton(
    isSmallScreenSize: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onListConfirm: () -> Unit,
    isListConfirm: Boolean,
    detailUIState: TaskModify.Detail.UIState,
) = AdaptiveTieredFlowLayout(
    modifier = Modifier.fillMaxWidth(),
    mergeWhenPossible = true,
    bottomAlignment = TieredFlowAlignment.End,
    topContent = {
        val listTextRid =
            if (isListConfirm) R.string.confirm_enter_list else R.string.list_management
        TextTooltipBox(textRid = listTextRid) {
            FilledTonalButton(
                onClick = onListConfirm,
                enabled = detailUIState.isValidTypeState()
            ) {
                val painterRid = if (isListConfirm) R.drawable.list_confirm else R.drawable.list
                Icon(
                    painter = painterResource(painterRid),
                    contentDescription = stringResource(R.string.list)
                )
                if (!isSmallScreenSize) {
                    Spacer(modifier = Modifier.width(5.dp))
                    val screenW = LocalWindowInfo.current.containerSize.width.pxToDpInt()
                    val listButtonTextRid = if (screenW > 500) {
                        if (isListConfirm) R.string.confirm_enter_list else R.string.list_management
                    } else R.string.list
                    Text(text = stringResource(listButtonTextRid))
                }
            }
        }
    },
    bottomContent = {
        TextButton(onClick = onDismiss) {
            Text(
                text = stringResource(R.string.dismiss),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
            )
        }
        TextButton(
            onClick = onConfirm,
            enabled = detailUIState.isValidTypeState()
        ) {
            Text(
                text = stringResource(R.string.confirm),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
)

@Preview
@Composable
private fun ModifyDetailDialogUIPreview() {
    val vmFake = viewModel { TaskModifyViewModel(TaskRepositoryFake(), StorageManagerFake()) }
    val v = LocalView.current
    vmFake.newTask(v)
    try {
        vmFake.addItem()
    } catch (_: IllegalArgumentException) {
    }
    vmFake.updateItemDescription(0, "Test title")
    val state = vmFake.taskModify.collectAsState().value
    ModifyDetailDialogUI(
        onDismissRequest = {},
        isModified = false,
        onTypeStateChange = {},
        onSelectChange = vmFake::onDetailItemTypeChange,
        onDismiss = {}, onRemove = {}, onConfirm = {}, onListConfirm = {},
        modifyType = TaskModify.Task.ModifyType.NewTask(),
        isListConfirm = false,
        detailUIState = state.detailState!!.first(),
        feedback = AndroidEffectFeedback(v)
    )
}
