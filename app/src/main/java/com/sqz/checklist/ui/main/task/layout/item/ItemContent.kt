package com.sqz.checklist.ui.main.task.layout.item

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
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
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
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

@Composable
internal fun ItemContent(
    description: String,
    createDate: String,
    descriptionBgColor: CardColors = CardDefaults.cardColors(Color.Transparent),
    reminderOnClick: () -> Unit,
    editOnClick: () -> Unit,
    timerIconState: Boolean,
    pinOnClick: () -> Unit,
    pinIconState: Boolean,
    tooltipRemindText: String?,
    modifier: Modifier = Modifier
) { // The card details UI
    val view = LocalView.current
    Column {
        ContentTop(
            description = description, pinOnClick = pinOnClick,
            pinIcon = if (pinIconState) R.drawable.pinned else R.drawable.pin,
            cardColors = descriptionBgColor,
            view = view
        )
        Spacer(modifier = modifier.weight(1f))
        ContentBottom(
            createDate = createDate, reminderOnClick = reminderOnClick, editOnClick = editOnClick,
            timerIcon = if (timerIconState) R.drawable.timer_on else R.drawable.timer,
            tooltipText = tooltipRemindText ?: stringResource(R.string.reminder),
            view = view
        )
    }
}

@Composable
private fun ContentTop(
    description: String,
    pinOnClick: () -> Unit,
    pinIcon: Int,
    cardColors: CardColors,
    view: View,
    modifier: Modifier = Modifier
) {
    Row {
        Column(
            modifier = modifier
                .fillMaxWidth(0.75f)
                .height(64.dp),
            horizontalAlignment = Alignment.Start
        ) {
            var overflowState by rememberSaveable { mutableStateOf(false) }
            var overflowInfo by rememberSaveable { mutableStateOf(false) }
            Card(colors = cardColors, modifier = modifier.padding(top = 8.dp)) {
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
            if (overflowInfo) {
                InfoAlertDialog(
                    onDismissRequest = { overflowInfo = false },
                    text = description
                )
            }
        }
        Spacer(modifier = modifier.weight(1f))
        Tooltip(tooltipText = stringResource(R.string.pin), view = view) {
            IconButton(
                modifier = modifier.rotate(40f),
                onClick = {
                    pinOnClick()
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            ) {
                Icon(
                    painter = painterResource(pinIcon),
                    contentDescription = stringResource(R.string.pin)
                )
            }
        }
    }
}

@Composable
private fun ContentBottom(
    createDate: String,
    reminderOnClick: () -> Unit,
    editOnClick: () -> Unit,
    timerIcon: Int,
    tooltipText: String,
    view: View,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = createDate,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(start = 2.dp)
        )
        Spacer(
            modifier = modifier
                .weight(1f)
                .widthIn(min = 10.dp)
        )
        Row(modifier = modifier.widthIn(min = 40.dp, max = 100.dp)) {
            Tooltip(
                tooltipText = tooltipText,
                view = view
            ) {
                IconButton(modifier = modifier.size(30.dp), onClick = {
                    reminderOnClick()
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Icon(
                        painter = painterResource(id = timerIcon),
                        contentDescription = stringResource(R.string.reminder)
                    )
                }
            }
            Spacer(modifier = modifier.weight(0.2f))
            Tooltip(tooltipText = stringResource(R.string.edit), view = view) {
                IconButton(modifier = modifier.size(30.dp), onClick = {
                    editOnClick()
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
    Surface(modifier = Modifier.size(500.dp, 120.dp)) {
        ItemContent(
            description = "description", createDate = "createDate",
            reminderOnClick = {}, editOnClick = {},
            timerIconState = false, pinOnClick = {},
            tooltipRemindText = null, pinIconState = false
        )
    }
}
