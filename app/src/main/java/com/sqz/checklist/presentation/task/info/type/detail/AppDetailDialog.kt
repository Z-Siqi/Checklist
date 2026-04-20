package com.sqz.checklist.presentation.task.info.type.detail

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.presentation.task.info.type.InfoDialogScaffold
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import sqz.checklist.common.EffectFeedback
import sqz.checklist.task.api.info.TaskInfo

/**
 * This method expected to be called only within this package and its sub-packages.
 *
 * @throws ClassCastException if the detail type is not [TaskInfo.DetailInfoState.DetailType.Application].
 */
@Composable
internal fun AppDetailDialog(
    detail: TaskInfo.DetailInfoState,
    onDismissRequest: () -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
    modifier: Modifier = Modifier,
    view: android.view.View = LocalView.current
) {
    val appType = detail.detailType as TaskInfo.DetailInfoState.DetailType.Application

    val packageName = appType.launchToken?.decodeToString()
    val packageManager = view.context.packageManager
    val appInfo = remember(packageName) {
        packageName?.let { _ ->
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                packageManager.getApplicationInfo(packageName, 0)
            }
        }
    }

    val requestClearFocus = remember { mutableStateOf(false) }
    InfoDialogScaffold(
        onDismissRequest = onDismissRequest,
        onDialogBackgroundClick = { requestClearFocus.value = true },
        isSmallScreenSize = isSmallScreenSize,
        modifier = modifier
    ) {
        InfoDetailDialogTitle(
            detailTitle = stringResource(R.string.application),
            detailDescription = detail.detailDescription
        )
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 18.dp))
        AppOpenCardScaffold(
            onOpenClick = {
                openExternalApp(packageName = packageName ?: "ERROR", context = view.context) {
                    Toast.makeText(
                        view.context,
                        R.string.failed_found_package,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                feedback.onClickEffect()
            },
            isAppExist = appInfo != null,
            requestClearFocus = requestClearFocus,
        ) {
            if (appInfo == null) {
                val notFoundText = stringResource(
                    R.string.package_not_found,
                    packageName ?: "N/A"
                )
                Text(text = notFoundText)
            } else {
                AppContent(
                    appPackageName = packageName ?: "ERROR",
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    appIcon = appInfo.loadIcon(packageManager),
                    isSmallScreenSize = isSmallScreenSize,
                )
            }
        }
        Spacer(modifier = Modifier.height(if (isSmallScreenSize) 10.dp else 20.dp))
        ThisDialogButton {
            onDismissRequest().also { feedback.onClickEffect() }
        }
    }
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

@Composable
private fun AppOpenCardScaffold(
    onOpenClick: () -> Unit,
    isAppExist: Boolean,
    requestClearFocus: MutableState<Boolean>,
    cardContentHeight: Dp = 130.dp,
    appInfoContent: @Composable () -> Unit
) {
    val shape = ShapeDefaults.Large
    val shapeConnect = ShapeDefaults.Small
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            SelectionContainer(
                modifier = scrollModifier then Modifier.padding(8.dp),
                content = {
                    val focus = LocalFocusManager.current
                    if (requestClearFocus.value) {
                        focus.clearFocus()
                        requestClearFocus.value = false
                    }
                    appInfoContent()
                }
            )
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = 45.dp),
            onClick = onOpenClick,
            shape = shape.copy(
                topStart = shapeConnect.topStart, topEnd = shapeConnect.topEnd,
            ),
            enabled = isAppExist
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

@Composable
private fun AppContent(
    appPackageName: String,
    appName: String,
    appIcon: Drawable,
    isSmallScreenSize: Boolean,
) {
    val iconPainter: Painter = remember {
        BitmapPainter(appIcon.toBitmap().asImageBitmap())
    }
    if (!isSmallScreenSize) Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = iconPainter,
            modifier = Modifier.size(48.dp),
            contentDescription = stringResource(R.string.app_icon),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = appName, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider()
            Text(
                text = appPackageName,
                style = MaterialTheme.typography.bodySmall
            )
        }
    } else Column {
        Image(
            painter = iconPainter,
            modifier = Modifier.size(48.dp),
            contentDescription = stringResource(R.string.app_icon),
        )
        Text(text = appName, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        Text(
            text = appPackageName,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ThisDialogButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier,
        ) {
            Text(
                text = stringResource(R.string.cancel),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview
@Composable
private fun AppDetailDialogPreview() {
    AppDetailDialog(
        detail = TaskInfo.DetailInfoState(
            detailDescription = "Test",
            detailType = TaskInfo.DetailInfoState.DetailType.Application(
                launchToken = "com.android.settings".encodeToByteArray()
            )
        ),
        onDismissRequest = {},
        isSmallScreenSize = false,
        feedback = AndroidEffectFeedback(LocalView.current),
    )
}
