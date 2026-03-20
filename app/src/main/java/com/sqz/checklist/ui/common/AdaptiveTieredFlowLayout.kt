package com.sqz.checklist.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

enum class TieredFlowAlignment {
    Start,
    End,
    Center
}

enum class MergedTieredArrangement {
    Start,
    End,
    Center,
    SpaceBetween
}

private enum class AdaptiveTieredSlot {
    TopMeasure,
    BottomMeasure,
    TopSplit,
    BottomSplit,
    TopMerged,
    BottomMerged,

    TopItemsMeasure,
    BottomItemsMeasure
}

private data class PackedLine(
    val items: List<Placeable>,
    val width: Int,
    val height: Int
)

private data class PackedLinesResult(
    val lines: List<PackedLine>,
    val totalHeight: Int
)

/**
 * A layout that arranges two sections of content (`topContent` and `bottomContent`) using
 * adaptive behavior depending on the available width.
 *
 * Layout behavior:
 *
 * 1. Merged mode:
 *    When the total width of `topContent` and `bottomContent` fits within the available
 *    width (and [mergeWhenPossible] is true), both sections are placed on the same row.
 *
 * 2. Split mode:
 *    When there is not enough horizontal space, the layout falls back to two rows:
 *    - `topContent` on the first section
 *    - `bottomContent` on the second section
 *
 * 3. Optional insert mode in split layout:
 *    When [insertBottomBeforeTopWrap] is enabled and `topContent` wraps to a new line,
 *    the layout first tries to place part of the wrapped `topContent` into the free space
 *    on the left side of the row where `bottomContent` is placed.
 *
 *    The inserted top items are always placed from the far left.
 *    The original `bottomContent` keeps its own [bottomAlignment].
 *
 *    If `bottomContent` would need to wrap, or if no top item fits into that left-side
 *    free space, the layout falls back to the original split behavior.
 */
