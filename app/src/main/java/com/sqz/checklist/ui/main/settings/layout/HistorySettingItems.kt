package com.sqz.checklist.ui.main.settings.layout

import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import kotlinx.coroutines.launch
import sqz.checklist.settings.api.manage.Settings
import sqz.checklist.model.settings.AppSettings

class HistorySettingItems(
    private val view: View,
    private val settings: Settings,
    private val appSettings: AppSettings,
) : SettingsList() {

    @Composable
    private fun allowedNumberOfHistory(): SettingsItem {
        val scope = rememberCoroutineScope()
        val currentValue = appSettings.history.allowedNumberOfHistory
        var sliderPosition by remember(currentValue) {
            mutableFloatStateOf(currentValue.toFloat())
        }
        var old by remember { mutableFloatStateOf(sliderPosition) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && old != sliderPosition) {
            old = sliderPosition
            super.vibrationEffect(view)
        }
        val text = when (sliderPosition.toInt()) {
            0 -> stringResource(R.string.disable)
            21 -> stringResource(R.string.unlimited)
            else -> sliderPosition.toInt().toString()
        }
        return SettingsItem(
            SettingsType.History, stringResource(R.string.allowed_number_of_history)
        ) {
            Column(
                Modifier.padding(start = 8.dp, end = 8.dp), verticalArrangement = Arrangement.Center
            ) {
                OptionText(it)
                OptionText(
                    text, modifier = Modifier.fillMaxWidth(),
                    textColor = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center
                )
                Slider(
                    value = sliderPosition, onValueChange = {
                        sliderPosition = it.toInt().toFloat()
                    }, onValueChangeFinished = {
                        scope.launch {
                            settings.setAllowedNumberOfHistory(sliderPosition.toInt())
                        }
                    }, colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ), modifier = Modifier
                        .padding(start = 15.dp, end = 15.dp)
                        .height(38.dp),
                    steps = 21, valueRange = 0f..21f
                )
            }
        }
    }

    @Composable
    private fun clearHistoryWhenLeaved(): SettingsItem {
        val scope = rememberCoroutineScope()
        val currentValue = appSettings.history.clearHistoryWhenLeaving
        var setting by remember(currentValue) {
            mutableStateOf(currentValue)
        }
        return SettingsItem(
            SettingsType.History, stringResource(R.string.clear_history_when_leaved)
        ) { text ->
            super.SwitchView(
                text = text,
                checked = setting,
                onCheckedChange = {
                    setting = it
                    scope.launch { settings.setClearHistoryWhenLeaving(it) }
                },
                view = view
            )
        }
    }

    @Composable
    private fun disableUndoButton(): SettingsItem {
        val scope = rememberCoroutineScope()
        val currentValue = !appSettings.history.undoEnabled
        var setting by remember(currentValue) { mutableStateOf(currentValue) }
        return SettingsItem(
            SettingsType.History, stringResource(R.string.disable_undo_button)
        ) { text ->
            super.SwitchView(
                text = text,
                checked = setting,
                onCheckedChange = {
                    setting = it
                    scope.launch { settings.setUndoEnabled(!it) }
                },
                view = view
            )
        }
    }

    @Composable
    fun list(): List<SettingsItem> {
        return listOf(
            this.allowedNumberOfHistory(),
            this.clearHistoryWhenLeaved(),
            this.disableUndoButton()
        )
    }
}
