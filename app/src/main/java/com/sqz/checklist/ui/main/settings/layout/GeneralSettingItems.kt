package com.sqz.checklist.ui.main.settings.layout

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.material.dialog.EditableContentDialog
import com.sqz.checklist.ui.theme.ThemePreference
import com.sqz.checklist.ui.theme.unit.pxToDpInt

class GeneralSettingItems(private val view: View) : SettingsList() {

    @Composable
    private fun appTheme(preferences: PrimaryPreferences): SettingsItem {
        var selectedIndex by remember { mutableIntStateOf(preferences.appTheme()) }
        val themeList = listOf(
            stringResource(R.string.pastel_theme), stringResource(R.string.colorful_theme)
        )
        @Composable
        fun SegmentedButton(modifier: Modifier = Modifier) {
            selectedIndex = segmentedButton(
                list = themeList.toTypedArray(),
                label = { label ->
                    themeList.find { it == label } ?: "N/A"
                },
                initSetter = selectedIndex,
                modifier = modifier
            ).let { ThemePreference.updatePreference(preferences.appTheme(it)) }
        }
        return SettingsItem(SettingsType.General, stringResource(R.string.theme)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (LocalWindowInfo.current.containerSize.width.pxToDpInt() > 370) {
                    OptionText(it, Modifier.widthIn(max = 150.dp))
                    Spacer(Modifier.weight(1f))
                    SegmentedButton(Modifier.width(215.dp))
                } else Column(Modifier.fillMaxWidth()) {
                    OptionText(it)
                    SegmentedButton(Modifier.fillMaxWidth())
                }
            }
        }
    }

    private data class CompressionItem(val name: String, val value: Int)

    private val pictureAndVideoMenuList = listOf(
        CompressionItem("100", 100), CompressionItem("90", 90),
        CompressionItem("80", 80), CompressionItem("70", 70),
        CompressionItem("60", 60), CompressionItem("50", 50),
        CompressionItem("40", 40), CompressionItem("30", 30),
        CompressionItem("20", 20), CompressionItem("10", 10),
        CompressionItem(view.context.getString(R.string.original), 0),
        CompressionItem(view.context.getString(R.string.custom), -1),
    )

    @Composable
    private fun pictureCompressionRate(preferences: PrimaryPreferences): SettingsItem {
        var setting by remember { mutableIntStateOf(preferences.pictureCompressionRate()) }
        val list = pictureAndVideoMenuList
        var custom by remember { mutableStateOf(false) }
        if (custom) {
            val state = rememberTextFieldState()
            var isNumeric by remember { mutableStateOf(true) }
            LaunchedEffect(state.text.toString()) {
                isNumeric = try {
                    state.text.toString().toInt() !in 0..100
                } catch (e: NumberFormatException) {
                    true
                }
            }
            EditableContentDialog(
                onDismissRequest = { custom = false },
                confirm = {
                    setting = preferences.pictureCompressionRate(
                        state.text.toString().toInt()
                    )
                    custom = false
                },
                title = stringResource(R.string.custom),
                confirmText = stringResource(R.string.confirm),
                state = state,
                singleLine = true, numberOnly = true,
                disableConform = isNumeric,
                onDisableConformClick = {
                    Toast.makeText(
                        view.context, view.context.getString(R.string.in_0_to_100_only),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        return SettingsItem(
            SettingsType.General, stringResource(R.string.picture_compression_rate)
        ) { text ->
            val expanded = remember { mutableStateOf(false) }
            DropdownMenuView(
                expanded = expanded,
                title = text,
                menuShowText = list.find { it.value == setting }?.name ?: setting.toString(),
                dropdownMenuList = {
                    list.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            if (it.value != -1) {
                                setting = preferences.pictureCompressionRate(it.value)
                            } else custom = true
                            expanded.value = false
                        })
                    }
                },
                view = view,
                text = stringResource(R.string.compression_rate_describe)
            )
        }
    }

    @Composable
    private fun videoCompressionRate(preferences: PrimaryPreferences): SettingsItem {
        var setting by remember { mutableIntStateOf(preferences.videoCompressionRate()) }
        val list = pictureAndVideoMenuList
        var custom by remember { mutableStateOf(false) }
        if (custom) {
            val state = rememberTextFieldState()
            var isNumeric by remember { mutableStateOf(true) }
            LaunchedEffect(state.text.toString()) {
                isNumeric = try {
                    state.text.toString().toInt() !in 0..100
                } catch (e: NumberFormatException) {
                    true
                }
            }
            EditableContentDialog(
                onDismissRequest = { custom = false },
                confirm = {
                    setting = preferences.videoCompressionRate(
                        state.text.toString().toInt()
                    )
                    custom = false
                },
                title = stringResource(R.string.custom),
                confirmText = stringResource(R.string.confirm),
                state = state,
                singleLine = true, numberOnly = true,
                disableConform = isNumeric,
                onDisableConformClick = {
                    Toast.makeText(
                        view.context, view.context.getString(R.string.in_0_to_100_only),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        return SettingsItem(
            SettingsType.General, stringResource(R.string.video_compression_rate)
        ) { text ->
            val expanded = remember { mutableStateOf(false) }
            DropdownMenuView(
                expanded = expanded,
                title = text,
                menuShowText = list.find { it.value == setting }?.name ?: setting.toString(),
                dropdownMenuList = {
                    list.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            if (it.value != -1) {
                                setting = preferences.videoCompressionRate(it.value)
                            } else custom = true
                            expanded.value = false
                        })
                    }
                },
                view = view,
                text = stringResource(R.string.compression_rate_describe)
            )
        }
    }

    @Composable
    private fun language(): SettingsItem {
        val supportState = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        return SettingsItem(
            SettingsType.General, stringResource(R.string.language)
        ) {
            ClickView(
                title = it,
                onClick = {
                    if (supportState) {
                        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            data = Uri.fromParts("package", view.context.packageName, null)
                        }
                        view.context.startActivity(intent)
                    } else Toast.makeText(
                        view.context, view.context.getString(R.string.unsupported_below_api_33),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                view = view,
                icon = {
                    val padding = Modifier.padding(end = 12.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.language), it, padding)
                        OptionText(stringResource(R.string.app_display_language), padding, true)
                    }
                },
                text = if (supportState) stringResource(R.string.click_select_language) else stringResource(
                    R.string.unsupported_api_must_higher_than_32
                )
            )
        }
    }

    @Composable
    private fun about(): SettingsItem {
        return SettingsItem(
            SettingsType.General, stringResource(R.string.about)
        ) {
            val url = "https://github.com/Z-Siqi"
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    OptionText(it)
                    Row {
                        OptionText(
                            stringResource(R.string.developed_by), miniTitle = true
                        )
                        OptionText(
                            "Z-Siqi", url = url, view = view,
                            textColor = MaterialTheme.colorScheme.primary, miniTitle = true
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton({
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$url/Checklist")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    view.context.startActivity(intent)
                }) {
                    Icon(
                        painterResource(R.drawable.github_mark), "Github",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun donate(): SettingsItem {
        val url = "https://github.com/sponsors/Z-Siqi"
        return SettingsItem(
            SettingsType.General, stringResource(R.string.sponsor_me)
        ) {
            ClickView(
                title = it,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    view.context.startActivity(intent)
                },
                view = view,
                text = stringResource(R.string.donate_describe),
                icon = {
                    Icon(painterResource(R.drawable.heart), it, Modifier.padding(end = 12.dp))
                }
            )
        }
    }

    @Composable
    fun list(): List<SettingsItem> {
        val primaryPreferences = PrimaryPreferences(view.context)
        return listOf(
            this.appTheme(primaryPreferences),
            this.pictureCompressionRate(primaryPreferences),
            this.videoCompressionRate(primaryPreferences),
            this.language(),
            this.about(), this.donate()
        )
    }
}
