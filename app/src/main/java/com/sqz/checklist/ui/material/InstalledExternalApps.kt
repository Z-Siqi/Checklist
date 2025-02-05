package com.sqz.checklist.ui.material

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sqz.checklist.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

suspend fun getInstalledApps(context: Context): List<AppInfo> {
    return withContext(Dispatchers.IO) {
        val pm: PackageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)

        apps.map { resolveInfo ->
            AppInfo(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(pm)
            )
        }.filter { it.packageName != context.packageName }
    }
}

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

@Composable
fun ApplicationList(
    packageName: (String) -> Unit,
    defaultPackageName: String?,
    context: Context
) {
    val pm: PackageManager = context.packageManager
    var selectedAppInfo by rememberSaveable { mutableStateOf<AppInfo?>(null) }
    var appList by rememberSaveable { mutableStateOf<List<AppInfo>>(emptyList()) }
    if (appList.isEmpty()) LaunchedEffect(Unit) {
        appList = getInstalledApps(context)
    }
    Box {
        if (appList.isEmpty()) Text(
            stringResource(R.string.loading), modifier = Modifier.padding(8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { Spacer(Modifier.height(3.dp)) }
            items(appList) { app ->
                AppItem(
                    app, { selectedAppInfo = it },
                    selected = app.packageName == selectedAppInfo?.packageName && app.name == selectedAppInfo?.name
                )
            }
            item { Spacer(Modifier.height(3.dp)) }
        }
    }
    if (selectedAppInfo != null) packageName(selectedAppInfo!!.packageName)
    else LaunchedEffect(appList.isNotEmpty()) {
        try {
            if (defaultPackageName != null && defaultPackageName != "") {
                val intent = context.packageManager.getLaunchIntentForPackage(defaultPackageName)
                val appInfo = pm.getApplicationInfo(defaultPackageName, 0)
                if (intent != null) selectedAppInfo = AppInfo(
                    name = pm.getApplicationLabel(appInfo).toString(),
                    packageName = defaultPackageName,
                    icon = appInfo.loadIcon(pm)
                )
            }
        } catch (e: Exception) {
            Toast.makeText(
                context, context.getString(R.string.failed_found_package), Toast.LENGTH_SHORT
            ).show()
            Log.w("PackageName", "Failed to found saved package name! ERROR: $e")
        }
    }
}

@Composable
private fun AppItem(app: AppInfo, onClick: (appInfo: AppInfo) -> Unit, selected: Boolean) {
    Card(
        onClick = { onClick(app) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 3.dp, end = 3.dp, top = 1.dp, bottom = 1.dp),
        colors = CardDefaults.cardColors(Color.Transparent),
        shape = ShapeDefaults.Small,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(modifier = Modifier.padding(5.dp)) {
            val iconPainter: Painter =
                remember { BitmapPainter(app.icon.toBitmap().asImageBitmap()) }
            Image(
                painter = iconPainter,
                contentDescription = stringResource(R.string.app_icon),
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = app.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
