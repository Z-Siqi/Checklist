package com.sqz.checklist.ui.common

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun TextTooltipBox(
    textRid: Int,
    modifier: Modifier = Modifier,
    topLeftExtraPadding: Boolean = false,
    topRightExtraPadding: Boolean = false,
    content: @Composable () -> Unit,
) = TextTooltipBox(
    text = stringResource(id = textRid),
    modifier = modifier,
    topLeftExtraPadding = topLeftExtraPadding,
    topRightExtraPadding = topRightExtraPadding,
    content = content
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    topLeftExtraPadding: Boolean = false,
    topRightExtraPadding: Boolean = false,
    enable: Boolean = true,
    content: @Composable () -> Unit,
) = TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
        TooltipAnchorPosition.Above
    ),
    tooltip = {
        val extraPaddingValue = if (topLeftExtraPadding) {
            modifier.padding(top = 25.dp, bottom = 20.dp)
        } else if (topRightExtraPadding) {
            modifier.padding(top = 25.dp, end = 20.dp)
        } else modifier
        PlainTooltip(modifier = extraPaddingValue) {
            Text(text = text)
            val view = LocalView.current
            LaunchedEffect(true) { // click feedback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.getSystemService(
                    view.context, Vibrator::class.java
                )?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                ) else view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }
    },
    state = rememberTooltipState(),
    enableUserInput = enable,
    content = content
)
