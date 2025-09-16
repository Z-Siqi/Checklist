package com.sqz.checklist.ui.common.unit

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@ReadOnlyComposable
@Composable
fun screenIsWidth(): Boolean {
    val localConfig = LocalWindowInfo.current.containerSize
    return localConfig.width.pxToDpInt() > localConfig.height.pxToDpInt() * 1.1
}

@ReadOnlyComposable
@Composable
fun screenIsWidthAndAPI34Above(): Boolean {
    return isApi35AndAbove && screenIsWidth()
}

/** if screenIsWidthAndAPI34Above return WindowInsets.navigationBars.getBottom dp **/
@Composable
fun navBarsBottomDp(defDp: Dp = 10.dp): Dp {
    return if (!screenIsWidthAndAPI34Above()) defDp else {
        (WindowInsets.navigationBars.getBottom(LocalDensity.current) / LocalDensity.current.density).dp
    }
}
