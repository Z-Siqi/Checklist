package com.sqz.checklist.presentation.history.task.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import kotlinx.datetime.LocalDate
import sqz.checklist.common.KmpLocalDatePatternFormatter
import sqz.checklist.common.TimestampHelper

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun TaskHistoryCard(
    historyLong: Long,
    taskDescription: String,
    createDate: LocalDate,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "border_color"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(200),
        label = "container_color"
    )

    val hasFinishedTime = historyLong > TimestampHelper.TWENTY_FIRST_CENTURY
    val finishLocalDate = if (hasFinishedTime) {
        TimestampHelper.toLocalDate(historyLong)
    } else {
        null
    }

    OutlinedCard(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shape = ShapeDefaults.ExtraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Task description + Completed expressive badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TaskDescription(
                    description = taskDescription,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Expressive completed indicator badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.check),
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Subtle divider for clearer visual hierarchy
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.8.dp
                )

                // Footer: Grouped metadata chips with early-wrap algorithm
                HistoryDateChipsLayout(
                    horizontalGap = 6.dp,
                    verticalGap = 4.dp,
                    wrapThresholdRatio = 0.832f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Creation date meta chip
                    DateMetaChip(
                        iconRes = R.drawable.schedule,
                        text = getTaskCreateDateText(localDate = createDate),
                        highlight = false,
                        isSelected = isSelected
                    )

                    if (finishLocalDate != null) {
                        // Finish date meta chip
                        DateMetaChip(
                            iconRes = null,
                            text = getTaskHistoryDateText(localDate = finishLocalDate),
                            highlight = true,
                            isSelected = isSelected
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom layout that measures chip items and triggers wrap early if the combined
 * width of the chips plus gap exceeds [wrapThresholdRatio] of the available width.
 */
@Composable
private fun HistoryDateChipsLayout(
    horizontalGap: Dp,
    verticalGap: Dp,
    wrapThresholdRatio: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalGapPx = horizontalGap.roundToPx()
        val verticalGapPx = verticalGap.roundToPx()
        val maxWidth = constraints.maxWidth

        // Measure children with unbounded width to get intrinsic size
        val childConstraints = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val placeables = measurables.map { it.measure(childConstraints) }

        if (placeables.isEmpty()) {
            return@Layout layout(maxWidth, 0) {}
        }

        if (placeables.size == 1) {
            val single = placeables.first()
            return@Layout layout(maxWidth, single.height) {
                single.placeRelative(0, 0)
            }
        }

        // Calculate combined width of all chips in a single row (including gap)
        val totalSingleLineWidth = placeables.sumOf { it.width } + (placeables.size - 1) * horizontalGapPx
        val thresholdWidth = (maxWidth * wrapThresholdRatio).toInt()

        val shouldWrap = totalSingleLineWidth > thresholdWidth

        if (!shouldWrap) {
            // Single row layout
            val rowHeight = placeables.maxOf { it.height }
            layout(maxWidth, rowHeight) {
                var currentX = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(currentX, 0)
                    currentX += placeable.width + horizontalGapPx
                }
            }
        } else {
            // Wrapped (multi-row) layout
            val rows = mutableListOf<List<Placeable>>()
            var currentRow = mutableListOf<Placeable>()
            var currentRowWidth = 0

            placeables.forEach { placeable ->
                val neededWidth = if (currentRow.isEmpty()) placeable.width else currentRowWidth + horizontalGapPx + placeable.width
                if (currentRow.isNotEmpty() && neededWidth > thresholdWidth) {
                    rows.add(currentRow)
                    currentRow = mutableListOf(placeable)
                    currentRowWidth = placeable.width
                } else {
                    currentRow.add(placeable)
                    currentRowWidth = neededWidth
                }
            }
            if (currentRow.isNotEmpty()) {
                rows.add(currentRow)
            }

            val totalHeight = rows.sumOf { r -> r.maxOf { it.height } } + (rows.size - 1) * verticalGapPx
            layout(maxWidth, totalHeight) {
                var currentY = 0
                rows.forEach { row ->
                    val rowHeight = row.maxOf { it.height }
                    var currentX = 0
                    row.forEach { placeable ->
                        placeable.placeRelative(currentX, currentY)
                        currentX += placeable.width + horizontalGapPx
                    }
                    currentY += rowHeight + verticalGapPx
                }
            }
        }
    }
}

@Composable
private fun DateMetaChip(
    iconRes: Int?,
    text: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    isSelected: Boolean = false,
) {
    val chipBgColor = if (highlight) {
        if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        }
    } else {
        if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
        }
    }

    val chipContentColor = if (highlight) {
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = ShapeDefaults.Small,
        color = chipBgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = chipContentColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            val textStyle = MaterialTheme.typography.labelSmall
            Text(
                text = text,
                style = textStyle,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                color = chipContentColor,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 7.sp,
                    maxFontSize = textStyle.fontSize,
                    stepSize = 0.5.sp
                ),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TaskDescription(
    description: String,
    modifier: Modifier = Modifier
) = Box(modifier = modifier) {
    Text(
        text = description,
        fontSize = 19.sp,
        lineHeight = 23.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
@ReadOnlyComposable
private fun getTaskCreateDateText(localDate: LocalDate): String {
    val dateFormat = KmpLocalDatePatternFormatter.format(
        localDate, stringResource(R.string.task_date_format),
    )
    return stringResource(R.string.task_creation_time, dateFormat)
}

@Composable
@ReadOnlyComposable
private fun getTaskHistoryDateText(localDate: LocalDate): String {
    val dateFormat = KmpLocalDatePatternFormatter.format(
        localDate, stringResource(R.string.task_date_format),
    )
    return stringResource(R.string.task_finish_time, dateFormat)
}

