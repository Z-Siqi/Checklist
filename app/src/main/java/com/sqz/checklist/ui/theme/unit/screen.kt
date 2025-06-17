package com.sqz.checklist.ui.theme.unit

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
fun screenIsWidth(): Boolean {
    val localConfig = LocalWindowInfo.current.containerSize
    return localConfig.width.pxToDpInt() > localConfig.height.pxToDpInt() * 1.1
}

@Composable
fun screenIsWidthAndAPI34(): Boolean {
    return Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenIsWidth()
}
