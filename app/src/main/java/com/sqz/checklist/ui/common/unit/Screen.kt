package com.sqz.checklist.ui.common.unit

import android.provider.Settings
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqz.checklist.ui.theme.smallInLargestEdgeSize
import com.sqz.checklist.ui.theme.smallInSmallestEdgeSize

@ReadOnlyComposable
@Composable
fun isLandscape(minWidthDp: Dp? = null): Boolean {
    val localConfig = LocalWindowInfo.current.containerSize
    val minWidthRequired: Boolean = if (minWidthDp == null) true else {
        localConfig.width.pxToDp() > minWidthDp
    }
    return localConfig.width.pxToDpInt() > localConfig.height.pxToDpInt() * 1.1 && minWidthRequired
}

@ReadOnlyComposable
@Composable
fun screenIsWidthAndAPI34Above(): Boolean {
    return isApi35AndAbove && isLandscape()
}

/** if screenIsWidthAndAPI34Above return WindowInsets.navigationBars.getBottom dp **/
@Composable
fun navBarsBottomDp(defDp: Dp = 10.dp): Dp {
    return if (!screenIsWidthAndAPI34Above()) defDp else {
        (WindowInsets.navigationBars.getBottom(LocalDensity.current) / LocalDensity.current.density).dp
    }
}

@Composable
fun isGestureNavigationMode(
    isGestureHeight: (Boolean, Float) -> Unit = { _, _ -> },
    isGestureMode: (Boolean, Int) -> Unit = { _, _ -> }
): Boolean {
    // Get via surmise
    val insets = WindowInsets.navigationBars.asPaddingValues()
    val navCalibrate = with(LocalDensity.current) {
        val layoutDirection = LocalLayoutDirection.current
        val top = insets.calculateTopPadding().toSp()
        val bottom = insets.calculateBottomPadding().toSp()
        val left = insets.calculateLeftPadding(layoutDirection).toSp()
        val right = insets.calculateRightPadding(layoutDirection).toSp()
        top.toDp() + bottom.toDp() + left.toDp() + right.toDp()
    }
    val isGestureHeight: Boolean = navCalibrate.value < 36f
    // Get via navigation_mode
    val navMode =
        Settings.Secure.getInt(LocalContext.current.contentResolver, "navigation_mode", 0)
    val isGestureMode: Boolean = navMode == 2
    // Return result
    isGestureHeight(isGestureHeight, navCalibrate.value)
    isGestureMode(isGestureMode, navMode)
    return isGestureMode || isGestureHeight
}

@ReadOnlyComposable
@Composable
fun isSmallScreenSizeForDialog(): Boolean {
    val containerSizePx = LocalWindowInfo.current.containerSize
    val widthDp: Int = containerSizePx.width.pxToDpInt()
    val heightDp: Int = containerSizePx.height.pxToDpInt()
    // minimal height size for dialog on portrait
    if (isLandscape()) {
        return heightDp < smallInSmallestEdgeSize
    }
    // minimal overall size for dialog
    val widthRequired = widthDp < smallInSmallestEdgeSize
    val heightRequired = heightDp < smallInLargestEdgeSize
    return widthRequired && heightRequired
}
