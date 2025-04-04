package com.sqz.checklist.ui.main.task.layout.function

import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.database.TaskDetail
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.ui.material.ApplicationList
import com.sqz.checklist.ui.material.media.PictureSelector
import com.sqz.checklist.ui.material.dialog.DialogWithMenu
import com.sqz.checklist.ui.material.media.AudioSelector
import com.sqz.checklist.ui.material.media.VideoSelector
import com.sqz.checklist.ui.material.rememberApplicationList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun TaskDetailDialog(
    onDismissRequest: (onDismissClick: Boolean?) -> Unit,
    confirm: (detailType: TaskDetailType, detailString: String, getUri: Uri?) -> Unit,
    title: String,
    detailData: TaskDetailData,
    view: View
) {
    val detailDataUri by detailData.detailUri().collectAsState()
    val detailDataString by detailData.detailString().collectAsState()
    val detailDataType by detailData.detailType().collectAsState()
    val detailTextState = rememberTextFieldState()
    val noDoNothing = stringResource(R.string.no_do_nothing)
    var remember by rememberSaveable { mutableStateOf(false) }
    if (!remember) LaunchedEffect(Unit) {
        detailTextState.clearText()
        detailTextState.edit { insert(0, detailDataString) }
        remember = true
    }
    val applicationListSaver = rememberApplicationList(view.context)
    DialogWithMenu(
        onDismissRequest = onDismissRequest,
        confirm = {
            val available = when (it?.toTaskDetailType()) {
                TaskDetailType.Text -> detailTextState.text.toString() != ""
                TaskDetailType.URL -> detailTextState.text.toString() != ""
                TaskDetailType.Application -> detailDataString != ""
                else -> detailDataString != "" && detailDataUri != null
            }
            if (available) {
                val notURL = view.context.getString(R.string.invalid_url)
                if (it == TaskDetailType.URL &&
                    !Patterns.WEB_URL.matcher(detailTextState.text.toString()).matches()
                ) Toast.makeText(view.context, notURL, Toast.LENGTH_SHORT).show() else {
                    val detailString = when {
                        it == TaskDetailType.Application -> detailDataString
                        it == TaskDetailType.Picture -> detailDataString
                        it == TaskDetailType.Video -> detailDataString
                        it == TaskDetailType.Audio -> detailDataString
                        it == TaskDetailType.URL && !detailTextState.text.toString()
                            .startsWith("http") -> {
                            detailTextState.edit { insert(0, "http://") }
                            detailTextState.text.toString()
                        }

                        else -> detailTextState.text.toString()
                    }
                    confirm(
                        detailData.detailType(it?.toTaskDetailType())!!,
                        detailString,
                        detailDataUri
                    )
                }
            } else Toast.makeText(view.context, noDoNothing, Toast.LENGTH_SHORT).show()
        },
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(if (detailDataType == null) R.string.dismiss else R.string.delete),
        title = title,
        menuListGetter = TaskDetailType.entries.toTypedArray(),
        menuText = {
            when (it) {
                TaskDetailType.Text -> view.context.getString(R.string.text)
                TaskDetailType.URL -> view.context.getString(R.string.url)
                TaskDetailType.Application -> view.context.getString(R.string.application)
                TaskDetailType.Picture -> view.context.getString(R.string.picture)
                TaskDetailType.Video -> view.context.getString(R.string.video)
                TaskDetailType.Audio -> view.context.getString(R.string.audio)
                else -> view.context.getString(R.string.click_select_detail_type)
            }
        },
        functionalType = {
            when (it) {
                TaskDetailType.Text -> false
                TaskDetailType.URL -> false
                TaskDetailType.Application -> ApplicationList({ name ->
                    detailData.detailString(name)
                }, applicationListSaver.apply {
                    if (selectedAppInfo.value == null) setter(detailDataString, view.context)
                }, view.context
                ) == Unit

                TaskDetailType.Picture -> PictureSelector(detailData, view) == Unit
                TaskDetailType.Video -> VideoSelector(detailData, view) == Unit
                TaskDetailType.Audio -> AudioSelector(detailData, view) == Unit

                else -> Text(
                    stringResource(R.string.select_detail_type),
                    Modifier.padding(7.dp), MaterialTheme.colorScheme.secondary, 16.sp
                ) == Unit
            }
        },
        defaultType = detailDataType,
        currentMenuSelection = {
            if (it != null && detailDataType != it) {
                detailData.detailString("")
                detailData.detailUri(null)
                detailData.inPreviewState(false)
                detailTextState.clearText()
            }
        },
        capitalize = { it == TaskDetailType.Text },
        doneImeAction = { it == TaskDetailType.URL },
        keyboardType = {
            when (it) {
                TaskDetailType.Text -> KeyboardType.Text
                TaskDetailType.URL -> KeyboardType.Uri
                TaskDetailType.Application -> KeyboardType.Unspecified
                TaskDetailType.Picture -> KeyboardType.Unspecified
                TaskDetailType.Video -> KeyboardType.Unspecified
                TaskDetailType.Audio -> KeyboardType.Unspecified
                else -> KeyboardType.Unspecified
            }
        },
        state = detailTextState
    )
}

