package com.sqz.checklist.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ContentScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    navigationRail: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = WindowInsets(),
    content: @Composable (paddingValues: PaddingValues) -> Unit
) = Row(modifier = modifier) {
    Surface(
        modifier = Modifier.weight(1f)
    ) {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            contentWindowInsets = contentWindowInsets,
        ) { paddingValues ->
            Surface(modifier = Modifier.padding(paddingValues)) {
                content(paddingValues)
            }
        }
    }
    Surface { navigationRail() }
}
