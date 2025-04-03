package com.sqz.checklist.ui.material.dialog

import android.util.Log
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.sqz.checklist.R
import com.sqz.checklist.ui.material.getApp

@Composable
fun OpenExternalAppDialog(
    onDismissRequest: () -> Unit,
    packageName: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val view = LocalView.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val height = when {
        screenHeightDp >= 700 -> (screenHeightDp / 6.8).toInt()
        screenHeightDp < (LocalConfiguration.current.screenWidthDp / 1.2) -> (screenHeightDp / 4.2).toInt()
        screenHeightDp <= 458 -> (screenHeightDp / 1.05).toInt()
        else -> (screenHeightDp / 6.1).toInt()
    }
    AlertDialog(
        modifier = modifier
            .width((LocalConfiguration.current.screenWidthDp / 1.2).dp)
            .sizeIn(maxWidth = 560.dp),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDismissRequest()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        text = {
            val focus = LocalFocusManager.current
            val getApp = getApp(packageName, view.context)
            OutlinedCard(
                modifier = modifier.fillMaxWidth() then modifier
                    .height(height.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { focus.clearFocus() }
                    },
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(if (getApp == null) modifier else modifier
                    .fillMaxSize()
                    .clickable {
                        try {
                            val intent = view.context.packageManager
                                .getLaunchIntentForPackage(packageName)
                            view.context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                view.context,
                                view.context.getString(R.string.failed_found_package),
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.w(
                                "PackageName", "Failed to found saved package name! ERROR: $e"
                            )
                        }
                    }) {
                    Row(modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (getApp == null) Text(
                            stringResource(R.string.package_not_found, packageName)
                        ) else {
                            val iconPainter: Painter =
                                remember { BitmapPainter(getApp.icon.toBitmap().asImageBitmap()) }
                            Image(
                                painter = iconPainter,
                                modifier = modifier.size(48.dp),
                                contentDescription = stringResource(R.string.app_icon),
                            )
                            Spacer(modifier = modifier.width(10.dp))
                            Column {
                                Text(text = getApp.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = getApp.packageName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        title = { if (title != null) Text(text = title) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Preview
@Composable
private fun OpenExternalAppDialogPreview() {
    OpenExternalAppDialog({}, "com.test.app")
}
