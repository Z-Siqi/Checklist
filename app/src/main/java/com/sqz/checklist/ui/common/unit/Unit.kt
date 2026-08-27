package com.sqz.checklist.ui.common.unit

import android.os.Build

/** Android 15 and above return true **/
val isApi35AndAbove = Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE

/** Android 10 and above return true **/
val isApi29AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

/** Android 12 and above return true **/
val isApi31AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Android 13 and above return true **/
val isApi33AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Default task card height in (dp)
 */
const val CardHeight = 100
