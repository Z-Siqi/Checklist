package com.sqz.checklist.ui.nav.group.home.button

import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.unit.isLandscape
import com.sqz.checklist.ui.nav.group.home.HomeNavGroup
import com.sqz.checklist.ui.nav.group.home.HomeNavGroupExtendedButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.common.EffectFeedback

interface TaskExtendedButton {

    sealed interface State {

        data object LongClickMenuDisabled : State

        data class LongClickMenuEnabled(val isScrollUpButton: Boolean) : State

        data object CancelClickStyle : State
    }

    enum class ClickRequest {
        Add, ScrollUp, ScrollDown, Search, Cancel
    }
}

@Composable
fun HomeNavGroup?.taskExtendedButton(
    state: StateFlow<TaskExtendedButton.State?>,
    onClickRequest: (TaskExtendedButton.ClickRequest) -> Unit,
    feedback: EffectFeedback,
): HomeNavGroupExtendedButton {
    if (this != HomeNavGroup.TaskNavRoute && this != null) {
        throw IllegalStateException("taskExtendedButton in wrong nav route!")
    }
    val stateValue by state.collectAsState()

    val isLandscape = isLandscape()
    var showMenuLayout by remember { mutableStateOf(false) }

    val buttonText = stateValue.let {
        if (it is TaskExtendedButton.State.CancelClickStyle) {
            return@let stringResource(R.string.cancel)
        }
        if (showMenuLayout) {
            return@let stringResource(R.string.cancel)
        }
        return@let stringResource(R.string.add)
    }
    val icon: @Composable (() -> Unit) = {
        val imageVector = stateValue.let {
            if (it is TaskExtendedButton.State.CancelClickStyle) {
                return@let Icons.Filled.Close
            }
            return@let Icons.Filled.AddCircle
        }
        val rotateDegrees by animateFloatAsState(targetValue = if (showMenuLayout) 45f else 0f)
        Icon(
            imageVector = imageVector,
            contentDescription = buttonText,
            modifier = Modifier.rotate(rotateDegrees)
        )
    }
    val label: @Composable (() -> Unit) = {
        var delayedButtonText by remember { mutableStateOf(buttonText) }
        LaunchedEffect(buttonText) {
            if (delayedButtonText != buttonText) {
                delay(100)
                delayedButtonText = buttonText
            }
        }
        LookaheadScope {
            Text(text = delayedButtonText, modifier = Modifier.animateBounds(this))
        }
    }
    val menu: @Composable (() -> Boolean) = {
        val menuState = stateValue as? TaskExtendedButton.State.LongClickMenuEnabled
            ?: throw IllegalStateException("TaskExtendedButton.State error!")
        DisposableEffect(Unit) {
            showMenuLayout = true
            feedback.onTapEffect()

            onDispose {
                showMenuLayout = false
            }
        }
        ExtendedButton(
            isLandscape = isLandscape,
            onSearchClick = {
                onClickRequest(TaskExtendedButton.ClickRequest.Search)
                showMenuLayout = false
                feedback.onClickEffect()
            },
            isScrollUpIcon = menuState.isScrollUpButton,
            onScrollClick = {
                val scroll = menuState.isScrollUpButton.let {
                    if (it) {
                        return@let TaskExtendedButton.ClickRequest.ScrollUp
                    }
                    return@let TaskExtendedButton.ClickRequest.ScrollDown
                }
                onClickRequest(scroll)
                showMenuLayout = false
                feedback.onClickEffect()
            }
        )
        var returnSuspend by remember { mutableStateOf(true) }
        if (showMenuLayout) {
            returnSuspend = false
        }
        showMenuLayout || returnSuspend
    }
    val isLongClickMenuDisabled = stateValue is TaskExtendedButton.State.LongClickMenuDisabled
    val isLongClickMenuEnabled = stateValue is TaskExtendedButton.State.LongClickMenuEnabled
    return HomeNavGroupExtendedButton(
        icon = icon,
        label = label,
        tooltip = if (isLongClickMenuDisabled) buttonText else null,
        menu = if (isLongClickMenuEnabled) menu else null,
    ) { // onClick
        if (stateValue is TaskExtendedButton.State.CancelClickStyle) {
            onClickRequest(TaskExtendedButton.ClickRequest.Cancel)
        } else {
            onClickRequest(TaskExtendedButton.ClickRequest.Add)
        }
        feedback.onClickEffect()
    }
}

@Composable
private fun ExtendedButton(
    isLandscape: Boolean,
    onSearchClick: () -> Unit,
    isScrollUpIcon: Boolean,
    onScrollClick: () -> Unit,
    modifier: Modifier = Modifier,
) = OutlinedCard(
    shape = CircleShape,
    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceDim),
    modifier = modifier.padding(8.dp),
    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright)
) {
    ButtonsScaffold(
        isLandscape = isLandscape,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = (8.5).dp, bottom = (8.5).dp)
    ) {
        TextTooltipBox(textRid = R.string.search) {
            ButtonItem(onClick = onSearchClick) {
                Icon(
                    modifier = modifier.size(25.dp),
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }
        }
        Spacer(
            modifier = Modifier.size(height = 8.dp, width = 8.dp)
        )
        val scrollText: String = isScrollUpIcon.let {
            if (it) stringResource(R.string.scroll_to_top) else stringResource(R.string.scroll_to_end)
        }
        val scrollIcon: ImageVector = isScrollUpIcon.let {
            if (it) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
        }
        TextTooltipBox(text = scrollText) {
            ButtonItem(onClick = onScrollClick) {
                Icon(
                    modifier = modifier.size(25.dp),
                    imageVector = scrollIcon,
                    contentDescription = scrollText
                )
            }
        }
    }
}

@Composable
private fun ButtonsScaffold(
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = if (!isLandscape) Column(
    modifier = modifier, verticalArrangement = Arrangement.Center, content = { content() }
) else Row(
    modifier = modifier, horizontalArrangement = Arrangement.Center, content = { content() }
)

@Composable
private fun ButtonItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) = Card(
    shape = CircleShape,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    modifier = modifier.size(58.dp, 55.dp)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(top = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Preview
@Composable
private fun ExtendedButtonPreview() {
    ExtendedButton(
        isLandscape = false,
        onSearchClick = {},
        isScrollUpIcon = false,
        onScrollClick = {}
    )
}
