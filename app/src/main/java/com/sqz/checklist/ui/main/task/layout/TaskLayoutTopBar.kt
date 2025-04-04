package com.sqz.checklist.ui.main.task.layout

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.task.cardHeight
import com.sqz.checklist.ui.material.TextTooltipBox
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/** TaskLayout Top App Bar **/
@SuppressLint("ComposableNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLayoutTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    topBarState: TopAppBarState,
    onMenuClick: @Composable (setter: Boolean, getter: (Boolean) -> Unit) -> Unit,
    view: View,
    modifier: Modifier = Modifier
): Boolean {
    var menu by remember { mutableStateOf(false) }
    onMenuClick(menu) { menu = it } // able to add the menu UI inside this

    val localConfig = LocalConfiguration.current
    val screenHeight = localConfig.screenHeightDp
    val topBarForLowScreen = screenHeight <= (cardHeight(view.context) + 24) * 3.8
    val screenWidth = localConfig.screenWidthDp
    val topBarForLowAndWidthScreen = screenWidth > 800 && topBarForLowScreen

    val screenIsWidth = localConfig.screenWidthDp > localConfig.screenHeightDp * 1.2
    val left = WindowInsets.displayCutout.asPaddingValues()
        .calculateLeftPadding(LocalLayoutDirection.current)
    val safePaddingForFullscreen = if (
        Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenIsWidth
    ) modifier.padding(start = left - if (left > 8.dp) 5.dp else 0.dp) else modifier

    val topAppBarTitle = stringResource(R.string.time_format)
    val year = "YYYY"
    val week = "EEEE"

    val uiMode = view.context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val textColor = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> MaterialTheme.colorScheme.primary
        uiMode.contrast > 0.3 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }

    val title = @Composable {
        if (topBarState.heightOffset <= topBarState.heightOffsetLimit * 0.7 && !topBarForLowScreen) {
            Row(
                modifier = safePaddingForFullscreen,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = topBarContent(topAppBarTitle),
                    maxLines = 1,
                    modifier = modifier.padding(bottom = 1.dp),
                    overflow = TextOverflow.Visible,
                    color = textColor
                )
                Text(
                    text = topBarContent(year),
                    maxLines = 1,
                    fontSize = 15.sp,
                    overflow = TextOverflow.Visible,
                    color = textColor
                )
            }
        } else {
            Row(
                modifier = safePaddingForFullscreen,
                verticalAlignment = Alignment.Bottom
            ) {
                val formatWithSize = if (topBarForLowScreen) {
                    if (topBarForLowAndWidthScreen) {
                        topBarContent(week + ", " + stringResource(R.string.top_bar_date))
                    } else topBarContent(topAppBarTitle)
                } else topBarContent(stringResource(R.string.top_bar_date))
                Text(
                    text = formatWithSize,
                    modifier = modifier.height(30.dp),
                    maxLines = 1,
                    fontSize = 24.sp,
                    overflow = TextOverflow.Visible,
                    color = textColor
                )
                Spacer(modifier = modifier.width(10.dp))
                Text(
                    text = topBarContent(year),
                    modifier = modifier.height(28.dp),
                    maxLines = 1,
                    fontSize = 15.sp,
                    overflow = TextOverflow.Visible,
                    color = textColor
                )
            }
        }
    }

    val actionButton = @Composable {
        TextTooltipBox(
            textRid = R.string.more_options,
            topRightExtraPadding = true
        ) {
            IconButton(onClick = {
                menu = true
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.more_options)
                )
            }
        }
    }

    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = if (topBarForLowScreen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        titleContentColor = MaterialTheme.colorScheme.primary,
        scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    )

    val shadow = modifier.shadow(
        elevation = 1.dp,
        ambientColor = MaterialTheme.colorScheme.primaryContainer
    )

    if (topBarForLowScreen) TopAppBar(
        title = title,
        actions = { actionButton() },
        colors = colors,
        scrollBehavior = scrollBehavior,
        modifier = shadow
    ) else {
        MediumTopAppBar(
            colors = colors,
            title = title,
            actions = { actionButton() },
            scrollBehavior = scrollBehavior,
            modifier = if (topBarState.heightOffset == topBarState.heightOffsetLimit) shadow else modifier
        )
        val visible = topBarState.heightOffset >= topBarState.heightOffsetLimit * 0.58
        if (topBarState.heightOffset != topBarState.heightOffsetLimit) {
            AnimatedVisibility(
                visible = visible,
                enter = expandHorizontally(
                    expandFrom = Alignment.CenterHorizontally
                ) + fadeIn(
                    initialAlpha = 0.3f
                ),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Row(
                    modifier = modifier.padding(
                        top = 22.dp,
                        start = 4.dp
                    ) then safePaddingForFullscreen,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(modifier = modifier.width(10.dp))
                    Text(
                        text = topBarContent(week),
                        maxLines = 1,
                        fontSize = 22.sp,
                        color = textColor,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
    return !topBarForLowScreen // return if top bar no need to scroll
}

@Composable
private fun topBarContent(pattern: String): String {
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    var dateTime by remember { mutableStateOf(LocalDate.now().format(formatter)) }
    LaunchedEffect(Unit) { // Auto update date time when date change
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val setUpdateWaitingTime = calendar.timeInMillis - System.currentTimeMillis()
        while (true) {
            delay(setUpdateWaitingTime)
            dateTime = LocalDate.now().format(formatter)
        }
    }
    return dateTime
}
