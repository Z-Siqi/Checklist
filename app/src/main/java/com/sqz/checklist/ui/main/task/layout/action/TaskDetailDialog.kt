package com.sqz.checklist.ui.main.task.layout.action

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
import com.sqz.checklist.ui.material.dialog.DialogWithMenu

@Composable
fun TaskDetailDialog(
    onDismissRequest: (onDismissClick: Boolean?) -> Unit,
    confirm: (detailType: TaskDetailType, detailString: String) -> Unit,
    title: String,
    getType: TaskDetailType?,
    getString: String?,
    view: View
) {
    val detailTextState = rememberTextFieldState()
    val noDoNothing = stringResource(R.string.no_do_nothing)
    var detailType by rememberSaveable { mutableStateOf<TaskDetailType?>(null) }
    var remember by rememberSaveable { mutableStateOf(false) }
    var packageName by rememberSaveable { mutableStateOf("") }
    if (!remember && getType != null && getString != null) LaunchedEffect(Unit) {
        detailType = getType
        if (getType == TaskDetailType.Application) packageName = getString
        detailTextState.clearText()
        detailTextState.edit { insert(0, getString) }
        remember = true
    }
    DialogWithMenu(
        onDismissRequest = onDismissRequest,
        confirm = {
            if (detailTextState.text.toString() != "" && it != null || packageName != "") {
                val notURL = view.context.getString(R.string.invalid_url)
                if (it == TaskDetailType.URL &&
                    !Patterns.WEB_URL.matcher(detailTextState.text.toString()).matches()
                ) Toast.makeText(view.context, notURL, Toast.LENGTH_SHORT).show() else {
                    detailType = when (it) {
                        TaskDetailType.Text -> TaskDetailType.Text
                        TaskDetailType.URL -> TaskDetailType.URL
                        TaskDetailType.Application -> TaskDetailType.Application
                        else -> null
                    }
                    val detailString = when {
                        it == TaskDetailType.Application -> packageName
                        it == TaskDetailType.URL && !detailTextState.text.toString()
                            .startsWith("http") -> {
                            detailTextState.edit { insert(0, "http://") }
                            detailTextState.text.toString()
                        }

                        else -> detailTextState.text.toString()
                    }
                    confirm(detailType!!, detailString)
                }
            } else Toast.makeText(view.context, noDoNothing, Toast.LENGTH_SHORT).show()
        },
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(if (getType == null) R.string.dismiss else R.string.delete),
        title = title,
        menuListGetter = TaskDetailType.entries.toTypedArray(),
        menuText = {
            when (it) {
                TaskDetailType.Text -> view.context.getString(R.string.text)
                TaskDetailType.URL -> view.context.getString(R.string.url)
                TaskDetailType.Application -> view.context.getString(R.string.application)
                else -> view.context.getString(R.string.click_select_detail_type)
            }
        },
        functionalType = {
            when (it) {
                TaskDetailType.Text -> false
                TaskDetailType.URL -> false
                TaskDetailType.Application -> ApplicationList({ name ->
                    packageName = name
                }, packageName, view.context) == Unit

                else -> Text(
                    stringResource(R.string.select_detail_type),
                    Modifier.padding(7.dp), MaterialTheme.colorScheme.secondary, 16.sp
                ) == Unit
            }
        },
        defaultType = detailType,
        currentMenuSelection = {
            if (it != null && detailType != it) {
                packageName = ""
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
                else -> KeyboardType.Unspecified
            }
        },
        state = detailTextState
    )
}

@Preview
@Composable
private fun Preview() {
    TaskDetailDialog(
        onDismissRequest = {}, confirm = { _, _ -> }, title = "TEST", getType = TaskDetailType.Text,
        getString = "TEST", view = LocalView.current
    )
}
