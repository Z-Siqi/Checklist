package com.sqz.checklist.ui.main.task.layout.item

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.ui.material.TextTooltipBox
import com.sqz.checklist.ui.material.dialog.InfoAlertDialog
import com.sqz.checklist.ui.theme.Theme
import com.sqz.checklist.ui.theme.unit.pxToDpInt

enum class CardClickType { Reminder, Edit, Pin, Close, Detail }

enum class ItemMode {
    NormalTask, PinnedTask, RemindedTask
}

/**
 * The content of the task UI
 */
@Composable
fun TaskCardContent(
    description: String,
    dateText: (overflowed: Boolean) -> String,
    onClick: (CardClickType) -> Unit,
    timerIconState: Boolean,
    pinIconState: Boolean,
    tooltipRemindText: String?,
    mode: ItemMode,
    isDetail: Boolean,
    modifier: Modifier = Modifier,
) { // card UI
    val view = LocalView.current
    val density = LocalDensity.current
    val colors = Theme.color
    val topScale = 0.55
    var parentHeight by remember { mutableIntStateOf((64 / topScale).toInt()) }
    OutlinedCard(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                val heightPx = layoutCoordinates.size.height
                parentHeight = with(density) { heightPx.toDp() }.value.toInt()
            },
        colors = CardDefaults.cardColors(colors.taskBackgroundColor),
        border = BorderStroke(1.dp, colors.taskBorderColor),
        shape = ShapeDefaults.ExtraLarge
    ) { // Must be Modifier not modifier in below Column
        Column(Modifier.padding(bottom = 7.dp, top = 5.dp, start = 12.dp, end = 12.dp)) {
            ContentTop(
                description = description,
                onClick = {
                    onClick(if (mode == ItemMode.RemindedTask) CardClickType.Close else CardClickType.Pin)
                },
                topRightIcon = if (mode == ItemMode.RemindedTask) R.drawable.close else {
                    if (pinIconState) R.drawable.pinned else R.drawable.pin
                },
                rotation = mode != ItemMode.RemindedTask,
                view = view,
                modifier = Modifier.heightIn(max = (parentHeight * topScale).dp)
            )
            Spacer(modifier = modifier.weight(1f))
            ContentBottom(
                dateText = dateText,
                onClick = onClick,
                timerIcon = if (timerIconState) R.drawable.timer_on else R.drawable.timer,
                attachIcon = if (isDetail) R.drawable.attach else null,
                tooltipText = tooltipRemindText ?: stringResource(R.string.reminder),
                view = view
            )
        }
    }
}

@Composable
private fun ContentTop(
    description: String,
    onClick: () -> Unit,
    topRightIcon: Int,
    rotation: Boolean,
    view: View,
    modifier: Modifier = Modifier
) = Row {
    Column(
        modifier = modifier.fillMaxWidth(0.75f),
        horizontalAlignment = Alignment.Start
    ) {
        var overflowState by rememberSaveable { mutableStateOf(false) }
        var overflowInfo by rememberSaveable { mutableStateOf(false) }
        Card(
            colors = CardDefaults.cardColors(Color.Transparent),
            modifier = modifier.padding(top = 8.dp)
        ) {
            Box(modifier = modifier.clickable(overflowState) { overflowInfo = true }) {
                Text(
                    text = description,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(start = 5.dp),
                    fontSize = 21.sp,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult: TextLayoutResult ->
                        overflowState = textLayoutResult.hasVisualOverflow
                    },
                    maxLines = 2,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        if (overflowInfo) InfoAlertDialog(
            onDismissRequest = { overflowInfo = false },
            text = description
        )
    }
    Spacer(modifier = modifier.weight(1f))
    TextTooltipBox(if (topRightIcon == R.drawable.pin) R.string.pin else R.string.cancel_highlight) {
        IconButton(
            modifier = if (rotation) modifier.rotate(40f) else modifier,
            onClick = {
                onClick()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        ) {
            Icon(
                painter = painterResource(topRightIcon),
                contentDescription = stringResource(R.string.pin)
            )
        }
    }
}

@Composable
private fun ContentBottom(
    dateText: (overflowed: Boolean) -> String,
    onClick: (CardClickType) -> Unit,
    timerIcon: Int,
    attachIcon: Int?,
    tooltipText: String,
    view: View
) = Row(verticalAlignment = Alignment.Bottom) {
    val modifier = Modifier
    val config = LocalWindowInfo.current.containerSize
    val screenIsWidth = config.width > config.height * 1.1
    val timeWidth =
        if (screenIsWidth) config.width.pxToDpInt() / 1.95 else config.width.pxToDpInt() / 1.7
    var overflowed by remember { mutableStateOf(false) }
    val localDateText = dateText(overflowed)
    Text(
        text = localDateText, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp,
        modifier = modifier.padding(
            start = 5.dp, bottom = 3.dp
        ) then modifier.widthIn(max = timeWidth.dp),
        onTextLayout = { overflowed = it.hasVisualOverflow || it.lineCount > 1 },
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = modifier.weight(1f) then modifier.widthIn(min = 10.dp))
    Row(
        modifier = modifier.widthIn(
            max = if (attachIcon != null) 150.dp else 100.dp,
            min = if (attachIcon != null) 150.dp else 150.dp,
        ),
        horizontalArrangement = Arrangement.End
    ) {
        attachIcon?.let {
            TextTooltipBox(textRid = R.string.detail) {
                IconButton(modifier = modifier.size(30.dp), onClick = {
                    onClick(CardClickType.Detail)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Icon(
                        modifier = modifier.rotate(-5f),
                        painter = painterResource(id = attachIcon),
                        contentDescription = stringResource(R.string.detail)
                    )
                }
            }
            Spacer(modifier = modifier.weight(0.18f))
        }
        TextTooltipBox(text = tooltipText) {
            IconButton(modifier = modifier.size(30.dp), onClick = {
                onClick(CardClickType.Reminder)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Icon(
                    painter = painterResource(id = timerIcon),
                    contentDescription = stringResource(R.string.reminder)
                )
            }
        }
        Spacer(modifier = modifier.weight(0.2f))
        TextTooltipBox(textRid = R.string.edit) {
            IconButton(modifier = modifier.size(30.dp), onClick = {
                onClick(CardClickType.Edit)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = stringResource(R.string.edit)
                )
            }
        }
        Spacer(modifier = modifier.weight(0.2f))
    }
}

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.size(500.dp, 120.dp)) {
        TaskCardContent(
            description = "description", dateText = { "createDate" }, onClick = {},
            timerIconState = false, pinIconState = false,
            tooltipRemindText = null, mode = ItemMode.NormalTask, isDetail = true
        )
    }
}
