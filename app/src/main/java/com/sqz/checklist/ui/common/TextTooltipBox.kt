package com.sqz.checklist.ui.common

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.PaddingValues
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTooltipBox(
    textRid: Int,
    modifier: Modifier = Modifier,
    extraPadding: PaddingValues = PaddingValues(0.dp),
    enable: Boolean = true,
    content: @Composable () -> Unit,
) = TextTooltipBox(
    text = stringResource(id = textRid),
    modifier = modifier,
    extraPadding = extraPadding,
    enable = enable,
    content = content
)

/** This method no need `@OptIn(ExperimentalMaterial3Api::class)` on called method **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    extraPadding: PaddingValues = PaddingValues(0.dp),
    enable: Boolean = true,
    content: @Composable () -> Unit,
) = TextTooltipBox(
    text = text,
    modifier = modifier,
    extraPadding = extraPadding,
    positioning = TooltipAnchorPosition.Above,
    enable = enable,
    content = content
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    extraPadding: PaddingValues = PaddingValues(0.dp),
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    enable: Boolean = true,
    content: @Composable (() -> Unit),
) {
    val rememberTooltipState = rememberTooltipState()
    val view = LocalView.current
    if (rememberTooltipState.isVisible) LaunchedEffect(Unit) {
        // click feedback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.getSystemService(
            view.context, Vibrator::class.java
        )?.vibrate(
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        ) else view.playSoundEffect(SoundEffectConstants.CLICK)
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
        tooltip = {
            PlainTooltip(modifier = modifier.padding(extraPadding)) {
                Text(text = text)
            }
        },
        state = rememberTooltipState,
        enableUserInput = enable,
        content = content
    )
}