@Composable
fun AdaptiveTieredFlowLayout(
    modifier: Modifier = Modifier,
    mergeWhenPossible: Boolean = true,
    insertBottomBeforeTopWrap: Boolean = false,
    topAlignment: TieredFlowAlignment = TieredFlowAlignment.Start,
    bottomAlignment: TieredFlowAlignment = TieredFlowAlignment.Start,
    mergedArrangement: MergedTieredArrangement = MergedTieredArrangement.SpaceBetween,
    horizontalGap: Dp = 8.dp,
    verticalGap: Dp = 8.dp,
    sectionGap: Dp = 8.dp,
    mergedSectionGap: Dp = 12.dp,
    topContent: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val horizontalGapPx = horizontalGap.roundToPx()
        val verticalGapPx = verticalGap.roundToPx()
        val sectionGapPx = sectionGap.roundToPx()
        val mergedSectionGapPx = mergedSectionGap.roundToPx()

        fun measureSingleLineWidth(
            slot: AdaptiveTieredSlot,
            content: @Composable () -> Unit
        ) = subcompose(slot) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(horizontalGap)
            ) {
                content()
            }
        }.first().measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth = Int.MAX_VALUE
            )
        )

        val topMeasure = measureSingleLineWidth(
            AdaptiveTieredSlot.TopMeasure,
            topContent
        )
        val bottomMeasure = measureSingleLineWidth(
            AdaptiveTieredSlot.BottomMeasure,
            bottomContent
        )

        val hasTop = topMeasure.width > 0 && topMeasure.height > 0

        val canMerge = mergeWhenPossible && (
                !hasTop || topMeasure.width + mergedSectionGapPx + bottomMeasure.width <= constraints.maxWidth
                )

        if (canMerge) {
            val topPlaceable = if (hasTop) {
                subcompose(AdaptiveTieredSlot.TopMerged) {
                    FlowRow(
                        horizontalArrangement = topAlignment.toArrangement(horizontalGap),
                        verticalArrangement = Arrangement.spacedBy(verticalGap)
                    ) {
                        topContent()
                    }
                }.first().measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxWidth = topMeasure.width
                    )
                )
            } else {
                null
            }

            val bottomPlaceable = subcompose(AdaptiveTieredSlot.BottomMerged) {
                FlowRow(
                    horizontalArrangement = bottomAlignment.toArrangement(horizontalGap),
                    verticalArrangement = Arrangement.spacedBy(verticalGap)
                ) {
                    bottomContent()
                }
            }.first().measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = bottomMeasure.width
                )
            )

            val topWidth = topPlaceable?.width ?: 0
            val topHeight = topPlaceable?.height ?: 0
            val bottomWidth = bottomPlaceable.width
            val bottomHeight = bottomPlaceable.height

            val layoutWidth = constraints.maxWidth
            val layoutHeight = max(topHeight, bottomHeight)

            layout(layoutWidth, layoutHeight) {
                if (!hasTop) {
                    val bottomX = when (mergedArrangement) {
                        MergedTieredArrangement.Start -> 0
                        MergedTieredArrangement.End -> layoutWidth - bottomWidth
                        MergedTieredArrangement.Center -> (layoutWidth - bottomWidth) / 2
                        MergedTieredArrangement.SpaceBetween -> 0
                    }
                    bottomPlaceable.placeRelative(bottomX, 0)
                    return@layout
                }

                val totalWidth = topWidth + mergedSectionGapPx + bottomWidth

                val (topX, bottomX) = when (mergedArrangement) {
                    MergedTieredArrangement.Start -> {
                        0 to (topWidth + mergedSectionGapPx)
                    }

                    MergedTieredArrangement.End -> {
                        val startX = layoutWidth - totalWidth
                        startX to (startX + topWidth + mergedSectionGapPx)
                    }

                    MergedTieredArrangement.Center -> {
                        val startX = (layoutWidth - totalWidth) / 2
                        startX to (startX + topWidth + mergedSectionGapPx)
                    }

                    MergedTieredArrangement.SpaceBetween -> {
                        0 to (layoutWidth - bottomWidth)
                    }
                }

                topPlaceable!!.placeRelative(topX, 0)
                bottomPlaceable.placeRelative(bottomX, 0)
            }
        } else {
            if (insertBottomBeforeTopWrap && hasTop) {
                val childConstraints = constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = constraints.maxWidth
                )

                val topItems = subcompose(AdaptiveTieredSlot.TopItemsMeasure) {
                    topContent()
                }.map { it.measure(childConstraints) }

                val bottomItems = subcompose(AdaptiveTieredSlot.BottomItemsMeasure) {
                    bottomContent()
                }.map { it.measure(childConstraints) }

                val topPacked = packIntoLines(
                    placeable = topItems,
                    maxWidth = constraints.maxWidth,
                    horizontalGapPx = horizontalGapPx,
                    verticalGapPx = verticalGapPx
                )

                val bottomSingleLine = packSingleLineOrNull(
                    placeable = bottomItems,
                    maxWidth = constraints.maxWidth,
                    horizontalGapPx = horizontalGapPx
                )

                if (topPacked.lines.size > 1 && bottomSingleLine != null) {
                    val layoutWidth = constraints.maxWidth
                    val firstTopLine = topPacked.lines.first()

                    val remainingTopItems = topPacked.lines
                        .drop(1)
                        .flatMap { it.items }

                    val bottomX = when (bottomAlignment) {
                        TieredFlowAlignment.Start -> 0
                        TieredFlowAlignment.End -> layoutWidth - bottomSingleLine.width
                        TieredFlowAlignment.Center -> (layoutWidth - bottomSingleLine.width) / 2
                    }.coerceAtLeast(0)

                    val insertedTopLine = takeFittingLine(
                        placeable = remainingTopItems,
                        maxWidth = bottomX,
                        horizontalGapPx = horizontalGapPx
                    )

                    if (insertedTopLine.items.isNotEmpty()) {
                        val restTopItems = remainingTopItems.drop(insertedTopLine.items.size)

                        val remainingPacked = packIntoLines(
                            placeable = restTopItems,
                            maxWidth = layoutWidth,
                            horizontalGapPx = horizontalGapPx,
                            verticalGapPx = verticalGapPx
                        )

                        val middleRowHeight = max(insertedTopLine.height, bottomSingleLine.height)

                        val remainingHeight = remainingPacked.totalHeight

                        val layoutHeight =
                            firstTopLine.height +
                                    sectionGapPx +
                                    middleRowHeight +
                                    (if (remainingPacked.lines.isNotEmpty()) sectionGapPx else 0) +
                                    remainingHeight

                        return@SubcomposeLayout layout(layoutWidth, layoutHeight) {
                            var currentY = 0

                            placeLine(
                                line = firstTopLine,
                                containerWidth = layoutWidth,
                                alignment = topAlignment,
                                horizontalGapPx = horizontalGapPx,
                                y = currentY
                            )

                            currentY += firstTopLine.height + sectionGapPx

                            placeLine(
                                line = insertedTopLine,
                                containerWidth = insertedTopLine.width,
                                alignment = TieredFlowAlignment.Start,
                                horizontalGapPx = horizontalGapPx,
                                y = currentY
                            )

                            placeAbsoluteLineAtX(
                                line = bottomSingleLine,
                                x = bottomX,
                                horizontalGapPx = horizontalGapPx,
                                y = currentY
                            )

                            currentY += middleRowHeight

                            if (remainingPacked.lines.isNotEmpty()) {
                                currentY += sectionGapPx

                                remainingPacked.lines.forEachIndexed { index, line ->
                                    placeLine(
                                        line = line,
                                        containerWidth = layoutWidth,
                                        alignment = topAlignment,
                                        horizontalGapPx = horizontalGapPx,
                                        y = currentY
                                    )
                                    currentY += line.height
                                    if (index != remainingPacked.lines.lastIndex) {
                                        currentY += verticalGapPx
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val topPlaceable = subcompose(AdaptiveTieredSlot.TopSplit) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = topAlignment.toArrangement(horizontalGap),
                    verticalArrangement = Arrangement.spacedBy(verticalGap)
                ) {
                    topContent()
                }
            }.first().measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )

            val bottomPlaceable = subcompose(AdaptiveTieredSlot.BottomSplit) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = bottomAlignment.toArrangement(horizontalGap),
                    verticalArrangement = Arrangement.spacedBy(verticalGap)
                ) {
                    bottomContent()
                }
            }.first().measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )

            val layoutWidth = constraints.maxWidth
            val layoutHeight = topPlaceable.height + sectionGapPx + bottomPlaceable.height

            layout(layoutWidth, layoutHeight) {
                topPlaceable.placeRelative(0, 0)
                bottomPlaceable.placeRelative(0, topPlaceable.height + sectionGapPx)
            }
        }
    }
}

