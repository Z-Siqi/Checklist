package com.sqz.checklist.ui.main.task.layout.function

import android.content.Context
import android.net.Uri
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.TaskDetail
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.ui.material.TextTooltipBox
import com.sqz.checklist.ui.material.dialog.EditableContentDialog
import com.sqz.checklist.ui.material.media.errUri
import com.sqz.checklist.ui.material.media.insertAudio
import com.sqz.checklist.ui.material.media.insertPicture
import com.sqz.checklist.ui.material.media.insertVideo
import kotlinx.coroutines.launch
import java.io.File

/** Add task dialog **/
@Composable
fun TaskModifyDialog(
    reminderButton: Boolean,
    confirm: (CreateTask) -> Unit,
    onDismissRequest: () -> Unit,
    view: View,
) {
    val detailData = TaskDetailData.instance()
    val detailDataType by detailData.detailType().collectAsState()
    var reminder by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf(false) }

    val extraButtonTop: @Composable () -> Unit = {
        val onPinClick = {
            pin = !pin
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
        TextTooltipBox(textRid = R.string.create_as_pin) {
            IconButton(onClick = onPinClick, modifier = Modifier.rotate(40f)) {
                Icon(
                    painter = painterResource(if (pin) R.drawable.pinned else R.drawable.pin),
                    contentDescription = stringResource(R.string.create_as_pin)
                )
            }
        }
    }
    val extraButtonBottom: @Composable (run: () -> Unit) -> Unit = {
        TaskDetailIcon(
            textRid = R.string.create_task_detail,
            onClick = { run(it) },
            selected = detailDataType != null
        )
        if (reminderButton) {
            val onReminderClick = {
                reminder = !reminder
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
            TextTooltipBox(textRid = R.string.create_with_reminder) {
                IconButton(onClick = onReminderClick) {
                    Icon(
                        painter = painterResource(if (reminder) R.drawable.timer_on else R.drawable.timer),
                        contentDescription = stringResource(R.string.create_with_reminder)
                    )
                }
            }
        }
    }
    TaskModifyDialog(
        onDismissRequest = onDismissRequest,
        confirm = {
            confirm(CreateTask(it.text.toString(), pin, reminder, detailData.outputAsFinal()))
        },
        detailData = detailData,
        parameter = TaskModifyDialog(
            title = stringResource(R.string.create_task),
            confirmText = stringResource(if (!reminder) R.string.add else R.string.next),
            detailTitle = stringResource(R.string.create_task_detail),
            extraButtonTop = extraButtonTop,
            extraButtonBottom = extraButtonBottom
        ),
        view = view
    )
}

/** Edit task dialog **/
@Composable
fun TaskModifyDialog(
    editTask: EditTask,
    confirm: (EditTask) -> Unit,
    onDismissRequest: () -> Unit,
    view: View,
) {
    val detailData = TaskDetailData.instance()
    val detailDataType by detailData.detailType().collectAsState()
    var remember by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val textFieldState: (TextFieldState) -> TextFieldState = {
        if (!remember) coroutineScope.launch {
            it.clearText()
            it.edit { insert(0, editTask.description) }
            if (editTask.detail != null) { // when found task detail
                detailData.detailType(editTask.detail.type)
                detailData.detailString(editTask.detail.dataString)
                if (editTask.detail.dataByte != null) {
                    detailData.detailUri(editTask.detail.dataByte.toUri(MainActivity.appDir))
                }
            }
            remember = true
        }
        it
    }
    val extraButtonBottom: @Composable (run: () -> Unit) -> Unit = {
        TaskDetailIcon(
            textRid = R.string.edit_task_detail,
            onClick = { run(it) },
            selected = detailDataType != null
        )
    }
    TaskModifyDialog(
        onDismissRequest = onDismissRequest,
        confirm = {
            confirm(EditTask(editTask.id, it.text.toString(), detailData.outputAsFinal()))
        },
        detailData = TaskDetailData.instance(),
        parameter = TaskModifyDialog(
            title = stringResource(R.string.edit_task),
            confirmText = stringResource(R.string.edit),
            detailTitle = stringResource(R.string.edit_task_detail),
            extraButtonBottom = extraButtonBottom,
            textFieldState = textFieldState
        ),
        view = view
    )
}

/** Add/Edit task dialog handler **/
@Composable
private fun TaskModifyDialog(
    onDismissRequest: () -> Unit,
    confirm: (TextFieldState) -> Unit,
    detailData: TaskDetailData,
    parameter: TaskModifyDialog,
    view: View,
) {
    val detailDataUri by detailData.detailUri().collectAsState()
    val state = parameter.textFieldState(rememberTextFieldState())
    val detailDataType by detailData.detailType().collectAsState()
    var confirmState by rememberSaveable { mutableIntStateOf(0) }
    var detailDialog by rememberSaveable { mutableStateOf(false) }
    EditableContentDialog(
        onDismissRequest = { onDismissRequest().also { detailData.releaseMemory() } },
        confirm = { confirmState = 1 },
        title = parameter.title,
        confirmText = parameter.confirmText,
        state = state,
        contentProperties = EditableContentDialog(
            extraButtonBottom = { parameter.extraButtonBottom { detailDialog = true } },
            extraButtonTop = parameter.extraButtonTop,
        ),
        doneImeAction = true
    )
    if (confirmState != 0) {
        if (state.text.toString() != "") {
            val uriToByteArray = when (detailDataType) {
                TaskDetailType.Picture -> {
                    val insertPicture = insertPicture(
                        view.context, detailDataUri!!, isExists(detailDataUri!!)
                    )
                    val picture = insertPicture?.toByteArray()
                    if (insertPicture != null) confirmState = 2
                    if (insertPicture != errUri) picture else {
                        detailData.detailType(TaskDetailType.Text)
                        null
                    }
                }

                TaskDetailType.Video -> {
                    val insertVideo = insertVideo(view.context, detailDataUri!!)
                    val video = insertVideo?.toByteArray()
                    if (insertVideo != null) confirmState = 2
                    if (insertVideo != errUri) video else {
                        detailData.detailType(TaskDetailType.Text)
                        null
                    }
                }

                TaskDetailType.Audio -> {
                    val insertAudio = insertAudio(view.context, detailDataUri!!)
                    val audio = insertAudio?.toByteArray()
                    if (insertAudio != null)confirmState = 2
                    if (insertAudio != errUri) audio else {
                        detailData.detailType(TaskDetailType.Text)
                        null
                    }
                }

                else -> {
                    confirmState = 2
                    detailDataUri?.toByteArray()
                }
            }
            if (confirmState == 2) {
                detailData.output(uriToByteArray)
                confirm(state)
                onDismissRequest().also { confirmState = 0 }
            }
        } else {
            doNothingToast(view.context)
            confirmState = 0
        }
    }
    if (detailDialog) TaskDetailDialog(
        onDismissRequest = { onDismissClick ->
            if (onDismissClick != null && onDismissClick) detailData.releaseMemory()
            detailDialog = false
        },
        confirm = { type, string, uri ->
            detailData.setter(type, string, uri)
            detailDialog = false
        },
        title = parameter.detailTitle,
        detailData = detailData,
        view = view
    )
}

private fun isExists(uri: Uri): Boolean {
    return uri.path?.toByteArray()?.toUri(MainActivity.appDir)!!.path?.let {
        File(it).exists()
    } == true
}

private data class TaskModifyDialog(
    val title: String,
    val confirmText: String,
    val detailTitle: String,
    val extraButtonTop: @Composable () -> Unit = {},
    val extraButtonBottom: @Composable (run: () -> Unit) -> Unit = {},
    val textFieldState: (TextFieldState) -> TextFieldState = { it },
)

data class CreateTask(
    val description: String,
    val pin: Boolean,
    val reminder: Boolean,
    val detail: TaskDetail?
)

data class EditTask(
    val id: Long,
    val description: String,
    val detail: TaskDetail?
)

private fun doNothingToast(context: Context) {
    Toast.makeText(context, context.getString(R.string.no_do_nothing), Toast.LENGTH_SHORT).show()
}

@Composable
private fun TaskDetailIcon(textRid: Int, onClick: () -> Unit, selected: Boolean) {
    TextTooltipBox(textRid = textRid) {
        IconButton(
            onClick = onClick,
            colors = if (selected) {
                IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            } else IconButtonDefaults.iconButtonColors()
        ) {
            Icon(
                painter = painterResource(R.drawable.attach),
                contentDescription = stringResource(textRid)
            )
        }
    }
}

@Preview
@Composable
private fun TaskModifyDialogPreview() {
    TaskModifyDialog(
        onDismissRequest = {}, confirm = {}, detailData = TaskDetailData.instance(),
        parameter = TaskModifyDialog(
            title = stringResource(R.string.create_task),
            confirmText = stringResource(R.string.add),
            detailTitle = stringResource(R.string.create_task_detail),
        ), view = LocalView.current
    )
}
