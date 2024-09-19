package com.sqz.checklist.ui.main.task.layout

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.main.NavTooltipContent
import com.sqz.checklist.ui.main.OnClickType
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.material.TaskChangeContentCard
import kotlinx.coroutines.launch

data class NavExtendedConnectData(
    val canScroll: Boolean = false,
    val scrollToFirst: Boolean = false,
    val scrollToBottom: Boolean = false,
    val searchState: Boolean = false,
    val canScrollForward: Boolean = false,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun taskExtendedNavButton(
    view: View,
    context: Context,
    viewModel: TaskLayoutViewModel
): NavExtendedButtonData {
    val connector = viewModel.navExtendedConnector.collectAsState().value
    var taskAddCard by rememberSaveable { mutableStateOf(false) }
    val buttonInfo = stringResource(if (!connector.searchState) R.string.add else R.string.cancel)
    val icon = @Composable {
        val icons = if (!connector.searchState) Icons.Filled.AddCircle else Icons.Filled.Close
        Icon(icons, contentDescription = buttonInfo)
    }
    val label = @Composable { Text(buttonInfo) }
    val tooltipState = rememberBasicTooltipState(isPersistent = true)
    val tooltipContent = @Composable {
        val coroutineScope = rememberCoroutineScope()
        if (connector.canScroll && !connector.searchState) NavTooltipContent(
            onClickType = { onClickType ->
                when (onClickType) {
                    OnClickType.Search -> {
                        tooltipState.dismiss()
                        val it = NavExtendedConnectData(searchState = true)
                        viewModel.updateNavConnector(it, it)
                    }

                    OnClickType.ScrollUp -> coroutineScope.launch {
                        tooltipState.dismiss()
                        val it = NavExtendedConnectData(scrollToFirst = true)
                        viewModel.updateNavConnector(it, it)
                    }

                    OnClickType.ScrollDown -> coroutineScope.launch {
                        tooltipState.dismiss()
                        val it = NavExtendedConnectData(scrollToBottom = true)
                        viewModel.updateNavConnector(it, it)
                    }
                }
            },
            view = view,
            scrollUp = !connector.canScrollForward
        )
    }
    val onClick = {
        if (!connector.searchState) taskAddCard = true else {
            val it = NavExtendedConnectData(searchState = false)
            viewModel.updateNavConnector(it, NavExtendedConnectData(searchState = true))
            viewModel.updateInSearch(reset = true)
        }
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    val state = rememberTextFieldState() // to add task
    val noDoNothing = stringResource(R.string.no_do_nothing)
    if (taskAddCard) TaskChangeContentCard(
        onDismissRequest = { taskAddCard = false },
        confirm = {
            if (state.text.toString() != "") {
                viewModel.insertTask(state.text.toString())
                taskAddCard = false
            } else {
                Toast.makeText(context, noDoNothing, Toast.LENGTH_SHORT).show()
            }
        },
        state = state,
        title = stringResource(R.string.create_task),
        confirmText = stringResource(R.string.add),
        doneImeAction = true
    ) else LaunchedEffect(true) {
        state.clearText()
    }

    return NavExtendedButtonData(
        icon = icon,
        label = label,
        tooltipContent = tooltipContent,
        tooltipState = tooltipState,
        onClick = onClick
    )
}
