package com.sqz.checklist.ui.main.task.layout

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.main.NavTooltipContent
import com.sqz.checklist.ui.main.OnClickType
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.material.TaskChangeContentCard
import kotlinx.coroutines.launch

/** Nav Extended Button Connect Data **/
data class NavConnectData(
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
    val extendedTooltipState = connector.canScroll && !connector.searchState
    val tooltipState = rememberBasicTooltipState(isPersistent = extendedTooltipState)
    val tooltipContent = @Composable {
        val coroutineScope = rememberCoroutineScope()
        if (extendedTooltipState) NavTooltipContent(
            onClickType = { onClickType ->
                when (onClickType) {
                    OnClickType.Search -> {
                        tooltipState.dismiss()
                        val it = NavConnectData(searchState = true)
                        viewModel.updateNavConnector(it, it)
                    }

                    OnClickType.ScrollUp -> coroutineScope.launch {
                        tooltipState.dismiss()
                        val it = NavConnectData(scrollToFirst = true)
                        viewModel.updateNavConnector(it, it)
                    }

                    OnClickType.ScrollDown -> coroutineScope.launch {
                        tooltipState.dismiss()
                        val it = NavConnectData(scrollToBottom = true)
                        viewModel.updateNavConnector(it, it)
                    }
                }
            },
            view = view,
            scrollUp = !connector.canScrollForward
        ) else NonExtendedTooltip(
            text = buttonInfo,
            view = view
        )
    }
    val onClick = {
        if (!connector.searchState) taskAddCard = true else {
            val it = NavConnectData(searchState = false)
            viewModel.updateNavConnector(it, NavConnectData(searchState = true))
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
                Toast.makeText(view.context, noDoNothing, Toast.LENGTH_SHORT).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NonExtendedTooltip(text: String, view: View) {
    val localConfig = LocalConfiguration.current
    val landscape = localConfig.screenWidthDp > localConfig.screenHeightDp * 1.1
    val height = (localConfig.screenHeightDp - if (landscape) 110 else 91).dp
    val width = if (localConfig.screenWidthDp < 738) 0.1569f else 0.1615f
    Row(Modifier.fillMaxWidth(1f), Arrangement.End) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(height),
            tooltip = {
                PlainTooltip { Text(text = text) }
                LaunchedEffect(true) { // click feedback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.getSystemService(
                        view.context, Vibrator::class.java
                    )?.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    ) else view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            },
            state = rememberTooltipState(initialIsVisible = true)
        ) {}
        Spacer(modifier = Modifier.fillMaxWidth(width))
    }
}
