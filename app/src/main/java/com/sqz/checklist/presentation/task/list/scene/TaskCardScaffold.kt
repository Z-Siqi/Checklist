package com.sqz.checklist.presentation.task.list.scene

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.main.task.CardHeight
import kotlin.math.min

/** This method expected to be called only within this package and its sub-packages. **/
internal object TaskCardScaffold {

    @Stable
    internal data class TitleState(
        val text: String,
        val onOverflowedClick: () -> Unit,
        val onLongClick: () -> Unit,
        val titleEndRowButton: @Composable () -> Unit,
    )

    @Stable
    internal data class SubTitleState(
        val text: String,
        val textTooltip: String = text,
        val tooltipEnabled: Boolean = false,
        val subTitleEndRowButtons: @Composable () -> Unit,
    )

    @Immutable
    internal enum class BgShape(val shape: Shape) {
        Whole(ShapeDefaults.ExtraLarge),
        TopWhole(ShapeDefaults.let {
            val corner = it.Medium
            it.ExtraLarge.copy(bottomStart = corner.bottomStart, bottomEnd = corner.bottomEnd)
        }),
        CenterWhole(ShapeDefaults.Large),
        BottomWhole(ShapeDefaults.let {
            val corner = it.Medium
            it.ExtraLarge.copy(topStart = corner.topStart, topEnd = corner.topEnd)
        }),
    }
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun TaskCardScaffold(
    titleState: TaskCardScaffold.TitleState,
    subTitleState: TaskCardScaffold.SubTitleState,
    backgroundColor: Color,
    backgroundBorder: BorderStroke,
    backgroundShape: TaskCardScaffold.BgShape,
    modifier: Modifier,
) {
    TaskCardBackground(
        color = backgroundColor,
        border = backgroundBorder,
        shape = backgroundShape.shape,
        modifier = modifier
    ) {
        Row(modifier = Modifier, horizontalArrangement = Arrangement.Start) {
            TaskDescription(
                description = titleState.text,
                onOverflowedClick = titleState.onOverflowedClick,
                onLongClick = titleState.onLongClick,
            )
            Spacer(modifier = Modifier.weight(1f))
            titleState.titleEndRowButton()
        }
        Row(
            modifier = Modifier.heightIn(min = CardHeight.dp * 0.38f),
            verticalAlignment = Alignment.Bottom
        ) {
            TaskSubDescription(
                text = subTitleState.text,
                tooltipText = subTitleState.textTooltip,
                enableTooltip = subTitleState.tooltipEnabled,
                modifier = Modifier.weight(1f)
            )
            subTitleState.subTitleEndRowButtons()
        }
    }
}

@Composable
private fun TaskDescription(
    description: String,
    onOverflowedClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val twoLinesHeightDp =
        with(LocalDensity.current) { ((21.sp).toPx() * 2.5f).toDp() }
    Card(
        modifier = modifier
            .padding(top = 8.dp)
            .fillMaxWidth(0.75f)
            .height(IntrinsicSize.Min)
            .width(IntrinsicSize.Min),
        colors = CardDefaults.cardColors(Color.Transparent),
    ) {
        var overflowState by remember { mutableStateOf(false) }
        val clickableModifier = if (overflowState) Modifier.combinedClickable(
            onLongClick = onLongClick,
            onClick = onOverflowedClick
        ) else Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { onLongClick() })
        }
        Box(modifier = clickableModifier.height(twoLinesHeightDp)) {
            Text(
                text = description,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 5.dp),
                fontSize = 21.sp,
                lineHeight = 21.sp,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { overflowState = it.hasVisualOverflow },
                maxLines = 2,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun TaskSubDescription(
    text: String,
    tooltipText: String,
    enableTooltip: Boolean,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val twoLinesHeightDp = with(density) { ((14.sp).toPx() * 2.5f).toDp() }
    BoxWithConstraints(
        modifier = modifier.heightIn(max = twoLinesHeightDp),
        contentAlignment = Alignment.BottomStart
    ) {
        val measurer = rememberTextMeasurer()
        val raw = text.replace(
            Regex("(\\d{1,2}) (\\p{Alpha}+) (\\d{4})"), "$1\u00A0$2\u00A0$3"
        )
        val normalStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
        val smallStyle = normalStyle.copy(
            fontSize = 12.sp / 1.8f,
            lineHeight = 14.sp / 1.8f
        )
        val maxW = constraints.maxWidth
        val maxH = min(constraints.maxHeight, with(density) { twoLinesHeightDp.roundToPx() })
        val fitsNormal = remember(raw, maxW, maxH) {
            val result = measurer.measure(
                text = AnnotatedString(raw),
                style = normalStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = maxW, maxHeight = maxH)
            )
            !result.hasVisualOverflow
        }
        val finalText = if (fitsNormal) raw else raw.replace("\n", "")
        val finalStyle = if (fitsNormal) normalStyle else smallStyle
        TextTooltipBox(
            text = tooltipText,
            enable = enableTooltip
        ) {
            Text(
                text = finalText,
                style = finalStyle,
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 5.sp,
                    maxFontSize = finalStyle.fontSize
                ),
                overflow = TextOverflow.Visible,
                modifier = Modifier.padding(start = 5.dp, bottom = 3.dp)
            )
        }
    }
}

@Composable
private fun TaskCardBackground(
    color: Color,
    border: BorderStroke,
    shape: Shape,
    modifier: Modifier,
    content: @Composable (ColumnScope.() -> Unit),
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(color),
        border = border,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(bottom = 7.dp, top = 5.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}
