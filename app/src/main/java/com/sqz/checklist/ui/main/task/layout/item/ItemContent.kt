package com.sqz.checklist.ui.main.task.layout.item

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sqz.checklist.R
import com.sqz.checklist.ui.material.InfoAlertDialog

enum class CardClickType { Reminder, Edit, Pin, Close }
data class EditState(val id: Int = -1, val description: String = "", val state: Boolean = false)
enum class ItemMode {
    NormalTask, PinnedTask, RemindedTask
}

/**
 * The content of the task UI
 */
@Composable
fun ItemContent(
    description: String,
    dateText: String,
    onClick: (CardClickType) -> Unit,
    timerIconState: Boolean,
    pinIconState: Boolean,
    tooltipRemindText: String?,
    mode: ItemMode,
    modifier: Modifier = Modifier,
) { // card UI
    val view = LocalView.current
    OutlinedCard(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceDim),
        shape = ShapeDefaults.ExtraLarge
    ) { // Must be Modifier not modifier in below Column
        Column(Modifier.padding(bottom = 8.dp, top = 5.dp, start = 12.dp, end = 11.dp)) {
            ContentTop(
                description = description,
                onClick = {
                    onClick(if (mode == ItemMode.RemindedTask) CardClickType.Close else CardClickType.Pin)
                },
                topRightIcon = if (mode == ItemMode.RemindedTask) R.drawable.close else {
                    if (pinIconState) R.drawable.pinned else R.drawable.pin
                },
                rotation = mode != ItemMode.RemindedTask,
                view = view
            )
            Spacer(modifier = modifier.weight(1f))
            ContentBottom(
                createDate = dateText,
                onClick = onClick,
                timerIcon = if (timerIconState) R.drawable.timer_on else R.drawable.timer,
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
        modifier = modifier
            .fillMaxWidth(0.75f)
            .height(64.dp),
        horizontalAlignment = Alignment.Start
    ) {
        var overflowState by rememberSaveable { mutableStateOf(false) }
        var overflowInfo by rememberSaveable { mutableStateOf(false) }
        Card(
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
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
    Tooltip(
        tooltipText = stringResource(
            if (topRightIcon == R.drawable.pin) R.string.pin else R.string.cancel_highlight
        ),
        view = view
    ) {
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
    createDate: String,
    onClick: (CardClickType) -> Unit,
    timerIcon: Int,
    tooltipText: String,
    view: View,
    modifier: Modifier = Modifier
) = Row(verticalAlignment = Alignment.Bottom) {
    val minWidth = modifier.widthIn(min = 10.dp)
    Text(
        text = createDate, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 14.sp, modifier = modifier.padding(start = 2.dp)
    )
    Spacer(modifier = modifier.weight(1f) then minWidth)
    Row(modifier = modifier.widthIn(min = 40.dp, max = 100.dp)) {
        Tooltip(
            tooltipText = tooltipText, view = view
        ) {
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
        Tooltip(
            tooltipText = stringResource(R.string.edit), view = view
        ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tooltip(
    tooltipText: String,
    view: View,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(text = tooltipText)
                LaunchedEffect(true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContextCompat.getSystemService(
                            context, Vibrator::class.java
                        )?.vibrate(
                            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        )
                    } else view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            }
        },
        state = rememberTooltipState(),
        content = content
    )
}

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.size(500.dp, 120.dp)) {
        ItemContent(
            description = "description", dateText = "createDate", onClick = {},
            timerIconState = false, pinIconState = false,
            tooltipRemindText = null, mode = ItemMode.NormalTask
        )
    }
}
