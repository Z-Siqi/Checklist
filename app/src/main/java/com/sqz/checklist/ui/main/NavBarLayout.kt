package com.sqz.checklist.ui.main

import android.os.Build
import android.util.Log
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BasicTooltipState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.MainLayoutNav

/**
 * Extended Button actions layout data for the Navigation Bar
 */
data class NavExtendedButtonData @OptIn(ExperimentalFoundationApi::class) constructor(
    val icon: @Composable () -> Unit = {},
    val label: @Composable () -> Unit = {},
    val tooltipContent: @Composable () -> Unit = {},
    val tooltipState: BasicTooltipState = BasicTooltipState(),
    val onClick: () -> Unit = {},
)

enum class NavMode { NavBar, NavRail, Disable }

/**
 * Main Navigation Bar Layout
 */
@Composable
fun NavBarLayout(
    mode: NavMode,
    extendedButtonData: NavExtendedButtonData,
    selected: (index: MainLayoutNav) -> Boolean,
    onNavClick: (index: MainLayoutNav) -> Unit,
    modifier: Modifier = Modifier
) = when (mode) {
    NavMode.NavBar -> NavBar(extendedButtonData, selected, onNavClick, modifier)
    NavMode.NavRail -> NavRailBar(extendedButtonData, selected, onNavClick, modifier)
    NavMode.Disable -> {
        val nulLog = { Log.d("NavBarLayout", "The navigation bar is disable") }
        Spacer(modifier = modifier).also { nulLog() }
    }
}

private data class Items(val text: String, val icon: Int) // Navigation bar items

private val items: @Composable () -> List<Items> = {
    //Navigation bar buttons
    listOf(
        Items(stringResource(R.string.tasks), R.drawable.task_icon), //id: 0
    )
}
private val selectedInNav: (index: Int) -> MainLayoutNav = { index ->
    when (index) { //Link items list id to Navigation
        0 -> MainLayoutNav.TaskLayout
        else -> MainLayoutNav.Unknown
    }
}

/** Navigation Bar **/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NavBar(
    extendedButtonData: NavExtendedButtonData,
    selected: (index: MainLayoutNav) -> Boolean,
    onNavClick: (index: MainLayoutNav) -> Unit,
    modifier: Modifier = Modifier
) {
    val notButtonNav = WindowInsets.navigationBars.getBottom(LocalDensity.current) < 100
    val heightLimit =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && notButtonNav) {
            modifier.heightIn(max = 100.dp)
        } else modifier
    NavigationBar(
        modifier = modifier.shadow(
            elevation = 5.dp,
            ambientColor = MaterialTheme.colorScheme.secondary
        ) then heightLimit,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Spacer(modifier = modifier.weight(0.5f))
        items().forEachIndexed { index, item ->
            NavigationBarItem(
                modifier = modifier.weight(1f),
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.inversePrimary,
                    selectedIconColor = MaterialTheme.colorScheme.inverseSurface,
                    disabledIconColor = MaterialTheme.colorScheme.primary
                ),
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.text,
                        modifier = modifier.size(24.dp, 24.dp)
                    )
                },
                label = { Text(item.text) },
                selected = selected(selectedInNav(index)),
                onClick = { onNavClick(selectedInNav(index)) }
            )
        }
        Spacer(modifier = modifier.weight(0.5f))
        VerticalDivider(
            modifier = modifier.height(50.dp), color = if (isSystemInDarkTheme()) {
                MaterialTheme.colorScheme.onSurface
            } else DividerDefaults.color
        )
        Row(modifier = modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            BasicTooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = extendedButtonData.tooltipContent,
                state = extendedButtonData.tooltipState
            ) {
                NavigationBarItem(
                    modifier = modifier,
                    colors = NavigationBarItemDefaults.colors(MaterialTheme.colorScheme.primary),
                    icon = extendedButtonData.icon,
                    label = extendedButtonData.label,
                    selected = false,
                    onClick = extendedButtonData.onClick
                )
            }
        }
    }
}

/** Navigation Rail **/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NavRailBar(
    extendedButtonData: NavExtendedButtonData,
    selected: (index: MainLayoutNav) -> Boolean,
    onNavClick: (index: MainLayoutNav) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Spacer(modifier = modifier.weight(0.5f))
        items().forEachIndexed { index, item ->
            NavigationRailItem(
                modifier = modifier.weight(1f),
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.inversePrimary,
                    selectedIconColor = MaterialTheme.colorScheme.inverseSurface,
                    disabledIconColor = MaterialTheme.colorScheme.primary
                ),
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.text,
                        modifier = modifier.size(24.dp, 24.dp)
                    )
                },
                label = { Text(item.text) },
                selected = selected(selectedInNav(index)),
                onClick = { onNavClick(selectedInNav(index)) }
            )
        }
        Spacer(modifier = modifier.weight(0.5f))
        HorizontalDivider(
            modifier = modifier.width(50.dp), color = if (isSystemInDarkTheme()) {
                MaterialTheme.colorScheme.onSurface
            } else DividerDefaults.color
        )
        Column(modifier = modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            BasicTooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = extendedButtonData.tooltipContent,
                state = extendedButtonData.tooltipState
            ) {
                NavigationRailItem(
                    modifier = modifier,
                    colors = NavigationRailItemDefaults.colors(MaterialTheme.colorScheme.primary),
                    icon = extendedButtonData.icon,
                    label = extendedButtonData.label,
                    selected = false,
                    onClick = extendedButtonData.onClick
                )
            }
        }
    }
}

@Preview
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavPreview() {
    NavBar(
        extendedButtonData = NavExtendedButtonData(
            icon = { Icon(Icons.Filled.AddCircle, "") },
            label = { Text("TEST") },
            tooltipContent = { Text("TEST") },
            tooltipState = BasicTooltipState(),
            onClick = {}
        ), selected = { true }, onNavClick = {}
    )
}

@Preview
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavRailPreview() {
    NavRailBar(
        extendedButtonData = NavExtendedButtonData(
            icon = { Icon(Icons.Filled.AddCircle, "") },
            label = { Text("TEST") },
            tooltipContent = { Text("TEST") },
            tooltipState = BasicTooltipState(),
            onClick = {}
        ), selected = { true }, onNavClick = {}
    )
}