private fun TieredFlowAlignment.toArrangement(space: Dp): Arrangement.Horizontal {
    return when (this) {
        TieredFlowAlignment.Start ->
            Arrangement.spacedBy(space, Alignment.Start)

        TieredFlowAlignment.End ->
            Arrangement.spacedBy(space, Alignment.End)

        TieredFlowAlignment.Center ->
            Arrangement.spacedBy(space, Alignment.CenterHorizontally)
    }
}

private fun packIntoLines(
    placeable: List<Placeable>,
    maxWidth: Int,
    horizontalGapPx: Int,
    verticalGapPx: Int,
): PackedLinesResult {
    if (placeable.isEmpty()) {
        return PackedLinesResult(
            lines = emptyList(),
            totalHeight = 0
        )
    }

    val lines = mutableListOf<PackedLine>()

    var currentItems = mutableListOf<Placeable>()
    var currentWidth = 0
    var currentHeight = 0

    fun commitLine() {
        if (currentItems.isEmpty()) return
        lines += PackedLine(
            items = currentItems.toList(),
            width = currentWidth,
            height = currentHeight
        )
        currentItems = mutableListOf()
        currentWidth = 0
        currentHeight = 0
    }

    placeable.forEach { placeable ->
        val proposedWidth = if (currentItems.isEmpty()) {
            placeable.width
        } else {
            currentWidth + horizontalGapPx + placeable.width
        }

        if (currentItems.isNotEmpty() && proposedWidth > maxWidth) {
            commitLine()
        }

        currentWidth = if (currentItems.isEmpty()) {
            placeable.width
        } else {
            currentWidth + horizontalGapPx + placeable.width
        }
        currentHeight = max(currentHeight, placeable.height)
        currentItems += placeable
    }

    commitLine()

    val totalHeight = lines.sumOf { it.height } +
            verticalGapPx * (lines.size - 1).coerceAtLeast(0)

    return PackedLinesResult(
        lines = lines,
        totalHeight = totalHeight
    )
}

private fun packSingleLineOrNull(
    placeable: List<Placeable>,
    maxWidth: Int,
    horizontalGapPx: Int,
): PackedLine? {
    if (placeable.isEmpty()) {
        return PackedLine(
            items = emptyList(),
            width = 0,
            height = 0
        )
    }

    var width = 0
    var height = 0

    placeable.forEachIndexed { index, placeable ->
        width += placeable.width
        if (index > 0) width += horizontalGapPx
        height = max(height, placeable.height)
    }

    return if (width <= maxWidth) {
        PackedLine(
            items = placeable,
            width = width,
            height = height
        )
    } else {
        null
    }
}

private fun takeFittingLine(
    placeable: List<Placeable>,
    maxWidth: Int,
    horizontalGapPx: Int,
): PackedLine {
    if (placeable.isEmpty() || maxWidth <= 0) {
        return PackedLine(
            items = emptyList(),
            width = 0,
            height = 0
        )
    }

    val accepted = mutableListOf<Placeable>()
    var width = 0
    var height = 0

    for (placeable in placeable) {
        val proposedWidth = if (accepted.isEmpty()) {
            placeable.width
        } else {
            width + horizontalGapPx + placeable.width
        }

        if (proposedWidth > maxWidth) break

        width = proposedWidth
        height = max(height, placeable.height)
        accepted += placeable
    }

    return PackedLine(
        items = accepted,
        width = width,
        height = height
    )
}

private fun Placeable.PlacementScope.placeLine(
    line: PackedLine,
    containerWidth: Int,
    alignment: TieredFlowAlignment,
    horizontalGapPx: Int,
    y: Int,
) {
    val startX = when (alignment) {
        TieredFlowAlignment.Start -> 0
        TieredFlowAlignment.End -> containerWidth - line.width
        TieredFlowAlignment.Center -> (containerWidth - line.width) / 2
    }.coerceAtLeast(0)

    var x = startX
    line.items.forEach { placeable ->
        placeable.placeRelative(x, y)
        x += placeable.width + horizontalGapPx
    }
}

private fun Placeable.PlacementScope.placeAbsoluteLineAtX(
    line: PackedLine,
    x: Int,
    horizontalGapPx: Int,
    y: Int,
) {
    var currentX = x
    line.items.forEach { placeable ->
        placeable.placeRelative(currentX, y)
        currentX += placeable.width + horizontalGapPx
    }
}
