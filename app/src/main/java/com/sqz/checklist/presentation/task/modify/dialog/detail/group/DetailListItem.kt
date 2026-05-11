package com.sqz.checklist.presentation.task.modify.dialog.detail.group

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.modify.dialog.detail.getTypeTextRid
import com.sqz.checklist.ui.common.TextTooltipBox
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.TaskModify

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun DetailListItem(
    itemContentData: ItemContentData,
    onDescriptionChanged: (index: Int, name: String?) -> Unit,
    onListItemSelect: (index: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    dragMode: Boolean,
    clearFocusState: MutableState<Boolean>,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
) {
    val cardModifier = itemContentData.itemModifier.let { modifier ->
        if (!dragMode) {
            modifier
                .heightIn(min = 50.dp)
                .pointerInput(Unit) {
                    detectTapGestures { clearFocusState.value = true }
                }
        } else modifier.heightIn(min = 32.dp)
    }
    Card(
        modifier = cardModifier.padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (itemContentData.isDragging) {
                MaterialTheme.colorScheme.primaryContainer
            } else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val nullItemDescription: String = itemContentData.item.let {
            val typeTextRid: Int = getTypeTextRid(it.typeState) ?: R.string.detail
            return@let stringResource(typeTextRid)
        }
        val itemDescription: String = itemContentData.item.let {
            it.itemDescription.apply {
                if (!(this.isNullOrEmpty() || this.isBlank())) return@let this
            }
            return@let nullItemDescription
        }
        if (dragMode) {
            DraggableListItem(
                itemDescription = itemDescription,
                isSmallScreenSize = isSmallScreenSize
            )
        } else {
            ModifyListItem(
                nullItemDescription = nullItemDescription,
                onDescriptionChanged = {
                    onDescriptionChanged(itemContentData.index, it)
                },
                onRemove = {
                    onRemove(itemContentData.index)
                    feedback.onClickEffect()
                },
                onModify = {
                    onListItemSelect(itemContentData.index)
                    feedback.onClickEffect()
                },
                clearFocusState = clearFocusState,
                detailItem = itemContentData.item,
            )
        }
    }
}

@Composable
private fun ModifyListItem(
    nullItemDescription: String,
    onDescriptionChanged: (name: String?) -> Unit,
    onRemove: () -> Unit,
    onModify: () -> Unit,
    clearFocusState: MutableState<Boolean>,
    detailItem: TaskModify.Detail.UIState,
) {
    val focus = LocalFocusManager.current
    if (clearFocusState.value) {
        focus.clearFocus()
        clearFocusState.value = false
    }
    val textFieldState = rememberTextFieldState(
        initialText = detailItem.itemDescription ?: nullItemDescription
    )
    LaunchedEffect(Unit) { // Update change (when recomposed)
        if (textFieldState.text.toString() != (detailItem.itemDescription ?: nullItemDescription)) {
            textFieldState.clearText()
            textFieldState.edit {
                insert(0, (detailItem.itemDescription ?: nullItemDescription))
            }
        }
    }
    TextField(
        state = textFieldState,
        modifier = Modifier.fillMaxWidth(),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        onKeyboardAction = { focus.clearFocus() },
        inputTransformation = {
            if (this.toString() != nullItemDescription) {
                if (this.toString().isBlank()) {
                    onDescriptionChanged(null)
                } else {
                    onDescriptionChanged(this.toString())
                }
            } else if (detailItem.itemDescription != null && this.toString() == nullItemDescription) {
                onDescriptionChanged(null)
            }
        },
        placeholder = { Text(nullItemDescription) },
        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextTooltipBox(textRid = R.string.edit) {
            Button(onClick = onModify) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = stringResource(R.string.edit),
                )
            }
        }
        Spacer(modifier = Modifier.widthIn(max = 8.dp) then Modifier.fillMaxWidth())
        TextTooltipBox(textRid = R.string.delete) {
            Button(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
        Spacer(modifier = Modifier.widthIn(max = 4.dp) then Modifier.fillMaxWidth())
    }
}

@Composable
private fun DraggableListItem(
    itemDescription: String,
    isSmallScreenSize: Boolean,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 50.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Spacer(modifier = Modifier.width(if (isSmallScreenSize) 4.dp else 8.dp))
    Icon(
        painter = painterResource(R.drawable.drag),
        contentDescription = stringResource(R.string.sort)
    )
    Spacer(modifier = Modifier.width(if (isSmallScreenSize) 4.dp else 8.dp))
    Text(
        text = itemDescription,
        maxLines = 1,
        overflow = TextOverflow.MiddleEllipsis,
        style = MaterialTheme.typography.titleMedium
    )
}

@Preview
@Composable
private fun DetailListItemPreview() {
    val itemContentData = ItemContentData(
        index = 0,
        isDragging = false,
        item = TaskModify.Detail.UIState(
            itemDescription = null,
            typeState = TaskModify.Detail.TypeState.Text()
        ),
        itemModifier = Modifier
    )
    DetailListItem(
        itemContentData = itemContentData,
        onDescriptionChanged = { _, _ -> },
        onListItemSelect = {},
        onRemove = {},
        dragMode = false,
        clearFocusState = remember { mutableStateOf(false) },
        isSmallScreenSize = false,
        feedback = AndroidEffectFeedback(androidx.compose.ui.platform.LocalView.current)
    )
}
