package com.sqz.checklist.presentation.settings.manage.scene.item

import android.content.Intent
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.presentation.settings.manage.SettingsManageScope
import com.sqz.checklist.presentation.settings.manage.SettingsManageViewModel
import com.sqz.checklist.presentation.settings.manage.component.ClickSetting
import com.sqz.checklist.presentation.settings.manage.component.DropdownSetting
import com.sqz.checklist.presentation.settings.manage.component.SwitchSetting
import com.sqz.checklist.ui.common.dialog.EditableContentDialog
import com.sqz.checklist.ui.common.unit.timeDisplay
import java.util.Locale
import sqz.checklist.model.settings.AppSettings

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.DisableRemoveNotificationSetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
) {
    SwitchSetting(
        title = stringResource(R.string.disable_remove_notify_in_reminded),
        checked = !settings.reminder.removeNotificationWhenReminded,
        onCheckedChange = { viewModel.setRemoveNotificationWhenReminded(!it) },
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.DisableExactAlarmWarningSetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
) {
    SwitchSetting(
        title = stringResource(R.string.disable_no_schedule_exact_alarm_notice),
        checked = !settings.reminder.showExactAlarmWarning,
        onCheckedChange = { viewModel.setShowExactAlarmWarning(!it) },
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.RecentReminderKeepTimeSetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
    view: View,
) {
    data class TimeOption(val label: String, val value: Long)

    val options = listOf(
        TimeOption(timeDisplay(10_800L), 10_800_000L),
        TimeOption(timeDisplay(18_000L), 18_000_000L),
        TimeOption(timeDisplay(43_200L), 43_200_000L),
        TimeOption(timeDisplay(86_400L), 86_400_000L),
        TimeOption(timeDisplay(604_800L), 604_800_000L),
        TimeOption(stringResource(R.string.unlimited), 0L),
        TimeOption(stringResource(R.string.custom), -1L),
    )
    val value = settings.reminder.recentlyRemindedKeepTimeMillis
    var customDialogVisible by remember { mutableStateOf(false) }
    DropdownSetting(
        title = stringResource(R.string.recently_reminded_keep_time),
        selectedText = options.find { it.value == value }?.label
            ?: timeDisplay((value * 0.001).toLong(), 3),
        options = options.map(TimeOption::label),
        onOptionSelected = { index ->
            val selected = options[index].value
            if (selected == -1L) customDialogVisible = true
            else viewModel.setRecentlyRemindedKeepTimeMillis(selected)
        },
    )
    if (customDialogVisible) {
        CustomKeepTimeDialog(
            view = view,
            onDismiss = { customDialogVisible = false },
            onConfirm = {
                viewModel.setRecentlyRemindedKeepTimeMillis(it)
                customDialogVisible = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomKeepTimeDialog(
    view: View,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val units = listOf(
        timeDisplay(60L).unitLabel(),
        timeDisplay(3_600L).unitLabel(),
        timeDisplay(86_400L).unitLabel(),
    )
    val multipliers = remember { listOf(60_000L, 3_600_000L, 86_400_000L) }
    val textState = rememberTextFieldState()
    var selectedIndex by remember { mutableIntStateOf(0) }
    val invalid by remember { derivedStateOf { textState.text.toString().toIntOrNull() !in 0..100 } }
    EditableContentDialog(
        onDismissRequest = onDismiss,
        confirm = {
            textState.text.toString().toLongOrNull()?.let {
                onConfirm(it * multipliers[selectedIndex])
            }
        },
        title = stringResource(R.string.custom),
        confirmText = stringResource(R.string.confirm),
        state = textState,
        contentProperties = EditableContentDialog(
            extraContentBottom = {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    units.forEachIndexed { index, unit ->
                        FilterChip(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            label = { Text(unit) },
                        )
                    }
                }
            },
        ),
        singleLine = true,
        numberOnly = true,
        disableConform = invalid,
        onDisableConformClick = {
            Toast.makeText(view.context, R.string.in_0_to_100_only, Toast.LENGTH_SHORT).show()
        },
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.RemoveNotificationWithHistorySetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
) {
    SwitchSetting(
        title = stringResource(R.string.remove_notice_in_auto_del_reminded),
        checked = settings.reminder.removeNotificationWhenHistoryDeleted,
        onCheckedChange = viewModel::setRemoveNotificationWhenHistoryDeleted,
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.SystemNotificationSetting(view: View) {
    ClickSetting(
        title = stringResource(R.string.system_notify_settings),
        supportingText = stringResource(R.string.system_notify_settings_describe),
        onClick = {
            view.context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, view.context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        },
    )
}

private fun String.unitLabel(): String = replace("1", "")
    .replace(" ", "")
    .replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(Locale.getDefault()) else character.toString()
    }
