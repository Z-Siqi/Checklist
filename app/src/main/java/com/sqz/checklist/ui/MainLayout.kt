package com.sqz.checklist.ui

import android.content.Context
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.NavBar
import com.sqz.checklist.ui.main.NavExtendedButtonData
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.history.TaskHistory
import com.sqz.checklist.ui.main.task.history.TaskHistoryNavBar
import com.sqz.checklist.ui.main.task.history.TaskHistoryViewModel
import com.sqz.checklist.ui.main.task.layout.TaskLayout
import com.sqz.checklist.ui.main.task.layout.taskExtendedNavButton
import com.sqz.checklist.ui.material.TextTooltipBox
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MainLayoutNav {
    TaskLayout,
    TaskHistory,
    Unknown,
}

/** Top level of MainLayout **/
@Composable
fun MainLayout(context: Context, view: View, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val taskLayoutViewModel: TaskLayoutViewModel = viewModel()
    val taskHistoryViewModel: TaskHistoryViewModel = viewModel()

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

    val nulLog = { Log.d("MainLayout", "Navigation bar is disable") }
    val nul = @Composable { Spacer(modifier = modifier).also { nulLog() } }
    ContentLayout(
        bottomBar = when (currentRoute) {
            MainLayoutNav.TaskHistory.name -> taskHistoryNavBar
            MainLayoutNav.Unknown.name -> nul
            else -> mainNavigationBar
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = MainLayoutNav.TaskLayout.name
        ) {
            composable(MainLayoutNav.TaskLayout.name) {
                TaskLayout(
                    toTaskHistory = { navController.navigate(MainLayoutNav.TaskHistory.name) },
                    context = context, view = view,
                    taskState = taskLayoutViewModel
                )
            }
            composable(MainLayoutNav.TaskHistory.name) {
                TaskHistory(
                    navBack = { navController.popBackStack() },
                    historyState = taskHistoryViewModel
                )
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

/** MainLayout Top App Bar **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    topBarState: TopAppBarState,
    onClick: () -> Unit,
    view: View,
    modifier: Modifier = Modifier
) {
    val topAppBarTitle = stringResource(R.string.time_format)
    val year = "YYYY"
    val week = "EEEE"
    MediumTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
            scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        title = {
            if (topBarState.heightOffset <= topBarState.heightOffsetLimit * 0.7) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = topBarContent(topAppBarTitle),
                        maxLines = 1,
                        modifier = modifier.padding(bottom = 1.dp),
                        overflow = TextOverflow.Visible
                    )
                    Text(
                        text = topBarContent(year),
                        maxLines = 1,
                        fontSize = 15.sp,
                        overflow = TextOverflow.Visible
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = topBarContent(stringResource(R.string.top_bar_date)),
                        modifier = modifier.height(30.dp),
                        maxLines = 1,
                        fontSize = 24.sp,
                        overflow = TextOverflow.Visible
                    )
                    Spacer(modifier = modifier.width(10.dp))
                    Text(
                        text = topBarContent(year),
                        modifier = modifier.height(28.dp),
                        maxLines = 1,
                        fontSize = 15.sp,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        },
        actions = {
            TextTooltipBox(
                textRid = R.string.more_options,
                topRightExtraPadding = true
            ) {
                IconButton(onClick = {
                    onClick()
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options)
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
    val visible = topBarState.heightOffset >= topBarState.heightOffsetLimit * 0.58
    if (topBarState.heightOffset != topBarState.heightOffsetLimit) {
        AnimatedVisibility(
            visible = visible,
            enter = expandHorizontally(
                expandFrom = Alignment.CenterHorizontally
            ) + fadeIn(
                initialAlpha = 0.3f
            ),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            Row(
                modifier = modifier.padding(top = 22.dp, start = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Spacer(modifier = modifier.width(10.dp))
                Text(
                    text = topBarContent(week),
                    maxLines = 1,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
private fun topBarContent(pattern: String): String {
    val currentDate = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return currentDate.format(formatter)
}
