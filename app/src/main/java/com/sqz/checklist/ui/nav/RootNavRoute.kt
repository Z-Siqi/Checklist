package com.sqz.checklist.ui.nav

import kotlinx.serialization.Serializable

@Serializable
sealed interface RootNavRoute {

    @Serializable
    data object Home : RootNavRoute

    @Serializable
    data object TaskHistory : RootNavRoute

    @Serializable
    data object Settings : RootNavRoute

    @Serializable
    data object BackupRestore : RootNavRoute
}
