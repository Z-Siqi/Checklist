package com.sqz.checklist.ui.common.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.sqz.checklist.ui.common.unit.isLandscape
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import com.sqz.checklist.ui.theme.UISizeLimit
import com.sqz.checklist.ui.theme.smallInLargestEdgeSize
import com.sqz.checklist.ui.theme.smallInSmallestEdgeSize

/**
 * A primary dialog composable that adapts its layout based on the screen size.
 *
 * On larger screens, it displays a standard [AlertDialog].
 * On smaller screens, it switches to a full-screen [BasicAlertDialog] to improve usability
 * by arranging the content, title, icon, and buttons in a column.
 *
 * The screen size threshold is determined by `normalInLargestEdgeSize` and `normalInSmallestEdgeSize`
 * and considers the device's orientation (landscape or portrait).
 *
 * @see AlertDialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryDialog(
    onDismissRequest: () -> Unit,
    actionButton: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    contentScrollModifier: Modifier = Modifier.let {
        val scrollState = rememberScrollState()
        it.verticalColumnScrollbar(
            scrollState = scrollState, endPadding = 0f, scrollBarCornerRadius = 12f,
            scrollBarTrackColor = MaterialTheme.colorScheme.outlineVariant,
            scrollBarColor = MaterialTheme.colorScheme.outline,
            showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
        ) then it.verticalScroll(scrollState)
    },
    properties: DialogProperties = DialogProperties(),
) {
    val containerSizePx = LocalWindowInfo.current.containerSize
    val widthDp: Int = containerSizePx.width.pxToDpInt()
    val heightDp: Int = containerSizePx.height.pxToDpInt()

    val isSmallScreenSize: Boolean = if (isLandscape()) {
        val heightRequired = heightDp < smallInSmallestEdgeSize
        heightRequired
    } else {
        val widthRequired = widthDp < smallInSmallestEdgeSize
        val heightRequired = heightDp < smallInLargestEdgeSize
        widthRequired && heightRequired
    }
    if (isSmallScreenSize) {
        val contentFullScreen: @Composable (() -> Unit) = {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                if (icon != null || title != null) Row(
                    modifier = Modifier
                        .padding(bottom = 15.dp),
                    content = {
                        if (icon != null) icon()
                        if (title != null) title()
                    }
                )
                if (content != null) Column(
                    modifier = Modifier.weight(1f) then contentScrollModifier
                ) { content() }
                Row(
                    modifier = Modifier
                        .weight(1f, false)
                        .padding(top = 15.dp),
                    verticalAlignment = Alignment.Bottom,
                    content = {
                        if (dismissButton != null) dismissButton()
                        actionButton()
                    }
                )
            }
        }
        BasicAlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .fillMaxSize()
                .background(color = containerColor, shape = ShapeDefaults.ExtraSmall),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            content = { UISizeLimit(content = contentFullScreen) }
        )
    } else {
        val confirmButton: @Composable (() -> Unit) = {
            UISizeLimit {
                Row(horizontalArrangement = Arrangement.End, content = actionButton)
            }
        }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = if (dismissButton != null) {
                { UISizeLimit(content = dismissButton) }
            } else null,
            icon = if (icon != null) {
                { UISizeLimit(content = icon) }
            } else null,
            title = if (title != null) {
                { UISizeLimit(content = title) }
            } else null,
            text = if (content != null) {
                {
                    BoxWithConstraints {
                        val textModifier = Modifier.let {
                            if (maxHeight < 85.dp) it then contentScrollModifier else it
                        }
                        Column(modifier = textModifier.requiredHeightIn(min = 50.dp)) {
                            UISizeLimit(content = content)
                        }
                    }
                }
            } else null,
            shape = shape,
            containerColor = containerColor,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
            tonalElevation = tonalElevation,
            properties = properties
        )
    }
}
