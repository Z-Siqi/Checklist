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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.preferences.PrimaryPreferences

class HistorySettingItems(private val view: View) : SettingsList() {

    @Composable
    private fun allowedNumberOfHistory(preferences: PrimaryPreferences): SettingsItem {
        var sliderPosition by remember {
            mutableFloatStateOf(preferences.allowedNumberOfHistory().toFloat())
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
                        sliderPosition = preferences.allowedNumberOfHistory(
                            it.toInt()
                        ).toFloat()
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
    private fun clearHistoryWhenLeaved(preferences: PrimaryPreferences): SettingsItem {
        var setting by remember { mutableStateOf(preferences.clearHistoryWhenLeaved()) }
        return SettingsItem(
            SettingsType.History, stringResource(R.string.clear_history_when_leaved)
        ) { text ->
            super.SwitchView(
                text = text,
                checked = setting,
                onCheckedChange = { setting = preferences.clearHistoryWhenLeaved(it) },
                view = view
            )
        }
    }

    @Composable
    private fun disableUndoButton(preferences: PrimaryPreferences): SettingsItem {
        var setting by remember { mutableStateOf(preferences.disableUndoButton()) }
        return SettingsItem(
            SettingsType.History, stringResource(R.string.disable_undo_button)
        ) { text ->
            super.SwitchView(
                text = text,
                checked = setting,
                onCheckedChange = { setting = preferences.disableUndoButton(it) },
                view = view
            )
        }
    }

    @Composable
    fun list(): List<SettingsItem> {
        val primaryPreferences = PrimaryPreferences(view.context)
        return listOf(
            this.allowedNumberOfHistory(primaryPreferences),
            this.clearHistoryWhenLeaved(primaryPreferences),
            this.disableUndoButton(primaryPreferences)
        )
    }
}
