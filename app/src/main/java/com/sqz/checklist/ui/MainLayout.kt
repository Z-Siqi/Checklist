package com.sqz.checklist.ui

import android.content.Context
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberBasicTooltipState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.NavBarLayout
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.main.NavMode
import com.sqz.checklist.ui.main.backup.BackupAndRestoreLayout
import com.sqz.checklist.ui.main.backup.BackupRestoreTopBar
import com.sqz.checklist.ui.main.settings.layout.SettingsLayout
import com.sqz.checklist.ui.main.settings.SettingsLayoutViewModel
import com.sqz.checklist.ui.main.settings.SettingsTopBar
import com.sqz.checklist.ui.main.settings.settingsExtendedNavButton
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.history.HistoryTopBar
import com.sqz.checklist.ui.main.task.history.TaskHistory
import com.sqz.checklist.ui.main.task.history.TaskHistoryNavBar
import com.sqz.checklist.ui.main.task.history.TaskHistoryViewModel
import com.sqz.checklist.ui.main.task.layout.TaskLayout
import com.sqz.checklist.ui.main.task.layout.TaskLayoutTopBar
import com.sqz.checklist.ui.main.task.layout.taskExtendedNavButton
import com.sqz.checklist.ui.main.task.layout.topBarExtendedMenu

enum class MainLayoutNav {
    TaskLayout,
    TaskHistory,
    Settings,
    BackupRestore,
    Unknown,
}

/** Top level of MainLayout **/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainLayout(context: Context, view: View, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var disableBackButton by rememberSaveable { mutableStateOf(false) }

    // ViewModel
    val taskLayoutViewModel: TaskLayoutViewModel = viewModel()
    val taskHistoryViewModel: TaskHistoryViewModel = viewModel()
    val settingsLayoutViewModel: SettingsLayoutViewModel = viewModel()

    // Top bar
    val topBarState = rememberTopAppBarState()
    var canScroll by rememberSaveable { mutableStateOf(true) }
    val scrollBehavior =
        if (canScroll) TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
        else TopAppBarDefaults.pinnedScrollBehavior() //fix scroll bug when open with landscape mode
            .also { if (it.state.heightOffset != 0f) it.state.heightOffset = 0f }
    val taskLayoutTopBar = @Composable {
        val onMenuClick: @Composable (setter: Boolean, getter: (Boolean) -> Unit) -> (Unit) =
            { setter, getter -> // setter: open menu. getter: get whether need close menu.
                val menu = topBarExtendedMenu( // menu UI
                    state = setter,
                    navController = navController,
                    onClickType = taskLayoutViewModel::onTopBarMenuClick,
                    view = view
                )
                if (menu == 0) getter(false)
            }
        canScroll = TaskLayoutTopBar(scrollBehavior, topBarState, onMenuClick, view)
    }
    val taskHistoryTopBar = @Composable {
        HistoryTopBar(onClick = {
            navController.popBackStack()
            view.playSoundEffect(SoundEffectConstants.CLICK)
        })
    }
    val backupRestoreTopBar = @Composable {
        BackupRestoreTopBar(onClick = {
            if (!disableBackButton) navController.popBackStack() else Toast.makeText(
                context, context.getString(R.string.back_disabled_notice), Toast.LENGTH_SHORT
            ).show()
            view.playSoundEffect(SoundEffectConstants.CLICK)
        })
    }
    val settingsTopBar = @Composable {
        SettingsTopBar(onBack = {
            navController.popBackStack()
            settingsLayoutViewModel.resetSearchState()
        }, view = view)
    }

    // Navigation bar
    val mainNavigationBar: @Composable (mode: NavMode) -> Unit = { mode ->
        val extendedButtonData = when (currentRoute) {
            // TaskLayout Extended Nav Button function
            MainLayoutNav.TaskLayout.name -> taskExtendedNavButton(
                mode = mode, view = view, viewModel = taskLayoutViewModel
            )
            // SettingsLayout Extended Nav Button function
            MainLayoutNav.Settings.name -> settingsExtendedNavButton(
                viewModel = settingsLayoutViewModel, view = view
            )
            // The else should never happen, never be called
            else -> NavExtendedButtonData({}, {}, {}, rememberBasicTooltipState())
        }
        NavBarLayout(
            mode = mode,
            extendedButtonData = extendedButtonData,
            selected = { index -> index.name == currentRoute },
            onNavClick = { index ->
                if (index.name != currentRoute) navController.navigate(index.name) {
                    popUpTo(0)
                }
                if (currentRoute == MainLayoutNav.Settings.name) settingsLayoutViewModel.resetSearchState()
            },
            modifier = modifier
        )
    }
    val taskHistoryNavBar: @Composable (mode: NavMode) -> Unit = { mode ->
        TaskHistoryNavBar(
            mode = mode,
            view = view,
            historyState = taskHistoryViewModel,
            modifier = modifier
        )
    }

    // Set Navigation bar mode (Navigation Bar or Navigation Rail)
    val localConfig = LocalConfiguration.current
    val screenIsWidth = localConfig.screenWidthDp > localConfig.screenHeightDp * 1.1
    val navMode = if (!screenIsWidth) NavMode.NavBar else NavMode.Disable
    val navRailMode = if (screenIsWidth) NavMode.NavRail else NavMode.Disable

    // Layout
    val nulLog = { Log.d("MainLayout", "Navigation bar or Top bar is disable") }
    val nul = @Composable { Spacer(modifier = modifier).also { nulLog() } }
    ContentLayout(
        topBar = when (currentRoute) {
            MainLayoutNav.TaskLayout.name -> taskLayoutTopBar
            MainLayoutNav.TaskHistory.name -> taskHistoryTopBar
            MainLayoutNav.BackupRestore.name -> backupRestoreTopBar
            MainLayoutNav.Settings.name -> settingsTopBar
            else -> nul
        },
        bottomBar = {
            when (currentRoute) {
                MainLayoutNav.TaskHistory.name -> taskHistoryNavBar(navMode)
                MainLayoutNav.BackupRestore.name -> nul()
                MainLayoutNav.Unknown.name -> nul()
                else -> mainNavigationBar(navMode)
            }
        },
        navigationRail = {
            when (currentRoute) {
                MainLayoutNav.TaskLayout.name -> mainNavigationBar(navRailMode)
                MainLayoutNav.TaskHistory.name -> taskHistoryNavBar(navRailMode)
                MainLayoutNav.Settings.name -> mainNavigationBar(navRailMode)
                else -> nul()
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        NavHost(
            navController = navController,
            startDestination = MainLayoutNav.TaskLayout.name
        ) {
            composable(MainLayoutNav.TaskLayout.name) {
                TaskLayout(
                    scrollBehavior = scrollBehavior,
                    context = context, view = view,
                    taskState = taskLayoutViewModel
                )
            }
            composable(MainLayoutNav.TaskHistory.name) {
                TaskHistory(historyState = taskHistoryViewModel)
            }
            composable(MainLayoutNav.BackupRestore.name) {
                BackupAndRestoreLayout(view = view) {
                    disableBackButton = it
                }
            }
            composable(MainLayoutNav.Settings.name) {
                SettingsLayout(viewModel = settingsLayoutViewModel, view = view)
            }
        }
    }
}

@Composable
private fun ContentLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    navigationRail: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (paddingValues: PaddingValues) -> Unit = {}
) = Row {
    Surface(
        modifier = modifier.weight(1f)
    ) {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
        ) { paddingValues ->
            Surface(modifier = modifier.padding(paddingValues)) {
                content(paddingValues)
            }
        }
    }
    Surface { navigationRail() }
}
