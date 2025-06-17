package com.sqz.checklist.ui.theme.unit

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun Int.pxToDp(): Dp {
    val it = this
    val density = LocalDensity.current
    return with(density) { it.toDp() }
}

@Composable
fun Int.pxToDpInt(): Int {
    return this.pxToDp().value.toInt()
}
