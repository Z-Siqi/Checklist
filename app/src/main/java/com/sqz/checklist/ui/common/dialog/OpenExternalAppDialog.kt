package com.sqz.checklist.ui.common.dialog

import android.content.Context
import android.util.Log
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.AppInfo
import com.sqz.checklist.ui.common.getApp
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.verticalColumnScrollbar

/**
 * A composable dialog that prompts the user to open an external application.
 * It displays information about the app and provides a button to launch it.
 *
 * @param onDismissRequest Callback invoked when the dialog is dismissed.
 * @param packageName The package name of the external application to open.
 * @param modifier Modifier for this composable.
 * @param title Optional title for the dialog.
 */
@Composable
fun OpenExternalAppDialog(
    onDismissRequest: () -> Unit,
    packageName: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val view = LocalView.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenHeightDp = containerSize.height.pxToDpInt()
    val heightCalculate = when {
        screenHeightDp >= 700 -> (screenHeightDp / 6.8).toInt()
        screenHeightDp < (containerSize.width.pxToDpInt() / 1.2) -> (screenHeightDp / 4.2).toInt()
        screenHeightDp <= 458 -> (screenHeightDp / 1.8).toInt()
        else -> (screenHeightDp / 6.1).toInt()
    }
    val height: Dp = heightCalculate.let {
        if (it > 130) it.dp else 130.dp
    }
    PrimaryDialog(
        modifier = modifier
            .width((containerSize.width.pxToDpInt() / 1.2).dp)
            .sizeIn(maxWidth = 560.dp),
        onDismissRequest = onDismissRequest,
        actionButton = {
            TextButton(onClick = {
                onDismissRequest()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        content = {
            val focus = LocalFocusManager.current
            val getApp = getApp(packageName, view.context)
            ExternalApp(
                packageName = packageName,
                appInfo = getApp,
                modifier = Modifier
                    .heightIn(min = height)
                    .pointerInput(Unit) {
                        detectTapGestures { focus.clearFocus() }
                    },
                cardContentHeight = height,
                onClick = {
                    openExternalApp(packageName = packageName, context = view.context) {
                        Toast.makeText(
                            view.context,
                            view.context.getString(R.string.failed_found_package),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    focus.clearFocus()
                }
            )
        },
        title = if (title != null) {
            { Text(text = title) }
        } else null,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

/**
 * Internal composable that displays the app information card and the open button.
 * It adapts its layout based on the available width.
 *
 * @param packageName The package name of the app.
 * @param appInfo An [AppInfo] object containing the app's details, or null if not found.
 * @param modifier Modifier for this composable.
 * @param cardContentHeight The calculated height for the card's content area.
 * @param onClick Callback invoked when the "Open" button is clicked.
 */
@Composable
private fun ExternalApp(
    packageName: String,
    appInfo: AppInfo?,
    modifier: Modifier = Modifier,
    cardContentHeight: Dp = 130.dp,
    onClick: () -> Unit
) = BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
    val maxW = maxWidth
    val isEnoughWidth = maxWidth > 220.dp
    val shape = ShapeDefaults.Large
    val shapeConnect = ShapeDefaults.Small
    Column {
        val cardDefHeight = (89.64).dp
        Card(
            modifier = Modifier
                .heightIn(min = cardDefHeight)
                .fillMaxWidth(),
            shape = shape.copy(
                bottomStart = shapeConnect.bottomStart,
                bottomEnd = shapeConnect.bottomEnd,
            ),
            colors = CardDefaults.cardColors()
        ) {
            val scrollHeight = (cardContentHeight - 45.dp).let {
                if (it > cardDefHeight) it else cardDefHeight
            }
            val scrollState = rememberScrollState()
            val scrollModifier = Modifier
                .heightIn(max = scrollHeight)
                .verticalColumnScrollbar(
                    scrollState = scrollState, endPadding = 24f, topBottomPadding = 50f,
                    scrollBarCornerRadius = 12f,
                    scrollBarTrackColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                        0.8f
                    ), scrollBarColor = MaterialTheme.colorScheme.secondary.copy(0.7f),
                    showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
                )
                .verticalScroll(scrollState)
            SelectionContainer(modifier = scrollModifier then Modifier.padding(8.dp)) {
                if (appInfo == null) Text(
                    text = stringResource(R.string.package_not_found, packageName)
                ) else {
                    if (isEnoughWidth) Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(appInfo)
                        Spacer(modifier = Modifier.width(10.dp))
                        AppDescriptionColumn(appInfo)
                    } else Column {
                        AppIcon(appInfo)
                        AppDescriptionColumn(appInfo)
                    }
                }
            }
        }
        Button(
            modifier = Modifier
                .width(maxW)
                .requiredHeightIn(min = 45.dp),
            onClick = onClick,
            shape = shape.copy(
                topStart = shapeConnect.topStart,
                topEnd = shapeConnect.topEnd,
            ),
            enabled = appInfo != null
        ) {
            Text(
                text = stringResource(R.string.open),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 5.sp, maxFontSize = LocalTextStyle.current.fontSize
                ),
                modifier = Modifier,
                maxLines = 1,
            )
        }
    }
}

/**
 * Displays a column with the application's name and package name, separated by a divider.
 *
 * @param appInfo The [AppInfo] object containing the app details.
 */
@Composable
private fun AppDescriptionColumn(appInfo: AppInfo) = SelectionContainer {
    Column {
        Text(text = appInfo.name, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        Text(
            text = appInfo.packageName,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Displays the application's icon.
 *
 * @param appInfo The [AppInfo] object containing the app icon.
 */
@Composable
private fun AppIcon(appInfo: AppInfo) {
    val iconPainter: Painter =
        remember { BitmapPainter(appInfo.icon.toBitmap().asImageBitmap()) }
    Image(
        painter = iconPainter,
        modifier = Modifier.size(48.dp),
        contentDescription = stringResource(R.string.app_icon),
    )
}

/**
 * Attempts to launch an external application using its package name.
 *
 * @param packageName The package name of the app to launch.
 * @param context The Android [Context].
 * @param onFailed Callback invoked if launching the app fails (e.g., app not installed).
 */
private fun openExternalApp(packageName: String, context: Context, onFailed: () -> Unit) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        context.startActivity(intent)
    } catch (e: Exception) {
        onFailed()
        Log.w("openExternalApp", "Failed to open by saved package name! ERROR: $e")
    }
}

/**
 * A preview for the [OpenExternalAppDialog] composable.
 */
@Preview
@Composable
private fun OpenExternalAppDialogPreview() {
    OpenExternalAppDialog({}, "com.test.app")
}
