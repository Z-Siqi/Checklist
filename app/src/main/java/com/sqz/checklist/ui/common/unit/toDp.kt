package com.sqz.checklist.ui.common.unit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@ReadOnlyComposable
@Composable
fun Int.pxToDp(): Dp {
    val it = this
    val density = LocalDensity.current
    return with(density) { it.toDp() }
}

@ReadOnlyComposable
@Composable
fun Int.pxToDpInt(): Int {
    return this.pxToDp().value.toInt()
}
