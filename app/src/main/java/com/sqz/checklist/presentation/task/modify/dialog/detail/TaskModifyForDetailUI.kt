package com.sqz.checklist.presentation.task.modify.dialog.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sqz.checklist.R
import com.sqz.checklist.presentation.task.modify.TaskModifyViewModel
import com.sqz.checklist.presentation.task.modify.dialog.detail.group.ModifyDetailListDialogUI
import com.sqz.checklist.presentation.task.modify.dialog.detail.set.ModifyDetailDialogUI
import sqz.checklist.data.preferences.PrimaryPreferences
import sqz.checklist.task.api.TaskModify

@Composable
fun TaskModifyForDetailUI(
    preference: PrimaryPreferences,
    viewModel: TaskModifyViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedItem by viewModel.getDetailSelectedItem().collectAsState()
    val taskModify by viewModel.taskModify.collectAsState()

    if (selectedItem != null) {
        ModifyDetailDialogUI(
            onDismissRequest = { viewModel.onDetailItemDismiss(true) },
            onTypeStateChange = viewModel::onDetailItemDataChanged,
            onSelectChange = viewModel::onDetailItemTypeChange,
            onDismiss = { viewModel.onDetailItemDismiss(false) },
            onRemove = viewModel::onDetailItemRemove,
            onConfirm = { viewModel.onDetailItemConfirm(preference) },
            onListConfirm = viewModel::detailItemToList,
            modifyType = taskModify.taskState!!.type,
            isListConfirm = viewModel.getIsDetailModified().collectAsState().value,
            detailUIState = selectedItem!!,
            modifier = modifier,
        )
    } else {
        ModifyDetailListDialogUI(
            allowAddToList = taskModify.allowAddToList(),
            onDismissRequest = viewModel::switchDialog, //TODO: add checker to make sure user not miss click incorrect area
            onDismiss = viewModel::switchDialog, //TODO: maybe switch back last detail dialog?
            onConfirm = viewModel::switchDialog,
            onAddNewDetail = viewModel::addItem,
            onListItemSelect = viewModel::selectItemInList,
            onDescriptionChanged = viewModel::updateItemDescription,
            onItemMoved = viewModel::moveItemTo,
            onItemRemove = viewModel::removeListItem,
            detailState = taskModify.detailState!!,
            modifier = modifier
        )
    }
}

/** This method expected to be called only within this package and its sub-packages. **/
internal fun getTypeTextRid(detailType: TaskModify.Detail.TypeState?): Int? {
    return when (detailType) {
        is TaskModify.Detail.TypeState.Text -> R.string.text
        is TaskModify.Detail.TypeState.URL -> R.string.url
        is TaskModify.Detail.TypeState.Application -> R.string.application
        is TaskModify.Detail.TypeState.Picture -> R.string.picture
        is TaskModify.Detail.TypeState.Video -> R.string.video
        is TaskModify.Detail.TypeState.Audio -> R.string.audio
        null -> null
    }
}
