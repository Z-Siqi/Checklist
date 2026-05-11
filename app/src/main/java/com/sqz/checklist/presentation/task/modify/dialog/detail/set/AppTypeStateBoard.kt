package com.sqz.checklist.presentation.task.modify.dialog.detail.set

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sqz.checklist.R
import com.sqz.checklist.common.device.isResourceReadyForHighPerformance
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.unit.pxToDp
import com.sqz.checklist.ui.common.unit.toDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sqz.checklist.common.EffectFeedback
import sqz.checklist.data.database.model.Platform
import sqz.checklist.task.api.TaskModify

/** This method expected to be called only within this package and its sub-packages. **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTypeStateBoard(
    view: android.view.View,
    clearFocusState: MutableState<Boolean>,
    appState: TaskModify.Detail.TypeState.Application,
    fromPlatform: Platform?,
    onStateChange: (TaskModify.Detail.TypeState.Application) -> Unit,
    isSmallScreenSize: Boolean,
    feedback: EffectFeedback,
) {
    val supportedPlatform = fromPlatform == Platform.Android
    var localAppInfoList by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused

    if (supportedPlatform) { // set application lists
        if (localAppInfoList.isEmpty()) LaunchedEffect(Unit) {
            localAppInfoList = getAppList(view.context)
        } else LaunchedEffect(isWindowFocused) {
            val updateWhenFocusChange = isResourceReadyForHighPerformance(view.context)
            if (isWindowFocused && updateWhenFocusChange) getAppList(view.context).let {
                if (localAppInfoList != it) {
                    localAppInfoList = it
                }
            }
        }
    }

    val searchQuery = rememberSaveable { mutableStateOf<String?>(null) }
    val filteredAppList = remember(localAppInfoList, (searchQuery.value ?: "")) {
        localAppInfoList.filter { (packageName, appName) ->
            val sq = searchQuery.value ?: ""
            appName.contains(sq, ignoreCase = true) ||
                    packageName.contains(sq, ignoreCase = true)
        }.toList()
    }

    Column {
        if (supportedPlatform && localAppInfoList.isNotEmpty()) {
            SearchBar(
                searchQuery = searchQuery,
                clearFocusState = clearFocusState,
                feedback = feedback,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardDefaults.shape
        ) {
            if (localAppInfoList.isEmpty()) LoadingCardContent(
                supportedPlatform = supportedPlatform,
                appState = appState,
                isSmallScreenSize = isSmallScreenSize,
            ) else {
                val savedPackage = appState.launchToken?.decodeToString()
                InstalledAppList(
                    onSelected = {
                        onStateChange(appState.copy(launchToken = it.encodeToByteArray()))
                        feedback.onClickEffect()
                    },
                    selectedPackage = savedPackage,
                    localAppList = filteredAppList,
                    view = view,
                    isSmallScreenSize = isSmallScreenSize,
                )
            }
        }
    }
}

/** @return `Map<packageName, appName>` **/
private suspend fun getAppList(context: Context) = withContext(Dispatchers.IO) {
    val pm: PackageManager = context.packageManager
    val intent =
        Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
    val apps = pm.queryIntentActivities(intent, 0)
    val list: List<Pair<String, String>> =
        apps.map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName.toString()
            val appName = resolveInfo.loadLabel(pm).toString()
            packageName to appName
        }.filter {
            it.first != context.packageName
        }
    val toMap: Map<String, String> = list.toMap()
    return@withContext toMap
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchBar(
    searchQuery: MutableState<String?>,
    clearFocusState: MutableState<Boolean>,
    feedback: EffectFeedback,
) = Row(modifier = Modifier.animateContentSize()) {
    val focus = LocalFocusManager.current
    if (searchQuery.value == null) {
        FilledTonalButton(onClick = {
            searchQuery.value = ""
            feedback.onClickEffect()
        }) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search)
            )
            Text(text = stringResource(R.string.search))
        }
    } else {
        val state = rememberTextFieldState()
        TextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            inputTransformation = InputTransformation.byValue { _, new ->
                searchQuery.value = new.toString()
                new
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search),
                )
            },
            trailingIcon = {
                IconButton(modifier = Modifier.padding(end = 4.dp), onClick = {
                    if (!searchQuery.value.isNullOrEmpty()) {
                        state.clearText()
                        searchQuery.value = ""
                    } else {
                        searchQuery.value = null
                    }
                    feedback.onClickEffect()
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.cancel),
                    )
                }
            },
            onKeyboardAction = { clearFocusState.value = true },
            shape = CircleShape,
            lineLimits = TextFieldLineLimits.SingleLine,
        )
    }
    LaunchedEffect(clearFocusState.value) {
        if (clearFocusState.value) {
            focus.clearFocus()
            clearFocusState.value = false
        }
    }
}

