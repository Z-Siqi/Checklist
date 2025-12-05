package com.sqz.checklist.ui.main.task.layout.dialog

import android.net.Uri
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.TaskDetailData
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.ui.common.ApplicationList
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.DialogWithMenu
import com.sqz.checklist.ui.common.dialog.DialogWithMenuView
import com.sqz.checklist.ui.common.media.AudioSelector
import com.sqz.checklist.ui.common.media.PictureSelector
import com.sqz.checklist.ui.common.media.VideoSelector
import com.sqz.checklist.ui.common.media.insertAudio
import com.sqz.checklist.ui.common.media.insertPicture
import com.sqz.checklist.ui.common.media.insertVideo
import com.sqz.checklist.ui.common.media.toUri
import com.sqz.checklist.ui.common.rememberApplicationList
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import java.io.File

@Composable
fun TaskDetailDialog(
    onDismissRequest: () -> Unit,
    confirm: (taskDetails: List<TaskDetailData>?, isChanged: Boolean) -> Unit,
    title: String,
    taskDetail: List<TaskDetailData>?,
    view: View = LocalView.current
) {
    val viewModel: TaskDetailDialogViewModel = viewModel()
    val init = rememberSaveable { mutableStateOf(false).also { viewModel.onCleared() } }
    if (!init.value) LaunchedEffect(Unit) {
        if (taskDetail == null) {
            viewModel.init()
        } else {
            viewModel.init(taskDetail)
        }
        init.value = true
    }
    val inAsList: Boolean = taskDetail != null && taskDetail.size > 1
    val showListDialog = rememberSaveable { mutableStateOf(inAsList) }
    val isChanged by viewModel.isChanged.collectAsState()
    val isListed: Boolean = viewModel.getTaskDetailList()?.collectAsState()?.value.let {
        it != null && it.isNotEmpty()
    }
    if (showListDialog.value) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isChanged.value) onDismissRequest() },
            dismissButton = {
                if (isListed) TextButton(onClick = {
                    onDismissRequest()
                    viewModel.onCleared()
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }, confirmButton = {
                TextButton(enabled = isChanged, onClick = {
                    if (isListed) {
                        viewModel.getFinalTaskDetail()?.let { confirm(it, isChanged) }
                            ?: onDismissRequest()
                        viewModel.onCleared()
                    } else {
                        viewModel.setTaskDetail { viewModel.getTaskDetailList()!!.value.last() }
                        showListDialog.value = false
                    }
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    val buttonText = if (isListed) stringResource(R.string.confirm) else {
                        stringResource(R.string.back)
                    }
                    Text(text = buttonText)
                }
            }, title = { Text(title) }, text = {
                Column {
                    Card(
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                        shape = ShapeDefaults.Medium
                    ) {
                        TaskDetailList(showListDialog = showListDialog, viewModel = viewModel)
                    }
                    Spacer(modifier = Modifier.weight(1f, false))
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = {
                            showListDialog.value = false
                        }, enabled = viewModel.allowAddToList().collectAsState().value) {
                            Icon(
                                painter = painterResource(R.drawable.add_list),
                                contentDescription = stringResource(R.string.add)
                            )
                        }
                    }
                }
            }
        )
    } else {
        val taskDetailOnProcess by viewModel.taskDetail.collectAsState()
        val onMode = rememberSaveable { mutableStateOf(OnMode.Null) }
        when (onMode.value) {
            OnMode.List -> {
                if (taskDetailOnProcess == null) {
                    Log.d("TaskDetailDialog", "List")
                    showListDialog.value = true
                    onMode.value = OnMode.Null
                } else if (mediaProcesser(viewModel, view)) {
                    viewModel.addTaskDetailToList()
                    showListDialog.value = true
                    onMode.value = OnMode.Null
                }
            }

            OnMode.Confirm -> {
                if (taskDetailOnProcess == null) { // on delete
                    if (!isListed) {
                        confirm(viewModel.getFinalTaskDetail(), isChanged)
                        viewModel.onCleared()
                    } else {
                        showListDialog.value = true
                    }
                    onMode.value = OnMode.Null
                } else if (mediaProcesser(viewModel, view)) {
                    viewModel.addTaskDetailToList()
                    if (!isListed) {
                        confirm(viewModel.getFinalTaskDetail(), isChanged)
                        viewModel.onCleared()
                    } else {
                        showListDialog.value = true
                    }
                    onMode.value = OnMode.Null
                }
            }

            else -> {}
        }
        val isNotEmptyCallIn = !taskDetail.isNullOrEmpty() && taskDetailOnProcess != null
        val isNew = isListed || taskDetail.isNullOrEmpty()
        if (isNotEmptyCallIn || isNew) TaskDetailDialog(
            onDismissRequest = { onDismissRequest().also { viewModel.onCleared() } },
            confirm = { onMode.value = OnMode.Confirm },
            onDismissClick = { if (isListed) onMode.value = OnMode.List else onDismissRequest() },
            listedConfirm = { onMode.value = OnMode.List },
            title = title,
            taskDetailIn = taskDetail,
            viewModel = viewModel
        )
    }
}

