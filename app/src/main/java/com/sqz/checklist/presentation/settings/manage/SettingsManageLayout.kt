package com.sqz.checklist.presentation.settings.manage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.assembly.settings.SettingsAssembly
import com.sqz.checklist.presentation.settings.manage.scene.SettingsManageSceneUI

/** Marker receiver that keeps settings-only composables out of unrelated code completion. */
internal interface SettingsManageScope

private object SettingsManageScopeInstance : SettingsManageScope

/**
 * Presentation entry point for managing application settings.
 *
 * Navigation supplies search mode while this layout assembles the process-wide Settings feature,
 * observes its immutable state and limits internal UI helpers to [SettingsManageScope]. New
 * setting rows belong in the item package and are registered by the scene's setting inventory.
 */
@Composable
fun SettingsManageLayout(
    searchEnabled: Boolean,
    onSearchCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsManageViewModel = settingsManageViewModel(),
) {
    val settings by viewModel.state.collectAsState()
    with(SettingsManageScopeInstance) {
        SettingsManageSceneUI(
            settings = settings,
            viewModel = viewModel,
            searchEnabled = searchEnabled,
            onSearchCancelled = onSearchCancelled,
            view = LocalView.current,
            modifier = modifier,
        )
    }
}

/** Creates the screen-scoped ViewModel with the process-wide settings feature contract. */
@Composable
private fun settingsManageViewModel(): SettingsManageViewModel {
    val context = LocalContext.current
    val settings = SettingsAssembly.get(context).settings
    return viewModel { SettingsManageViewModel(settings) }
}
