package com.sqz.checklist.ui.main.task.layout.function

import android.graphics.Bitmap
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
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.ui.material.ApplicationList
import com.sqz.checklist.ui.material.PictureSelector
import com.sqz.checklist.ui.material.dialog.DialogWithMenu

@Composable
fun TaskDetailDialog(
    onDismissRequest: (onDismissClick: Boolean?) -> Unit,
    confirm: (detailType: TaskDetailType, detailString: String, detailBitmap: Bitmap?) -> Unit,
    title: String,
    detailData: TaskDetailData,
    view: View
) {
    val detailTextState = rememberTextFieldState()
    val noDoNothing = stringResource(R.string.no_do_nothing)
    var remember by rememberSaveable { mutableStateOf(false) }
    if (!remember) LaunchedEffect(Unit) {
        detailTextState.clearText()
        detailTextState.edit { insert(0, detailData.detailString()) }
        remember = true
    }
    DialogWithMenu(
        onDismissRequest = onDismissRequest,
        confirm = {
            if (detailTextState.text.toString() != "" && it != null || detailData.detailString() != "") {
                val notURL = view.context.getString(R.string.invalid_url)
                if (it == TaskDetailType.URL &&
                    !Patterns.WEB_URL.matcher(detailTextState.text.toString()).matches()
                ) Toast.makeText(view.context, notURL, Toast.LENGTH_SHORT).show() else {
                    val detailType = when (it) {
                        TaskDetailType.Text -> TaskDetailType.Text
                        TaskDetailType.URL -> TaskDetailType.URL
                        TaskDetailType.Application -> TaskDetailType.Application
                        TaskDetailType.Picture -> TaskDetailType.Picture
                        else -> null
                    }
                    detailData.detailType(detailType)
                    val detailString = when {
                        it == TaskDetailType.Application -> detailData.detailString()
                        it == TaskDetailType.Picture -> detailData.detailString()
                        it == TaskDetailType.URL && !detailTextState.text.toString()
                            .startsWith("http") -> {
                            detailTextState.edit { insert(0, "http://") }
                            detailTextState.text.toString()
                        }

                        else -> detailTextState.text.toString()
                    }
                    confirm(detailData.detailType()!!, detailString, detailData.detailBitmap())
                }
            } else Toast.makeText(view.context, noDoNothing, Toast.LENGTH_SHORT).show()
        },
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(if (detailData.detailType() == null) R.string.dismiss else R.string.delete),
        title = title,
        menuListGetter = TaskDetailType.entries.toTypedArray(),
        menuText = {
            when (it) {
                TaskDetailType.Text -> view.context.getString(R.string.text)
                TaskDetailType.URL -> view.context.getString(R.string.url)
                TaskDetailType.Application -> view.context.getString(R.string.application)
                TaskDetailType.Picture -> view.context.getString(R.string.picture)
                else -> view.context.getString(R.string.click_select_detail_type)
            }
        },
        functionalType = {
            when (it) {
                TaskDetailType.Text -> false
                TaskDetailType.URL -> false
                TaskDetailType.Application -> ApplicationList({ name ->
                    detailData.detailString(name)
                }, detailData.detailString(), view.context) == Unit

                TaskDetailType.Picture -> PictureSelector({ title, picture ->
                    detailData.detailString(title ?: "")
                    detailData.detailBitmap(picture)
                }, view, getBitmap = detailData.detailBitmap()) == Unit

                else -> Text(
                    stringResource(R.string.select_detail_type),
                    Modifier.padding(7.dp), MaterialTheme.colorScheme.secondary, 16.sp
                ) == Unit
            }
        },
        defaultType = detailData.detailType(),
        currentMenuSelection = {
            if (it != null && detailData.detailType() != it) {
                detailData.detailString("")
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
                else -> KeyboardType.Unspecified
            }
        },
        state = detailTextState
    )
}

class TaskDetailData private constructor() {
    companion object {
        fun instance(): TaskDetailData = TaskDetailData()
    }

    private var detailType by mutableStateOf<TaskDetailType?>(null)
    private var detailString by mutableStateOf("")
    private var bitmap by mutableStateOf<Bitmap?>(null)

    fun detailType(): TaskDetailType? = this.detailType
    fun detailType(setter: TaskDetailType?) {
        this.detailType = setter
    }

    fun detailBitmap(): Bitmap? = this.bitmap
    fun detailBitmap(setter: Bitmap?) {
        this.bitmap = setter
    }

    fun detailString(): String = this.detailString
    fun detailString(setter: String) {
        this.detailString = setter
    }

    fun setter(detailType: TaskDetailType, detailString: String, detailBitmap: Bitmap? = null) {
        this.detailType = detailType
        this.detailString = detailString
        this.bitmap = detailBitmap
    }

    fun releaseMemory() {
        this.detailType = null
        this.detailString = ""
        this.bitmap = null
    }
}

@Preview
@Composable
private fun Preview() {
    TaskDetailDialog(
        onDismissRequest = {}, confirm = { _, _, _ -> }, title = "TEST",
        detailData = TaskDetailData.instance(), view = LocalView.current
    )
}