private enum class OnMode {
    Null, Confirm, List
}

/** The list of task details **/
@Composable
private fun TaskDetailList( //TODO: implemented it
    showListDialog: MutableState<Boolean>,
    viewModel: TaskDetailDialogViewModel
) {
    val detailList = viewModel.getTaskDetailList()?.collectAsState()
    val rememberDetailList = remember { mutableStateOf(detailList?.value) }
    fun onClick(index: Int) {
        viewModel.setTaskDetailFromList(index)
        showListDialog.value = false
    }

    @Composable
    fun ListView(index: Int, data: TaskDetailData) = Card(modifier = Modifier.padding(4.dp)) {
        val typeString = (data.type as Any?).toString()
        val textFieldState = rememberTextFieldState(initialText = data.description ?: typeString)
        TextField(
            state = textFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            inputTransformation = {
                if (this.toString() != typeString) {
                    if (this.toString() == "") {
                        viewModel.updateTaskDetailDescription(index, null)
                    } else {
                        viewModel.updateTaskDetailDescription(index, this.toString())
                    }
                } else if (data.description != null && this.toString() == typeString) {
                    viewModel.updateTaskDetailDescription(index, null)
                }
            },
            placeholder = { Text(typeString) },
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium)
        )
        Text(typeString)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = { onClick(index) }) {
                Icon(painterResource(R.drawable.edit), null)
            }
            Spacer(modifier = Modifier.widthIn(max = 8.dp) then Modifier.fillMaxWidth())
            Button(onClick = { viewModel.removeFromTaskDetailList(index) }) {
                Icon(painterResource(R.drawable.delete), null)
            }
            Spacer(modifier = Modifier.widthIn(max = 4.dp) then Modifier.fillMaxWidth())
        }
    }

    if (detailList != null) LazyColumn {
        item {
            Text("This feature is not implemented yet.")
            Text("If you see this, please report to developer as soon as possible!")
        }

        itemsIndexed(detailList.value) { index, data ->
            ListView(index, data)
        }
    }
}

/**
 * Write media file to storage and save unconfirmed file path to prefs.
 * @return true if process is finished.
 */
@Composable
private fun mediaProcesser(viewModel: TaskDetailDialogViewModel, view: View): Boolean {
    val onReturn = rememberSaveable { mutableStateOf(false) }
    val taskDetail: TaskDetailData? = viewModel.taskDetail.collectAsState().value
    if (taskDetail == null || !viewModel.isChanged.collectAsState().value) return true
    try {
        taskDetail.dataByte as Uri
    } catch (_: ClassCastException) {
        return true
    }
    fun isExists(uri: Uri): Boolean {
        return uri.path?.toByteArray()?.toUri(MainActivity.appDir)!!.path?.let {
            File(it).exists()
        } == true
    }
    when (taskDetail.type) {
        TaskDetailType.Picture -> {
            val insertPicture = insertPicture(
                view.context, taskDetail.dataByte, isExists(taskDetail.dataByte)
            )
            Log.d("TaskDetailDialog", "insertPicture: $insertPicture")
            insertPicture?.let {
                viewModel.onMediaSave(
                    uri = insertPicture,
                    prefsCache = PreferencesInCache(view.context)
                )
                onReturn.value = true
            }
        }

        TaskDetailType.Video -> {
            val insertVideo = insertVideo(
                view.context, taskDetail.dataByte, isExists(taskDetail.dataByte)
            )
            insertVideo?.let {
                viewModel.onMediaSave(
                    uri = insertVideo,
                    prefsCache = PreferencesInCache(view.context)
                )
                onReturn.value = true
            }
        }

        TaskDetailType.Audio -> {
            val insertAudio = insertAudio(view.context, taskDetail.dataByte)
            insertAudio?.let {
                viewModel.onMediaSave(
                    uri = insertAudio,
                    prefsCache = PreferencesInCache(view.context)
                )
                onReturn.value = true
            }
        }

        else -> return true
    }
    return onReturn.value
}

