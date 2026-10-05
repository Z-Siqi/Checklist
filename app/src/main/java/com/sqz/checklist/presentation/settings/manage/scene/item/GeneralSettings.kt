package com.sqz.checklist.presentation.settings.manage.scene.item

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sqz.checklist.R
import com.sqz.checklist.presentation.settings.manage.SettingsManageScope
import com.sqz.checklist.presentation.settings.manage.SettingsManageViewModel
import com.sqz.checklist.presentation.settings.manage.component.ClickSetting
import com.sqz.checklist.presentation.settings.manage.component.DropdownSetting
import com.sqz.checklist.presentation.settings.manage.component.FlowChoiceSetting
import com.sqz.checklist.presentation.settings.manage.component.SettingText
import com.sqz.checklist.ui.common.dialog.EditableContentDialog
import com.sqz.checklist.ui.theme.Theme
import com.sqz.checklist.ui.theme.ThemePreference
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.model.settings.AppTheme

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.ThemeSetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
) {
    val options = listOf(
        stringResource(R.string.pastel_theme),
        stringResource(R.string.colorful_theme),
    )
    val selectedIndex = if (settings.appearance.theme == AppTheme.Colorful) 1 else 0
    FlowChoiceSetting(
        title = stringResource(R.string.theme),
        options = options,
        selectedIndex = selectedIndex,
        onSelected = { index ->
            ThemePreference.updatePreference(index)
            viewModel.setTheme(if (index == 1) AppTheme.Colorful else AppTheme.Pastel)
        },
    )
    Theme.SetSystemBarsColorByPreference()
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.PictureCompressionSetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
    view: View,
) {
    CompressionSetting(
        title = stringResource(R.string.picture_compression_rate),
        value = settings.media.pictureCompressionPercent,
        view = view,
        onValueChanged = viewModel::setPictureCompressionPercent,
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.VideoCompressionSetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
    view: View,
) {
    CompressionSetting(
        title = stringResource(R.string.video_compression_rate),
        value = settings.media.videoCompressionPercent,
        view = view,
        onValueChanged = viewModel::setVideoCompressionPercent,
    )
}

@Composable
private fun SettingsManageScope.CompressionSetting(
    title: String,
    value: Int,
    view: View,
    onValueChanged: (Int) -> Unit,
) {
    val values = remember { (100 downTo 10 step 10).toList() + 0 + -1 }
    val original = stringResource(R.string.original)
    val customLabel = stringResource(R.string.custom)
    val labels = remember(original, customLabel) {
        values.map { option ->
            when (option) {
                0 -> original
                -1 -> customLabel
                else -> option.toString()
            }
        }
    }
    var customDialogVisible by remember { mutableStateOf(false) }
    DropdownSetting(
        title = title,
        supportingText = stringResource(R.string.compression_rate_describe),
        selectedText = labels.getOrNull(values.indexOf(value)) ?: value.toString(),
        options = labels,
        onOptionSelected = { index ->
            val selectedValue = values[index]
            if (selectedValue == -1) customDialogVisible = true else onValueChanged(selectedValue)
        },
    )
    if (customDialogVisible) {
        val textState = rememberTextFieldState()
        val invalid by remember { derivedStateOf { textState.text.toString().toIntOrNull() !in 0..100 } }
        EditableContentDialog(
            onDismissRequest = { customDialogVisible = false },
            confirm = {
                textState.text.toString().toIntOrNull()?.let(onValueChanged)
                customDialogVisible = false
            },
            title = customLabel,
            confirmText = stringResource(R.string.confirm),
            state = textState,
            singleLine = true,
            numberOnly = true,
            disableConform = invalid,
            onDisableConformClick = {
                Toast.makeText(view.context, R.string.in_0_to_100_only, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.LanguageSetting(view: View) {
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val title = stringResource(R.string.language)
    ClickSetting(
        title = title,
        supportingText = stringResource(
            if (supported) R.string.click_select_language else R.string.unsupported_api_must_higher_than_32
        ),
        onClick = {
            if (supported) {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = Uri.fromParts("package", view.context.packageName, null)
                }
                view.context.startActivity(intent)
            } else {
                Toast.makeText(view.context, R.string.unsupported_below_api_33, Toast.LENGTH_SHORT).show()
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.language), contentDescription = title)
                SettingText(stringResource(R.string.app_display_language), supporting = true)
            }
        },
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsManageScope.AboutSetting(view: View) {
    val developerUrl = "https://github.com/Z-Siqi"
    ClickSetting(
        title = stringResource(R.string.about),
        onClick = { openUrl(view, developerUrl) },
        trailingContent = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = { openStore(view) }) {
                    Icon(
                        painterResource(R.drawable.google_play),
                        contentDescription = "Google Play",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { openUrl(view, "$developerUrl/Checklist") }) {
                    Icon(
                        painterResource(R.drawable.github_mark),
                        contentDescription = "Github",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        supportingText = stringResource(R.string.developed_by) + " Z-Siqi",
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.DonateSetting(view: View) {
    val title = stringResource(R.string.sponsor_me)
    ClickSetting(
        title = title,
        supportingText = stringResource(R.string.donate_describe),
        onClick = { openUrl(view, "https://github.com/sponsors/Z-Siqi") },
        trailingContent = {
            Icon(
                painterResource(R.drawable.heart),
                contentDescription = title,
                modifier = Modifier.padding(end = 4.dp),
            )
        },
    )
}

private fun openStore(view: View) {
    val packageName = view.context.packageName
    try {
        view.context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.vending")
        })
    } catch (_: ActivityNotFoundException) {
        openUrl(view, "https://play.google.com/store/apps/details?id=$packageName")
    }
}

private fun openUrl(view: View, url: String) {
    view.context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
