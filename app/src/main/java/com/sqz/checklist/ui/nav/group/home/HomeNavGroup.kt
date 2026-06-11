package com.sqz.checklist.ui.nav.group.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.sqz.checklist.R
import com.sqz.checklist.common.AndroidEffectFeedback
import com.sqz.checklist.ui.common.ContentScaffold
import com.sqz.checklist.ui.common.unit.isLandscape
import com.sqz.checklist.ui.common.unit.rightSideWindowInsets
import com.sqz.checklist.ui.nav.group.NavButtonLongClickScaffold
import com.sqz.checklist.ui.nav.group.home.button.taskExtendedButton
import kotlinx.serialization.Serializable
import sqz.checklist.common.EffectFeedback
import kotlin.reflect.KClass

@Serializable
sealed interface HomeNavGroup {

    @Serializable
    data object TaskNavRoute : HomeNavGroup

    companion object {
        fun navValueOf(homeDestination: NavDestination?): HomeNavGroup? {
            if (homeDestination == null) return null
            val route = homeDestination.route
            return entries().firstOrNull {
                it.serialName() == route
            } ?: navValueOf(homeDestination.parent)
        }
    }
}

private fun HomeNavGroup.serialName(): String = when (this) {
    HomeNavGroup.TaskNavRoute -> HomeNavGroup.TaskNavRoute.serializer().descriptor.serialName
}

private val selectedNavRoute: (index: Int) -> HomeNavGroup = { index ->
    when (index) {
        0 -> HomeNavGroup.TaskNavRoute
        else -> throw IllegalArgumentException("Unknown HomeNavGroup state")
    }
}

@Stable
private val navButtonItems: @Composable () -> List<NavButton> = {
    listOf(
        NavButton(stringResource(R.string.tasks), R.drawable.task_icon),
    )
}

@Stable
data class HomeNavGroupExtendedButton(
    val icon: @Composable () -> Unit = {},
    val label: @Composable (() -> Unit)? = null,
    val tooltip: String? = null,
    val menu: @Composable (() -> Boolean)? = null,
    val onClick: () -> Unit = {},
) {
    init {
        val requireMsg = "Cannot have both a tooltip and a menu defined at the same time!"
        require(!(tooltip != null && menu != null)) { requireMsg }
    }
}

fun <T : Any> NavGraphBuilder.homeNavGroup(
    route: KClass<T>,
    builder: NavGraphBuilder.(
        homeNavController: NavHostController,
        homeViewModel: HomeNavGroupInterface,
    ) -> Unit,
) {
    @Composable
    fun getNavExtendedButton(
        current: HomeNavGroup?, viewModel: HomeNavGroupViewModel, feedback: EffectFeedback
    ): HomeNavGroupExtendedButton {
        return when (current) {
            HomeNavGroup.TaskNavRoute -> current.taskExtendedButton(
                state = viewModel.getTaskTypeState(),
                onClickRequest = { viewModel.taskNavRequest.value = it },
                feedback = feedback
            )

            null -> HomeNavGroupExtendedButton()
        }
    }
    composable(route = route) {
        val view = LocalView.current
        val homeNavController = rememberNavController()
        val viewModel: HomeNavGroupViewModel = viewModel()

        val homeBackStackEntry by homeNavController.currentBackStackEntryAsState()
        val currentDestination = homeBackStackEntry?.destination
        val navExtendedButton = getNavExtendedButton(
            current = HomeNavGroup.navValueOf(currentDestination),
            viewModel = viewModel,
            feedback = AndroidEffectFeedback(view)
        )

        val isLandscape = isLandscape(288.dp)
        ContentScaffold(
            bottomBar = {
                if (!isLandscape) NavBar(
                    navExtendedButton = navExtendedButton,
                    selected = { HomeNavGroup.navValueOf(currentDestination) == it },
                    onNavClick = { routeItem ->
                        routeItem.onNavigate(currentDestination, homeNavController)
                    }
                )
            },
            navigationRail = {
                if (isLandscape) NavRailBar(
                    navExtendedButton = navExtendedButton,
                    selected = { HomeNavGroup.navValueOf(currentDestination) == it },
                    onNavClick = { routeItem ->
                        routeItem.onNavigate(currentDestination, homeNavController)
                    }
                )
            },
        ) {
            NavHost(
                navController = homeNavController,
                startDestination = HomeNavGroup.TaskNavRoute,
                builder = { builder(homeNavController, viewModel) }
            )
        }
    }
}

