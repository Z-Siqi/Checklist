package com.sqz.checklist.ui.common

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

/**
 * Retrieves application information for a given package name.
 *
 * @param packageName The package name of the application.
 * @param context The application context.
 * @return An [AppInfo] object if the application is found and has a launch intent, otherwise null.
 */
fun getApp(packageName: String, context: Context): AppInfo? {
    val pm: PackageManager = context.packageManager
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    return if (intent == null) null else {
        val appInfo = pm.getApplicationInfo(packageName, 0)
        AppInfo(
            name = pm.getApplicationLabel(appInfo).toString(),
            packageName = packageName,
            icon = appInfo.loadIcon(pm)
        )
    }
}
