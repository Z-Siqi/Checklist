package com.sqz.checklist.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.unit.isLandscape
import kotlin.math.min

const val extraSmallInLargestEdgeSize = 380
const val extraSmallInSmallestEdgeSize = 270

const val smallInLargestEdgeSize = 470
const val smallInSmallestEdgeSize = 310

const val normalInLargestEdgeSize = 524
const val normalInSmallestEdgeSize = 385

/**
 * A Composable function that clamps the font scale to a maximum value.
 * This helps prevent UI distortion on devices with very large font size settings.
 *
 * @param maxScale The maximum font scale to apply. Defaults to 1f (no scaling).
 * @param enabled A boolean to enable or disable the font scale clamping. Defaults to true.
 * @param content The Composable content to which the clamped font scale will be applied.
 */
@Composable
private fun ClampFontScale(
    maxScale: Float = 1f,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val cur = LocalDensity.current
    val clamped = if (enabled) min(cur.fontScale, maxScale) else cur.fontScale
    CompositionLocalProvider(
        LocalDensity provides Density(density = cur.density, fontScale = clamped)
    ) {
        content()
    }
}

/**
 * A Composable function that adjusts UI parameters based on the screen size.
 * It can limit the font scale on smaller screens to ensure a consistent and readable layout.
 * The function determines the screen size category (normal, small, extra small) and applies a corresponding maximum font scale.
 *
 * @param enabled A boolean to enable or disable the size-limiting behavior. Defaults to true.
 * @param content The Composable content to be displayed within this size-limited scope.
 */
@Composable
fun UISizeLimit(
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val containerSizePx = LocalWindowInfo.current.containerSize
    val widthDp: Int = containerSizePx.width.pxToDpInt()
    val heightDp: Int = containerSizePx.height.pxToDpInt()

    val enableClampFontScale: Boolean = if (isLandscape()) {
        val heightRequired = heightDp < normalInSmallestEdgeSize
        heightRequired
    } else {
        val widthRequired = widthDp < normalInSmallestEdgeSize
        val heightRequired = heightDp < normalInLargestEdgeSize
        widthRequired && heightRequired
    }
    if (!enabled) content() else {
        val isSmall: Boolean = if (isLandscape()) {
            val heightRequired = heightDp < smallInSmallestEdgeSize
            heightRequired
        } else {
            val widthRequired = widthDp < smallInSmallestEdgeSize
            val heightRequired = heightDp < smallInLargestEdgeSize
            widthRequired && heightRequired
        }
        val isExtraSmall: Boolean = if (isLandscape()) {
            val heightRequired = heightDp < extraSmallInSmallestEdgeSize
            heightRequired
        } else {
            val widthRequired = widthDp < extraSmallInSmallestEdgeSize
            val heightRequired = heightDp < extraSmallInLargestEdgeSize
            widthRequired && heightRequired
        }
        val maxScale: Float = when {
            isExtraSmall -> 0.9f
            isSmall -> 1.2f
            else -> LocalDensity.current.fontScale
        }
        ClampFontScale(
            maxScale = maxScale,
            enabled = enableClampFontScale,
            content = content
        )
    }
}
