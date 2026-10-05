package com.sqz.checklist.presentation.settings.manage.scene.item

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sqz.checklist.R
import com.sqz.checklist.presentation.settings.manage.SettingsManageScope
import com.sqz.checklist.presentation.settings.manage.SettingsManageViewModel
import com.sqz.checklist.presentation.settings.manage.component.SettingText
import com.sqz.checklist.presentation.settings.manage.component.SwitchSetting
import androidx.compose.ui.res.stringResource
import sqz.checklist.model.settings.AppSettings

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.AllowedHistorySetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
    view: View,
) {
    val currentValue = settings.history.allowedNumberOfHistory
    var sliderPosition by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }
    var previousValue by remember { mutableFloatStateOf(sliderPosition) }
    val valueText = when (sliderPosition.toInt()) {
        0 -> stringResource(R.string.disable)
        21 -> stringResource(R.string.unlimited)
        else -> sliderPosition.toInt().toString()
    }
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingText(stringResource(R.string.allowed_number_of_history))
        SettingText(
            text = valueText,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it.toInt().toFloat()
                if (previousValue != sliderPosition) {
                    previousValue = sliderPosition
                    vibrateTick(view)
                }
            },
            onValueChangeFinished = {
                viewModel.setAllowedNumberOfHistory(sliderPosition.toInt())
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            valueRange = 0f..21f,
            steps = 20,
        )
    }
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.ClearHistorySetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
) {
    SwitchSetting(
        title = stringResource(R.string.clear_history_when_leaved),
        checked = settings.history.clearHistoryWhenLeaving,
        onCheckedChange = viewModel::setClearHistoryWhenLeaving,
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.DisableUndoSetting(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
) {
    SwitchSetting(
        title = stringResource(R.string.disable_undo_button),
        checked = !settings.history.undoEnabled,
        onCheckedChange = { viewModel.setUndoEnabled(!it) },
    )
}

private fun vibrateTick(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.getSystemService(view.context, Vibrator::class.java)?.vibrate(
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        )
    }
}
