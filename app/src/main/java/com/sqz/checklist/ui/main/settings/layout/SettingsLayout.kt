package com.sqz.checklist.ui.main.settings.layout

import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.settings.SettingsLayoutViewModel

/**
 * App Settings layout
 */
@Composable
fun SettingsLayout(
    viewModel: SettingsLayoutViewModel,
    view: View,
    modifier: Modifier = Modifier
) {
    var inSearchText by remember { mutableStateOf<String?>(null) }

    val localConfig = LocalConfiguration.current
    val screenIsWidth = localConfig.screenWidthDp > localConfig.screenHeightDp * 1.1
    val left = WindowInsets.displayCutout.asPaddingValues()
        .calculateLeftPadding(LocalLayoutDirection.current)
    val safePaddingForFullscreen = if (
        Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenIsWidth
    ) modifier.padding(
        start = left, end = if (left / 3 > 15.dp) 15.dp else left / 3
    ) else modifier
    val safeBottomForFullscreen =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenIsWidth
        ) (WindowInsets.navigationBars.getBottom(LocalDensity.current) / LocalDensity.current.density).dp else 10.dp
    val settingsList = @Composable {
        var currentHeight by remember { mutableIntStateOf(0) }
        val searchBarSpace = if (currentHeight > 55) (currentHeight + 10).dp else 55.dp
        Box(safePaddingForFullscreen) {
            if (inSearchText == null) LazyColumn {
                item { if (viewModel.getSearchState()) Spacer(Modifier.height(searchBarSpace)) }
                item { SubtitleText(stringResource(R.string.task_history)) }
                item {
                    var height by remember { mutableIntStateOf(0) }
                    val list = settingsList(view, SettingsType.History) { height = it }
                    OutlinedCard(Modifier.padding(10.dp) then Modifier.height(height.dp)) {
                        LazyColumn(userScrollEnabled = false) {
                            items(list) { it.Content() }
                        }
                    }
                }
                item { SubtitleText(stringResource(R.string.notification)) }
                item {
                    var height by remember { mutableIntStateOf(0) }
                    val list = settingsList(view, SettingsType.Notification) { height = it }
                    OutlinedCard(Modifier.padding(10.dp) then Modifier.height(height.dp)) {
                        LazyColumn(userScrollEnabled = false) {
                            items(list) { it.Content() }
                        }
                    }
                }
                item { SubtitleText(stringResource(R.string.general_settings)) }
                item {
                    var height by remember { mutableIntStateOf(0) }
                    val list = settingsList(view, SettingsType.General) { height = it }
                    OutlinedCard(Modifier.padding(10.dp) then Modifier.height(height.dp)) {
                        LazyColumn(userScrollEnabled = false) {
                            items(list) { it.Content() }
                        }
                    }
                }
                item { Spacer(modifier = modifier.height(2.dp + safeBottomForFullscreen)) }
            } else {
                val list = settingsList(view).filter {
                    val text = it.text.replace("\n", "").replace(Regex("[A-Z]")) { matchResult ->
                        matchResult.value.lowercase()
                    }
                    text.contains(inSearchText!!)
                }
                LazyColumn {
                    item { if (viewModel.getSearchState()) Spacer(Modifier.height(searchBarSpace)) }
                    items(list) { it.Content() }
                    item { Spacer(modifier = modifier.height(2.dp + safeBottomForFullscreen)) }
                }
            }
            inSearchText = if (viewModel.getSearchState()) searchBar { currentHeight = it } else {
                null
            }
        }
    }
    if (viewModel.getSearchState()) BackHandler {
        viewModel.resetSearchState()
    } else if (inSearchText != null) {
        inSearchText = null
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        content = settingsList
    )
}

@Composable
private fun searchBar(currentHeight: (Int) -> Unit = {}): String? {
    val textFieldState = rememberTextFieldState()
    Box {
        val density = LocalDensity.current
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp)
                .heightIn(min = 50.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    val heightPx = layoutCoordinates.size.height
                    currentHeight(with(density) { heightPx.toDp() }.value.toInt())
                },
            shape = ShapeDefaults.ExtraLarge
        ) {
            Row(Modifier.heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.padding(start = 10.dp),
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.search)
                )
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 9.dp, end = 9.dp, top = 10.dp, bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    state = textFieldState,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
    return if (textFieldState.text.isEmpty()) null else {
        textFieldState.text.toString()
    }
}

@Composable
private fun SubtitleText(text: String) {
    Text(
        text = text, modifier = Modifier.padding(start = 12.dp, top = 16.dp),
        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

class SettingsItem(
    val type: SettingsType,
    val heightDp: Int,
    val text: String,
    private val content: @Composable (String) -> Unit
) {
    @Composable
    fun Content() {
        this.content(this.text)
    }
}

enum class SettingsType { Notification, History, General }

@Preview
@Composable
private fun Preview() {
    val viewModel: SettingsLayoutViewModel = viewModel()
    SettingsLayout(viewModel, LocalView.current)
}
