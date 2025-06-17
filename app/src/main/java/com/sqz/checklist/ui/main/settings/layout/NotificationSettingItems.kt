package com.sqz.checklist.ui.main.settings.layout

import android.content.Intent
import android.provider.Settings
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.material.dialog.EditableContentDialog
import com.sqz.checklist.ui.theme.unit.timeDisplay
import java.util.Locale

class NotificationSettingItems(private val view: View) : SettingsList() {

    @Composable
    private fun disableRemoveNotifyInReminded(preferences: PrimaryPreferences): SettingsItem {
        var setting by remember { mutableStateOf(preferences.disableRemoveNotifyInReminded()) }
        return SettingsItem(
            SettingsType.Notification,
            stringResource(R.string.disable_remove_notify_in_reminded)
        ) { text ->
            super.SwitchView(
                text = text,
                checked = setting,
                onCheckedChange = { setting = preferences.disableRemoveNotifyInReminded(it) },
                view = view
            )
        }
    }

    @Composable
    private fun disableNoScheduleExactAlarmNotice(preferences: PrimaryPreferences): SettingsItem {
        var setting by remember { mutableStateOf(preferences.disableNoScheduleExactAlarmNotice()) }
        return SettingsItem(
            SettingsType.Notification,
            stringResource(R.string.disable_no_schedule_exact_alarm_notice)
        ) { text ->
            super.SwitchView(
                text = text,
                checked = setting,
                onCheckedChange = { setting = preferences.disableNoScheduleExactAlarmNotice(it) },
                view = view
            )
        }
    }

    @Composable
    private fun recentlyRemindedKeepTime(preferences: PrimaryPreferences): SettingsItem {
        data class TimeItem(val name: String, val value: Long)
        var setting by remember { mutableLongStateOf(preferences.recentlyRemindedKeepTime()) }
        var custom by remember { mutableStateOf(false) }
        val list = listOf(
            TimeItem(timeDisplay(10800L), 10800000L),
            TimeItem(timeDisplay(18000L), 18000000L),
            TimeItem(timeDisplay(43200L), 43200000L),
            TimeItem(timeDisplay(86400L), 86400000L),
            TimeItem(timeDisplay(604800L), 604800000L),
            TimeItem(stringResource(R.string.disable), 0L),
            TimeItem(stringResource(R.string.custom), -1L)
        )

        fun String.formatTime() = this.replace("1", "").replace(" ", "").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }
        if (custom) {
            val segmentedButton = listOf(
                timeDisplay(60L).formatTime(), // Minute
                timeDisplay(3600L).formatTime(), // Hour
                timeDisplay(86400L).formatTime() // Day
            )
            val state = rememberTextFieldState()
            var isNumeric by remember { mutableStateOf(true) }
            var selectedIndex by remember { mutableIntStateOf(0) }
            EditableContentDialog(
                onDismissRequest = { custom = false }, confirm = {
                    setting = preferences.recentlyRemindedKeepTime(
                        state.text.toString().toLong() * when (selectedIndex) {
                            0 -> 60 * 1000 // Minute
                            1 -> 60 * 60 * 1000 // Hour
                            2 -> 24 * 60 * 60 * 1000 // Day
                            else -> throw ArrayStoreException("Item not found!")
                        }
                    )
                    custom = false
                }, title = stringResource(R.string.custom),
                confirmText = stringResource(R.string.confirm),
                state = state, contentProperties = EditableContentDialog(extraContentBottom = {
                    selectedIndex = super.segmentedButton(
                        list = segmentedButton.toTypedArray(), label = { label ->
                            segmentedButton.find { it == label } ?: "N/A"
                        }, initSetter = selectedIndex, Modifier.fillMaxWidth()
                    )
                }), singleLine = true, numberOnly = true,
                disableConform = isNumeric, onDisableConformClick = {
                    Toast.makeText(
                        view.context, view.context.getString(R.string.in_0_to_100_only),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            LaunchedEffect(state.text.toString()) {
                isNumeric = try {
                    state.text.toString().toInt() !in 0..100
                } catch (e: NumberFormatException) {
                    true
                }
            }
        }
        return SettingsItem(
            SettingsType.Notification, stringResource(R.string.recently_reminded_keep_time)
        ) { text ->
            val expanded = remember { mutableStateOf(false) }
            DropdownMenuView(
                expanded = expanded,
                title = text,
                menuShowText = list.find { it.value == setting }?.name ?: timeDisplay((setting * 0.001).toLong(), 3),
                dropdownMenuList = {
                    list.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            if (it.value != -1L) {
                                setting = preferences.recentlyRemindedKeepTime(it.value)
                            } else custom = true
                            expanded.value = false
                        })
                    }
                },
                view = view
            )
        }
    }

    @Composable
    private fun removeNoticeInAutoDelReminded(preferences: PrimaryPreferences): SettingsItem {
        var setting by remember { mutableStateOf(preferences.removeNoticeInAutoDelReminded()) }
        return SettingsItem(
            SettingsType.Notification,
            stringResource(R.string.remove_notice_in_auto_del_reminded)
        ) { text ->
            SwitchView(
                text = text,
                checked = setting,
                onCheckedChange = { setting = preferences.removeNoticeInAutoDelReminded(it) },
                view = view
            )
        }
    }

    @Composable
    private fun systemNotificationSetting(): SettingsItem {
        return SettingsItem(
            SettingsType.Notification, stringResource(R.string.system_notify_settings)
        ) {
            ClickView(
                title = it,
                text = stringResource(R.string.system_notify_settings_describe),
                fullWidth = true,
                onClick = {
                    val intent = Intent().apply {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, view.context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    view.context.startActivity(intent)
                },
                view = view
            )
        }
    }

    @Composable
    fun list(): List<SettingsItem> {
        val primaryPreferences = PrimaryPreferences(view.context)
        return listOf(
            this.disableRemoveNotifyInReminded(primaryPreferences),
            this.disableNoScheduleExactAlarmNotice(primaryPreferences),
            this.recentlyRemindedKeepTime(primaryPreferences),
            this.removeNoticeInAutoDelReminded(primaryPreferences),
            this.systemNotificationSetting()
        )
    }
}
