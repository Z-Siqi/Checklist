package com.sqz.checklist.ui.material

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

class ApplicationListSaver(
    val selectedAppInfo: MutableState<AppInfo?>,
    val appList: MutableState<List<AppInfo>>,
    val lazyListState: LazyListState
) {
    companion object {
        fun saver(context: Context) = Saver<AppInfo?, Any>(
            save = {
                if (it == null) null
                else listOf(it.name, it.packageName)
            },
            restore = {
                run {
                    val list = it as List<*>
                    val name = list[0] as String
                    val packageName = list[1] as String
                    val icon = try {
                        context.packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        context.getDrawable(android.R.drawable.sym_def_app_icon)!!
                    }
                    AppInfo(name, packageName, icon)
                }
            }
        )

        fun setter(packageName: String, context: Context): AppInfo? {
            return try {
                if (packageName != "") {
                    val pm: PackageManager = context.packageManager
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    if (intent != null)  AppInfo(
                        name = pm.getApplicationLabel(appInfo).toString(),
                        packageName = packageName,
                        icon = appInfo.loadIcon(pm)
                    ) else null
                } else null
            } catch (e: NameNotFoundException) {
                Toast.makeText(
                    context, context.getString(R.string.failed_found_package), Toast.LENGTH_SHORT
                ).show()
                null
            }
        }

        fun listSaver(context: Context): Saver<List<AppInfo>, Any> {
            return listSaver(
                save = { list ->
                    list.map { listOf(it.name, it.packageName) }
                },
                restore = { list ->
                    list.map {
                        val name = it[0]
                        val packageName = it[1]
                        val icon = try {
                            context.packageManager.getApplicationIcon(packageName)
                        } catch (e: Exception) {
                            context.getDrawable(android.R.drawable.sym_def_app_icon)!!
                        }
                        AppInfo(name, packageName, icon)
                    }
                }
            )
        }
    }
}

@Composable
fun rememberApplicationList(context: Context, setter: String? = null): ApplicationListSaver {
    return ApplicationListSaver(
        rememberSaveable(stateSaver = ApplicationListSaver.saver(context)) {
            if (setter != null) mutableStateOf(ApplicationListSaver.setter(setter, context))
            else mutableStateOf(null)
        }, rememberSaveable(stateSaver = ApplicationListSaver.listSaver(context)) {
            mutableStateOf(emptyList())
        }, rememberLazyListState()
    )
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
    saver: ApplicationListSaver,
    context: Context
) {
    var selectedAppInfo by saver.selectedAppInfo
    var appList by saver.appList
    var requestScroll by remember { mutableStateOf(false) }
    if (appList.isEmpty()) LaunchedEffect(Unit) {
        appList = getInstalledApps(context)
        requestScroll = true
    }
    Box {
        if (appList.isEmpty()) Text(
            stringResource(R.string.loading), modifier = Modifier.padding(8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize(), state = saver.lazyListState) {
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
    if (selectedAppInfo != null) {
        packageName(selectedAppInfo!!.packageName)
        if (requestScroll) LaunchedEffect(Unit) {
            delay(100)
            val index = appList.indexOfFirst { it.packageName == selectedAppInfo!!.packageName }
            saver.lazyListState.scrollToItem(index)
        }
    }
}

private suspend fun getInstalledApps(context: Context): List<AppInfo> {
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
