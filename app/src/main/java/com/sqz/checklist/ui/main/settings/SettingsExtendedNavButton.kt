package com.sqz.checklist.ui.main.settings

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.common.NonExtendedTooltip

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun settingsExtendedNavButton(
    viewModel: SettingsLayoutViewModel,
    view: View
): NavExtendedButtonData {
    val buttonText = stringResource(
        if (viewModel.getSearchState()) R.string.cancel else R.string.search
    )
    val icon = @Composable {
        val icon = if (viewModel.getSearchState()) Icons.Filled.Close else Icons.Filled.Search
        Icon(icon, contentDescription = buttonText)
    }
    val label = @Composable { Text(buttonText) }
    val tooltipContent = @Composable {
        NonExtendedTooltip(buttonText, view)
    }
    val tooltipState = rememberBasicTooltipState(isPersistent = false)
    val onClick = {
        if (viewModel.getSearchState()) viewModel.resetSearchState() else viewModel.requestSearch()
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    return NavExtendedButtonData(
        icon = icon,
        label = label,
        tooltipContent = tooltipContent,
        tooltipState = tooltipState,
        onClick = onClick
    )
}
