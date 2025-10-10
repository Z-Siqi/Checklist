package com.sqz.checklist.ui.main

import android.os.Build
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.MainLayoutNav
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.unit.isApi35AndAbove
import com.sqz.checklist.ui.common.unit.isGestureNavigationMode
import com.sqz.checklist.ui.theme.Theme

/**
 * Extended Button actions layout data for the Navigation Bar
 */
data class NavExtendedButtonData(
    val icon: @Composable () -> Unit = {},
    val label: @Composable () -> Unit = {},
    val tooltipContent: @Composable (@Composable () -> Unit) -> Unit = { it() },
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

private val colors: Theme @Composable get() = Theme.color

/** Navigation Bar **/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NavBar(
    extendedButtonData: NavExtendedButtonData,
    selected: (index: MainLayoutNav) -> Boolean,
    onNavClick: (index: MainLayoutNav) -> Unit,
    modifier: Modifier = Modifier
) {
    val heightLimit = if (!(isApi35AndAbove && isGestureNavigationMode())) Modifier else {
        Modifier.heightIn(max = 100.dp)
    }
    NavigationBar(
        modifier = modifier.shadow(
            elevation = 5.dp,
            ambientColor = MaterialTheme.colorScheme.secondary
        ) then heightLimit,
        containerColor = colors.navBarBgColor,
        contentColor = colors.navBarContentColor
    ) {
        Spacer(modifier = Modifier.weight(0.5f))
        items().forEachIndexed { index, item ->
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.navBarSelectedIconColor,
                    disabledIconColor = colors.navBarDisabledIconColor,
                    indicatorColor = colors.navBarItemColor
                ),
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.text,
                        modifier = Modifier.size(24.dp, 24.dp)
                    )
                },
                label = { Text(item.text) },
                selected = selected(selectedInNav(index)),
                onClick = { onNavClick(selectedInNav(index)) }
            )
        }
        Spacer(modifier = modifier.weight(0.5f))
        VerticalDivider(
            modifier = Modifier.height(50.dp), color = if (isSystemInDarkTheme()) {
                MaterialTheme.colorScheme.onSurface
            } else DividerDefaults.color
        )
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            extendedButtonData.tooltipContent {
                NavigationBarItem(
                    modifier = Modifier,
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
    val right = WindowInsets.displayCutout.asPaddingValues()
        .calculateRightPadding(LocalLayoutDirection.current)
    val safeWidthForFullscreen =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Modifier.width(right) else Modifier
    val bgColor = colors.navBarBgColor
    Row(modifier = modifier) {
        NavigationRail(
            modifier = Modifier,
            containerColor = bgColor,
            contentColor = colors.navBarContentColor,
            windowInsets = WindowInsets()
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            items().forEachIndexed { index, item ->
                NavigationRailItem(
                    modifier = Modifier.weight(1f),
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = colors.navBarItemColor,
                        selectedIconColor = colors.navBarSelectedIconColor,
                        disabledIconColor = colors.navBarDisabledIconColor
                    ),
                    icon = {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.text,
                            modifier = Modifier.size(24.dp, 24.dp)
                        )
                    },
                    label = { Text(item.text) },
                    selected = selected(selectedInNav(index)),
                    onClick = { onNavClick(selectedInNav(index)) }
                )
            }
            Spacer(modifier = Modifier.weight(0.5f))
            HorizontalDivider(
                modifier = Modifier.width(50.dp), color = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.onSurface
                } else DividerDefaults.color
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                extendedButtonData.tooltipContent {
                    NavigationRailItem(
                        modifier = Modifier,
                        colors = NavigationRailItemDefaults.colors(MaterialTheme.colorScheme.primary),
                        icon = extendedButtonData.icon,
                        label = extendedButtonData.label,
                        selected = false,
                        onClick = extendedButtonData.onClick
                    )
                }
            }
        }
        Spacer(modifier = safeWidthForFullscreen.fillMaxHeight() then Modifier.background(bgColor))
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
            tooltipContent = { TextTooltipBox(text = "TEST", content = it) },
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
            tooltipContent = { it() },
            onClick = {}
        ), selected = { true }, onNavClick = {}
    )
}
