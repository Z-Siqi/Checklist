package com.sqz.checklist.ui.material

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NonExtendedTooltip(text: String, view: View) {
    var rememberPosition by remember { mutableStateOf(IntOffset.Zero) }
    var rememberTextWidth by remember { mutableIntStateOf(0) }
    var rememberTextHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Text(text = text, color = Color.Transparent, onTextLayout = {
        rememberTextHeight = with(density) { it.size.height.toDp() }.value.toInt()
        rememberTextWidth = with(density) { it.size.width.toDp() }.value.toInt() + 10
    })
    Spacer(modifier = Modifier
        .size(rememberTextWidth.dp, rememberTextHeight.dp)
        .onGloballyPositioned { layoutCoordinates ->
            val position = layoutCoordinates.positionOnScreen()
            Log.d("NonExtendedTooltip", "position = x: ${position.x} | y: ${position.y}")
            if (position.x < 2147483647L || position.y < 2147483647L) {
                rememberPosition = IntOffset(position.x.toInt(), position.y.toInt())
            }
        })
    Log.d("NonExtendedTooltip", "rememberText = Height: $rememberTextWidth | Width: $rememberTextHeight")
    if (rememberTextWidth != 0 && rememberTextHeight != 0) TooltipBox(
        positionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset(rememberPosition.x, rememberPosition.y)
        },
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
        state = rememberTooltipState(initialIsVisible = true, isPersistent = true)
    ) {}
}