private fun HomeNavGroup.onNavigate(
    currentDestination: NavDestination?,
    homeNavController: NavHostController,
) {
    val routeItem = this
    if (HomeNavGroup.navValueOf(currentDestination) != routeItem) {
        homeNavController.navigate(routeItem, navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(homeNavController.graph.startDestinationId) {
                saveState = true
            }
        })
    }
}

private fun HomeNavGroup.Companion.entries(): List<HomeNavGroup> {
    val list = mutableListOf<HomeNavGroup>()
    try {
        while (true) {
            list.add(selectedNavRoute(list.size))
        }
    } catch (_: IllegalArgumentException) {
        return list
    }
}

@Stable
private data class NavButton(val text: String, val icon: Int)

@Composable
private fun NavBar(
    navExtendedButton: HomeNavGroupExtendedButton,
    selected: (index: HomeNavGroup) -> Boolean,
    onNavClick: (index: HomeNavGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.shadow(
            elevation = 5.dp,
            ambientColor = MaterialTheme.colorScheme.secondary
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Spacer(modifier = Modifier.weight(0.5f))
        navButtonItems().forEachIndexed { index, item ->
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                colors = NavigationBarItemDefaults.colors(),
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.text,
                        modifier = Modifier.size(24.dp, 24.dp)
                    )
                },
                label = { Text(item.text) },
                selected = selected(selectedNavRoute(index)),
                onClick = { onNavClick(selectedNavRoute(index)) }
            )
        }
        Spacer(modifier = modifier.weight(0.5f))
        VerticalDivider(
            modifier = Modifier.height(50.dp),
            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else DividerDefaults.color
        )
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            NavButtonLongClickScaffold(
                tooltip = navExtendedButton.tooltip,
                menu = navExtendedButton.menu,
            ) { interactionSource, longClicked ->
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(MaterialTheme.colorScheme.primary),
                    icon = navExtendedButton.icon,
                    modifier = Modifier,
                    label = navExtendedButton.label,
                    selected = false,
                    onClick = { if (!longClicked) navExtendedButton.onClick() },
                    interactionSource = interactionSource,
                )
            }
        }
    }
}

@Composable
private fun NavRailBar(
    navExtendedButton: HomeNavGroupExtendedButton,
    selected: (index: HomeNavGroup) -> Boolean,
    onNavClick: (index: HomeNavGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            windowInsets = NavigationRailDefaults.rightSideWindowInsets()
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            navButtonItems().forEachIndexed { index, item ->
                NavigationRailItem(
                    modifier = Modifier.weight(1f),
                    colors = NavigationRailItemDefaults.colors(),
                    icon = {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.text,
                            modifier = Modifier.size(24.dp, 24.dp)
                        )
                    },
                    label = { Text(item.text) },
                    selected = selected(selectedNavRoute(index)),
                    onClick = { onNavClick(selectedNavRoute(index)) }
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
            HorizontalDivider(
                modifier = Modifier.width(50.dp),
                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else DividerDefaults.color
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                NavButtonLongClickScaffold(
                    tooltip = navExtendedButton.tooltip,
                    menu = navExtendedButton.menu,
                ) { interactionSource, longClicked ->
                    NavigationRailItem(
                        colors = NavigationRailItemDefaults.colors(MaterialTheme.colorScheme.primary),
                        icon = navExtendedButton.icon,
                        label = navExtendedButton.label,
                        selected = false,
                        onClick = { if (!longClicked) navExtendedButton.onClick() },
                        interactionSource = interactionSource,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NavBarPreview() {
    NavBar(
        navExtendedButton = HomeNavGroupExtendedButton(
            icon = { Icon(imageVector = Icons.Default.Build, "icon") },
            label = { Text("Test") }, tooltip = "Test", menu = null, onClick = {}
        ),
        selected = { true },
        onNavClick = {}
    )
}

@Preview
@Composable
private fun NavRailBarPreview() {
    NavRailBar(
        navExtendedButton = HomeNavGroupExtendedButton(
            icon = { Icon(imageVector = Icons.Default.Build, "icon") },
            label = { Text("Test") },
            tooltip = null, menu = null, onClick = {}
        ),
        selected = { true },
        onNavClick = {}
    )
}
