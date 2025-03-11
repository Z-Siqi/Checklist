package com.sqz.checklist.ui.material

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.verticalColumnScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    showScrollBar: Boolean = true,
    scrollBarTrackColor: Color = Color.Gray,
    scrollBarColor: Color = Color.Black,
    scrollBarCornerRadius: Float = 4f,
    endPadding: Float = 12f,
    topBottomPadding: Float = 0f,
): Modifier {
    return if (showScrollBar) drawWithContent {
        val topBottom = if (topBottomPadding == 0f) 0f else topBottomPadding / 2
        // Draw the column's content
        drawContent()
        // Dimensions and calculations
        val viewportHeight = this.size.height - topBottomPadding
        val totalContentHeight = scrollState.maxValue.toFloat() + viewportHeight
        val scrollValue = scrollState.value.toFloat()
        // Compute scrollbar height and position
        val scrollBarHeight =
            (viewportHeight / totalContentHeight) * viewportHeight
        val scrollBarStartOffset =
            (scrollValue / totalContentHeight) * viewportHeight
        // Draw the track (disable: set scrollBarTrackColor to Color.Transparent)
        drawRoundRect(
            cornerRadius = CornerRadius(scrollBarCornerRadius),
            color = scrollBarTrackColor,
            topLeft = Offset(this.size.width - endPadding, topBottom),
            size = Size(width.toPx(), viewportHeight),
        )
        // Draw the scrollbar
         drawRoundRect(
            cornerRadius = CornerRadius(scrollBarCornerRadius),
            color = scrollBarColor,
            topLeft = Offset(this.size.width - endPadding, scrollBarStartOffset + topBottom),
            size = Size(width.toPx(), scrollBarHeight)
        )
    } else drawWithContent {
        drawContent()
    }
}
