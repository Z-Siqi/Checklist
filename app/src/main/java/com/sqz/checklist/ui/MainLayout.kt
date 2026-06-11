package com.sqz.checklist.ui

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.ContentScaffold
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.main.backup.BackupAndRestoreLayout
import com.sqz.checklist.ui.main.backup.BackupRestoreTopBar
import com.sqz.checklist.ui.main.history.task.taskHistoryScreen
import com.sqz.checklist.ui.main.settings.SettingsLayoutViewModel
import com.sqz.checklist.ui.main.settings.SettingsTopBar
import com.sqz.checklist.ui.main.settings.layout.SettingsLayout
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.taskScreen
import com.sqz.checklist.ui.nav.RootNavRoute
import com.sqz.checklist.ui.nav.group.home.homeNavGroup

enum class MainLayoutNav {
    Settings,
    BackupRestore,
}

@Composable
fun MainLayout(modifier: Modifier, context: Context, view: View) {
    val navController = rememberNavController()

    val refreshListRequest = rememberSaveable { mutableStateOf(false) }
    val settingsLayoutViewModel: SettingsLayoutViewModel = viewModel()
    val taskState: TaskLayoutViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = RootNavRoute.Home,
        modifier = modifier,
    ) {
        homeNavGroup(route = RootNavRoute.Home::class) { homeNavController, homeVM ->
            taskScreen(
                homeViewModel = homeVM,
                homeNavController = homeNavController,
                rootNavController = navController,
                taskState = taskState,
                view = view,
                refreshListRequest = refreshListRequest,
            )
        }

        taskHistoryScreen(
            route = RootNavRoute.TaskHistory::class,
            rootNavController = navController,
            modifier = modifier,
        )

        composable(MainLayoutNav.BackupRestore.name) { //TODO: Finish refactoring this
            val disableBackButton = rememberSaveable { mutableStateOf(false) }
            ContentScaffold(
                topBar = {
                    BackupRestoreTopBar(onClick = {
                        if (!disableBackButton.value) {
                            navController.popBackStack()
                        } else {
                            Toast.makeText(
                                context,
                                R.string.back_disabled_notice,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    })
                }
            ) {
                BackupAndRestoreLayout(
                    refreshListRequest = refreshListRequest,
                    view = view,
                ) {
                    disableBackButton.value = it
                }
            }
        }

        composable(MainLayoutNav.Settings.name) { //TODO: Finish refactoring this
            ContentScaffold(
                topBar = {
                    SettingsTopBar(
                        onBack = {
                            navController.popBackStack()
                            settingsLayoutViewModel.resetSearchState()
                        },
                        view = view,
                    )
                },
                floatingActionButton = {
                    val buttonText = stringResource(
                        if (settingsLayoutViewModel.getSearchState()) R.string.cancel else R.string.search
                    )
                    TextTooltipBox(text = buttonText) {
                        FloatingActionButton(
                            onClick = {
                                if (settingsLayoutViewModel.getSearchState()) {
                                    settingsLayoutViewModel.resetSearchState()
                                } else {
                                    settingsLayoutViewModel.requestSearch()
                                }
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                            }
                        ) {
                            val icon = if (settingsLayoutViewModel.getSearchState()) {
                                Icons.Filled.Close
                            } else {
                                Icons.Filled.Search
                            }
                            Icon(icon, contentDescription = buttonText)
                        }
                    }
                }
            ) {
                SettingsLayout(
                    viewModel = settingsLayoutViewModel,
                    view = view,
                )
            }
        }
    }
}