/**
 * Show dialog with task detail.
 */
@Composable
private fun TaskDetailDialog(
    onDismissRequest: () -> Unit,
    confirm: () -> Unit,
    onDismissClick: () -> Unit,
    listedConfirm: () -> Unit,
    title: String,
    taskDetailIn: List<TaskDetailData>?,
    viewModel: TaskDetailDialogViewModel,
    view: View = LocalView.current
) {
    val noDoNothing = stringResource(R.string.no_do_nothing)
    val notURL = stringResource(R.string.invalid_url)
    val taskDetail = viewModel.taskDetail.collectAsState().value
    val textIn = try {
        (taskDetail?.dataByte as ByteArray?)?.toString(Charsets.UTF_8) ?: ""
    } catch (_: ClassCastException) {
        ""
    }
    val rememberTextIn by rememberSaveable { mutableStateOf(textIn) }
    val textFieldState = rememberTextFieldState(initialText = textIn)
    if (rememberTextIn != textFieldState.text.toString()) LaunchedEffect(Unit) {
        viewModel.setTaskDetail {
            it?.copy(dataByte = textFieldState.text.toString().toByteArray(Charsets.UTF_8))
        }
    }
    val applicationListSaver = rememberApplicationList(view.context)
    fun onTaskDetailConfirm(getType: Any?, callback: () -> Unit) = viewModel.onTaskDetailConfirm(
        getType = getType, textFieldState = textFieldState, onIssue = { issueType ->
            when (issueType) {
                TaskDetailType.URL -> {
                    Toast.makeText(view.context, notURL, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Toast.makeText(view.context, noDoNothing, Toast.LENGTH_SHORT).show()
                }
            }
        }
    ) { callback() }

    val functionalContent: @Composable ((Any?) -> Unit) = {
        val mediaUriGetter: Uri? = try {
            (taskDetail?.dataByte as ByteArray).toUri()
        } catch (_: Exception) {
            null
        }
        when (it as TaskDetailType?) {
            TaskDetailType.Application -> ApplicationList(
                packageName = { name ->
                    textFieldState.clearText()
                    textFieldState.edit { insert(0, name) }
                }, saver = applicationListSaver.apply {
                    taskDetail?.dataByte?.let { dataByte ->
                        val str = (dataByte as ByteArray).toString(Charsets.UTF_8)
                        if (selectedAppInfo.value == null) setter(str, view.context)
                    }
                }, view.context
            )

            TaskDetailType.Picture -> {
                if (viewModel.functionalSaver == null) viewModel.functionalSaver =
                    mediaUriGetter?.let { uri ->
                        PictureSelector(uriIn = uri, nameIn = taskDetail!!.dataString)
                    } ?: PictureSelector()
                val handler = viewModel.functionalSaver as PictureSelector
                viewModel.isChanged(handler.dataUri.collectAsState().value != mediaUriGetter)
                PictureSelector(handler, view)
            }

            TaskDetailType.Video -> {
                if (viewModel.functionalSaver == null) viewModel.functionalSaver =
                    mediaUriGetter?.let { uri ->
                        VideoSelector(uriIn = uri, nameIn = taskDetail!!.dataString)
                    } ?: VideoSelector()
                val handler = viewModel.functionalSaver as VideoSelector
                viewModel.isChanged(handler.dataUri.collectAsState().value != mediaUriGetter)
                VideoSelector(handler, view)
            }

            TaskDetailType.Audio -> {
                if (viewModel.functionalSaver == null) viewModel.functionalSaver =
                    mediaUriGetter?.let { uri ->
                        AudioSelector(uriIn = uri, nameIn = taskDetail!!.dataString)
                    } ?: AudioSelector()
                val handler = viewModel.functionalSaver as AudioSelector
                viewModel.isChanged(handler.dataUri.collectAsState().value != mediaUriGetter)
                AudioSelector(handler, view)
            }

            null -> Text(
                text = stringResource(R.string.select_detail_type),
                modifier = Modifier.padding(7.dp), MaterialTheme.colorScheme.secondary,
                fontSize = 16.sp
            )

            else -> TextField(
                currentType = it, state = textFieldState,
                keyboardType = { type ->
                    when (type) {
                        TaskDetailType.Text -> KeyboardType.Text
                        TaskDetailType.URL -> KeyboardType.Uri
                        else -> KeyboardType.Unspecified
                    }
                },
                lineLimits = if (it == TaskDetailType.URL) {
                    TextFieldLineLimits.SingleLine
                } else TextFieldLineLimits.MultiLine(),
                capitalize = { type -> type == TaskDetailType.Text },
                doneImeAction = { type -> type == TaskDetailType.URL },
            )
        }
    }
    val isListed: Boolean = viewModel.getTaskDetailList()?.collectAsState()?.value.let {
        it != null && it.isNotEmpty()
    }
    DialogWithMenu(
        onDismissRequest = { if (!viewModel.isChanged.value) onDismissRequest() },
        confirm = { onTaskDetailConfirm(it) { confirm() } },
        menuListGetter = TaskDetailType.entries.toTypedArray(),
        menuText = { it.toString() },
        functionalContent = functionalContent,
        defaultType = taskDetail?.type,
        dialogWithMenuView = DialogWithMenuView(
            title = title,
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.dismiss),
            extensionActionButton = {
                if (taskDetail?.dataByte != null && !taskDetailIn.isNullOrEmpty()) TextTooltipBox(R.string.delete) {
                    IconButton(onClick = {
                        viewModel.setTaskDetail { null }
                        confirm()
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            },
            extensionTitleButton = {
                /*IconButton(onClick = {
                    onTaskDetailConfirm(it) { listedConfirm() }
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.add_list),
                        contentDescription = stringResource(R.string.add_detail)
                    )
                }*/
            }
        ),
        currentMenuSelection = { viewModel.onMenuSelectionChanged(it, textFieldState) },
        onDismissClick = onDismissClick,
    )
}

/** Format `Any as TaskDetailType` to string. **/
@Composable
private fun Any?.toString(): String {
    return when (this) {
        TaskDetailType.Text -> stringResource(R.string.text)
        TaskDetailType.URL -> stringResource(R.string.url)
        TaskDetailType.Application -> stringResource(R.string.application)
        TaskDetailType.Picture -> stringResource(R.string.picture)
        TaskDetailType.Video -> stringResource(R.string.video)
        TaskDetailType.Audio -> stringResource(R.string.audio)
        else -> stringResource(R.string.click_select_detail_type)
    }
}

/** Process input text. **/
@Composable
private fun TextField(
    currentType: Any?,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    doneImeAction: (type: Any?) -> Boolean = { false },
    capitalize: (type: Any?) -> Boolean = { false },
    keyboardType: (type: Any?) -> KeyboardType = { KeyboardType.Unspecified },
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.MultiLine(),
) = Column(modifier = modifier) {
    val focus = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val rememberType = rememberSaveable { mutableStateOf(currentType) }
    if (currentType == rememberType.value) BasicTextField(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalColumnScrollbar(
                scrollState = scrollState, endPadding = 0f, scrollBarCornerRadius = 12f,
                scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
                scrollBarColor = MaterialTheme.colorScheme.outline,
                showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
            ),
        state = state,
        textStyle = TextStyle(
            fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
        keyboardOptions = KeyboardOptions(
            capitalization = if (capitalize(currentType)) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            imeAction = if (doneImeAction(currentType)) {
                ImeAction.Done
            } else ImeAction.Default,
            keyboardType = keyboardType(currentType)
        ),
        onKeyboardAction = { if (doneImeAction(currentType)) focus.clearFocus() },
        inputTransformation = InputTransformation {
            if (lineLimits == TextFieldLineLimits.SingleLine && this.toString().contains("\n")) {
                this.replace(0, this.length, this.toString().replace("\n", ""))
            }
        },
        lineLimits = lineLimits.let {
            if (it == TextFieldLineLimits.SingleLine) TextFieldLineLimits.MultiLine() else it
        },
        scrollState = scrollState
    ) else {
        rememberType.value = currentType
    }
}
