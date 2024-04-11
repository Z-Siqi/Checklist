package com.sqz.checklist.ui

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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sqz.checklist.R
import com.sqz.checklist.ui.mainLayout.taskLayout.TaskHistory
import com.sqz.checklist.ui.mainLayout.taskLayout.TaskLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MainLayoutNav {
    TaskLayout,
    TaskHistory,
}

/** Top level of MainLayout **/
@Composable
fun MainLayout() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = MainLayoutNav.TaskLayout.name
    ) {
        composable(MainLayoutNav.TaskLayout.name) {
            TaskLayout(toTaskHistory = {
                navController.navigate(MainLayoutNav.TaskHistory.name)
            })
        }
        composable(MainLayoutNav.TaskHistory.name) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            TaskHistory(navBack = {
                if (currentRoute != MainLayoutNav.TaskLayout.name) {
                    navController.popBackStack()
                }
            })
        }
    }
}

/** MainLayout NavigationBar **/
@Composable
fun NavBar(
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class Items(
        val text: String,
        val icon: ImageVector
    )
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(Items(stringResource(R.string.tasks), Icons.Filled.Home))
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Spacer(modifier = modifier.weight(0.5f))
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                modifier = modifier.weight(1f),
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.inversePrimary,
                    selectedIconColor = MaterialTheme.colorScheme.inverseSurface,
                    disabledIconColor = MaterialTheme.colorScheme.primary
                ),
                icon = { Icon(item.icon, contentDescription = item.text) },
                label = { Text(item.text) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
        Spacer(modifier = modifier.weight(0.5f))
        VerticalDivider(modifier = modifier.height(50.dp))
        NavigationBarItem(
            colors = NavigationBarItemDefaults.colors(MaterialTheme.colorScheme.primary),
            icon = icon,
            label = label,
            selected = false,
            onClick = onClick
        )
    }
}

/** MainLayout Top App Bar **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    topBarState: TopAppBarState,
    onClick: () -> Unit,
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
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.more_options)
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun Preview() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = { TopBar(scrollBehavior, rememberTopAppBarState(), {}) },
        bottomBar = { NavBar({ Icon(Icons.Filled.AddCircle, "") }, { Text("Add") }, {}) }
    ) { Modifier.padding(it) }
}
