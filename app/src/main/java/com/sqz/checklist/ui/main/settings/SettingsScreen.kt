package com.sqz.checklist.ui.main.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sqz.checklist.R
import com.sqz.checklist.presentation.settings.manage.SettingsManageLayout
import com.sqz.checklist.ui.common.ContentScaffold
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.nav.RootNavRoute

fun NavGraphBuilder.settingsScreen(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
) {
    composable(route = RootNavRoute.Settings::class) {
        var searchEnabled by rememberSaveable { mutableStateOf(false) }
        ContentScaffold(
            modifier = modifier,
            topBar = {
                SettingsTopBar(
                    onBack = {
                        searchEnabled = false
                        rootNavController.popBackStack()
                    },
                )
            },
            floatingActionButton = {
                val buttonText = stringResource(
                    if (searchEnabled) R.string.cancel else R.string.search
                )
                TextTooltipBox(text = buttonText) {
                    FloatingActionButton(onClick = { searchEnabled = !searchEnabled }) {
                        Icon(
                            imageVector = if (searchEnabled) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = buttonText,
                        )
                    }
                }
            },
        ) {
            SettingsManageLayout(
                searchEnabled = searchEnabled,
                onSearchCancelled = { searchEnabled = false },
                modifier = Modifier,
            )
        }
    }
}
