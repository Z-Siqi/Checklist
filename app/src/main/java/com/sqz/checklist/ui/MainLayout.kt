package com.sqz.checklist.ui

import android.content.Context
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sqz.checklist.ui.main.NavBar
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.history.HistoryTopBar
import com.sqz.checklist.ui.main.task.history.TaskHistory
import com.sqz.checklist.ui.main.task.history.TaskHistoryNavBar
import com.sqz.checklist.ui.main.task.history.TaskHistoryViewModel
import com.sqz.checklist.ui.main.task.layout.NavExtendedConnectData
import com.sqz.checklist.ui.main.task.layout.TaskLayout
import com.sqz.checklist.ui.main.task.layout.TaskLayoutTopBar
import com.sqz.checklist.ui.main.task.layout.taskExtendedNavButton
import com.sqz.checklist.ui.main.task.layout.topBarExtendedMenu

enum class MainLayoutNav {
    TaskLayout,
    TaskHistory,
    Unknown,
}

/** Top level of MainLayout **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(context: Context, view: View, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ViewModel
    val taskLayoutViewModel: TaskLayoutViewModel = viewModel()
    val taskHistoryViewModel: TaskHistoryViewModel = viewModel()

    // Navigation bar
    val mainNavigationBar = @Composable {
        @Suppress("OPT_IN_USAGE_FUTURE_ERROR") val extendedButtonData =
            when (currentRoute) {
                // TaskLayout Extended Nav Button function
                MainLayoutNav.TaskLayout.name -> taskExtendedNavButton(
                    view, context, taskLayoutViewModel
                )
                // The else should never happen, never be called
                else -> NavExtendedButtonData()
            }
        NavBar(
            extendedButtonData = extendedButtonData,
            selected = { index -> index.name == currentRoute },
            onNavClick = { index ->
                if (index.name != currentRoute) navController.navigate(index.name) {
                    popUpTo(0)
                }
            },
            modifier = modifier
        )
    }
    val taskHistoryNavBar = @Composable {
        TaskHistoryNavBar(view = view, historyState = taskHistoryViewModel)
    }

    // Top bar
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
    val taskLayoutTopBar = @Composable {
        val onMenuClick: @Composable (setter: Boolean, getter: (Boolean) -> Unit) -> (Unit) =
            { setter, getter -> // setter: open menu. getter: get whether need close menu.
                val menu = topBarExtendedMenu( // menu UI
                    state = setter,
                    onClickToTaskHistory = {
                        taskLayoutViewModel.resetUndo(context)
                        navController.navigate(MainLayoutNav.TaskHistory.name)
                    },
                    onClickToSearch = {
                        val it = NavExtendedConnectData(searchState = true)
                        taskLayoutViewModel.updateNavConnector(it, it)
                    }, view = view
                )
                if (menu == 0) getter(false)
            }
        TaskLayoutTopBar(scrollBehavior, topBarState, onMenuClick = onMenuClick, view)
    }
    val taskHistoryTopBar = @Composable {
        HistoryTopBar(onClick = {
            navController.popBackStack()
            view.playSoundEffect(SoundEffectConstants.CLICK)
        })
    }

    // Layout
    val nulLog = { Log.d("MainLayout", "Navigation bar or Top bar is disable") }
    val nul = @Composable { Spacer(modifier = modifier).also { nulLog() } }
    ContentLayout(
        topBar = when (currentRoute) {
            MainLayoutNav.TaskLayout.name -> taskLayoutTopBar
            MainLayoutNav.TaskHistory.name -> taskHistoryTopBar
            else -> nul
        },
        bottomBar = when (currentRoute) {
            MainLayoutNav.TaskHistory.name -> taskHistoryNavBar
            MainLayoutNav.Unknown.name -> nul
            else -> mainNavigationBar
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
    content: @Composable () -> Unit = {}
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
                content()
            }
        }
    }
    Surface { navigationRail() }
}