@Composable
private fun InstalledAppList(
    onSelected: (appInfo: String) -> Unit,
    selectedPackage: String?,
    localAppList: List<Pair<String, String>>,
    view: android.view.View,
    isSmallScreenSize: Boolean,
) {
    val windowSize = LocalWindowInfo.current.containerSize
    val heightLimit = windowSize.height.pxToDp().let {
        if (isSmallScreenSize) it else (it / 3.8f).let { dvd ->
            if (dvd < 200.dp) 210.dp else dvd
        }
    }
    var rememberIcon by remember { mutableStateOf(mapOf<String, Drawable>()) }
    val state = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .heightIn(max = heightLimit)
            .fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(items = localAppList, key = { it.first }) { appInfo ->
            val getIcon = rememberIcon[appInfo.first]

            if (getIcon == null) LaunchedEffect(appInfo.first) {
                getAppIcon(appInfo.first, view.context)?.let {
                    rememberIcon = rememberIcon + (appInfo.first to it)
                }
            }
            AppListItem(
                onClick = onSelected,
                appPackage = appInfo.first,
                appName = appInfo.second,
                appIcon = getIcon,
                selected = appInfo.first == selectedPackage
            )
        }
    }
    var requestScroll by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (selectedPackage == null || !requestScroll) {
            requestScroll = false
            return@LaunchedEffect
        }
        val index = localAppList.indexOfFirst { it.first == selectedPackage }
        if (index != -1) state.scrollToItem(index)
        requestScroll = false
    }
}

private suspend fun getAppIcon(
    packageName: String, context: Context
) = withContext(Dispatchers.IO) {
    val pm: PackageManager = context.packageManager
    // check app still installed
    context.packageManager.getLaunchIntentForPackage(packageName) ?: return@withContext null
    // get app icon
    val appInfo = pm.getApplicationInfo(packageName, 0)
    return@withContext appInfo.loadIcon(pm)
}

@Composable
private fun AppListItem(
    onClick: (appInfo: String) -> Unit,
    appPackage: String,
    appName: String,
    appIcon: Drawable?,
    selected: Boolean,
) = TextTooltipBox(text = appName + "\n\n" + appPackage) {
    val windowSize = LocalWindowInfo.current.containerSize
    val titleStyle = MaterialTheme.typography.bodyLarge
    val smallStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = TextUnit.Unspecified)
    val itemMaxHeight = (10.dp).let {
        val smallStyleH = smallStyle.fontSize.toDp() * 2
        val totalH = it + titleStyle.fontSize.toDp() + smallStyleH
        if (100.dp < totalH) 100.dp else totalH
    }
    Card(
        onClick = { onClick(appPackage) },
        modifier = Modifier
            .height(itemMaxHeight)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.Transparent),
        shape = ShapeDefaults.Small,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        val imgSizeModifier = (windowSize.width.pxToDp() / 1.168f).let {
            val size = if (it > 50.dp) 50.dp else it
            Modifier.sizeIn(maxHeight = size, maxWidth = size) then Modifier.fillMaxSize()
        }
        Row(modifier = Modifier.padding(5.dp)) {
            if (appIcon != null) Image(
                painter = remember { BitmapPainter(appIcon.toBitmap().asImageBitmap()) },
                contentDescription = stringResource(R.string.app_icon),
                modifier = imgSizeModifier
            ) else Image(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.app_icon),
                modifier = imgSizeModifier
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = appName, style = titleStyle, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = appPackage, style = smallStyle,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = smallStyle.fontSize / 2,
                        maxFontSize = smallStyle.fontSize,
                    ),
                    maxLines = 2, overflow = TextOverflow.MiddleEllipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingCardContent(
    supportedPlatform: Boolean,
    appState: TaskModify.Detail.TypeState.Application,
    isSmallScreenSize: Boolean,
) {
    val heightModifier = Modifier.let { if (isSmallScreenSize) it else it.heightIn(min = 56.dp) }
    val textStyle = MaterialTheme.typography.labelLarge
    Column(
        modifier = heightModifier
            .fillMaxWidth()
            .padding(5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (supportedPlatform) {
            LoadingIndicator()
        }
        val text = supportedPlatform.let {
            if (it) return@let stringResource(R.string.loading) else {
                val wrong = "Unable to load due to saved Application not belong to this platform:"
                return@let "$wrong\n${appState.launchToken!!.decodeToString()}"
            }
        }
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = textStyle.copy(color = MaterialTheme.colorScheme.outline)
        )
    }
}
