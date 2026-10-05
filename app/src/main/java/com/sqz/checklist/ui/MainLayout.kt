package com.sqz.checklist.ui

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.ContentScaffold
import com.sqz.checklist.ui.main.backup.BackupAndRestoreLayout
import com.sqz.checklist.ui.main.backup.BackupRestoreTopBar
import com.sqz.checklist.ui.main.history.task.taskHistoryScreen
import com.sqz.checklist.ui.main.settings.settingsScreen
import com.sqz.checklist.ui.main.task.taskScreen
import com.sqz.checklist.ui.nav.RootNavRoute
import com.sqz.checklist.ui.nav.group.home.homeNavGroup

enum class MainLayoutNav {
    BackupRestore,
}

/**
 * Hosts the application's root navigation and assembles the process-wide settings feature.
 *
 * @param modifier Modifier applied to the root navigation host.
 * @param context Application context used by platform integrations and settings assembly.
 * @param view Activity decor view used by Android-specific presentation components.
 */
@Composable
fun MainLayout(modifier: Modifier, context: Context, view: View) {
    val navController = rememberNavController()
    val mainCoroutineScope = rememberCoroutineScope()

    val refreshListRequest = rememberSaveable { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = RootNavRoute.Home,
        modifier = modifier,
        predictivePopEnterTransition = { fadeIn() },
        predictivePopExitTransition = { fadeOut() },
    ) {
        homeNavGroup(route = RootNavRoute.Home::class) { homeNavController, homeVM ->
            taskScreen(
                mainCoroutineScope = mainCoroutineScope,
                homeViewModel = homeVM,
                homeNavController = homeNavController,
                rootNavController = navController,
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

        settingsScreen(rootNavController = navController, modifier = modifier)
    }
}