private fun Any.toTaskDetailType(): TaskDetailType {
    return when (this) {
        TaskDetailType.Text -> TaskDetailType.Text
        TaskDetailType.URL -> TaskDetailType.URL
        TaskDetailType.Application -> TaskDetailType.Application
        TaskDetailType.Picture -> TaskDetailType.Picture
        TaskDetailType.Video -> TaskDetailType.Video
        TaskDetailType.Audio -> TaskDetailType.Audio
        else -> throw TypeCastException("Failed to convert as TaskDetailType!")
    }
}

class TaskDetailData private constructor() {
    companion object {
        @Volatile
        private var instance: TaskDetailData? = null
        fun instance(): TaskDetailData = instance ?: synchronized(this) {
            instance ?: TaskDetailData().also { instance = it }
        }
    }

    private var output by mutableStateOf<TaskDetail?>(null)

    private val detailType = MutableStateFlow<TaskDetailType?>(null)
    private val detailString = MutableStateFlow("")
    private val uri = MutableStateFlow<Uri?>(null)
    private val inPreviewState = MutableStateFlow<Boolean?>(null)

    fun inPreviewState(): StateFlow<Boolean?> = this.inPreviewState.asStateFlow()
    fun inPreviewState(setter: Boolean) {
        if (setter) this.inPreviewState.update { true }
        else this.inPreviewState.update { null }
    }

    fun detailType(): StateFlow<TaskDetailType?> = this.detailType.asStateFlow()
    fun detailType(setter: TaskDetailType?): TaskDetailType? {
        this.detailType.update { setter }
        return this.detailType.value
    }

    fun detailUri(): StateFlow<Uri?> = this.uri.asStateFlow()
    fun detailUri(setter: Uri?) {
        this.uri.update { setter }
    }

    fun detailString(): StateFlow<String> = this.detailString.asStateFlow()
    fun detailString(setter: String) {
        this.detailString.update { setter }
    }

    fun setter(detailType: TaskDetailType, detailString: String, detailUri: Uri? = null) {
        this.detailType.update { detailType }
        this.detailString.update { detailString }
        this.uri.update { detailUri }
    }

    private fun output(): TaskDetail? = this.output
    fun output(toByteArray: ByteArray?) {
        this.detailType.value?.let {
            this.output = TaskDetail(
                0, this.detailType.value!!, this.detailString.value, toByteArray
            )
        }
    }

    fun outputAsFinal(): TaskDetail? {
        this.releaseTemporaryMemory()
        return this.output()
    }

    private fun releaseTemporaryMemory() {
        this.detailType.value = null
        this.detailString.value = ""
        this.uri.value = null
        this.inPreviewState.value = null
    }

    fun releaseMemory() {
        this.releaseTemporaryMemory()
        this.output = null
        Log.d("TaskDetailData", "Release memory is called")
    }
}

fun Uri.toByteArray(): ByteArray {
    return this.toString().toByteArray(Charsets.UTF_8)
}

fun ByteArray.toUri(): Uri {
    return Uri.parse(String(this, Charsets.UTF_8))
}

fun ByteArray.toUri(filesDir: String): Uri {
    val regex = Regex("file:///.*/files/")
    return Uri.parse(String(this, Charsets.UTF_8).replace(regex, "file://$filesDir/"))
}

@Preview
@Composable
private fun Preview() {
    TaskDetailDialog(
        onDismissRequest = {}, confirm = { _, _, _ -> }, title = "TEST",
        detailData = TaskDetailData.instance(), view = LocalView.current
    )
}
