package com.sqz.checklist.ui.main.task.layout

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.ui.main.task.CardHeight
import com.sqz.checklist.ui.material.TextTooltipBox
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/** TaskLayout Top App Bar **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLayoutTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    topBarState: TopAppBarState,
    onMenuClick: @Composable (setter: Boolean, getter: (Boolean) -> Unit) -> Unit,
    view: View,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    onMenuClick(menu) { menu = it } // able to add the menu UI inside this

    val screenHeight = LocalConfiguration.current.screenHeightDp
    val topBarForLowScreen = screenHeight <= (CardHeight + 24) * 3.8
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val topBarForLowAndWidthScreen = screenWidth > 800 && topBarForLowScreen

    val topAppBarTitle = stringResource(R.string.time_format)
    val year = "YYYY"
    val week = "EEEE"

    val title = @Composable {
        if (topBarState.heightOffset <= topBarState.heightOffsetLimit * 0.7) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = topBarContent(topAppBarTitle),
                    maxLines = 1,
                    modifier = modifier.padding(bottom = 1.dp),
                    overflow = TextOverflow.Visible
                )
                Text(
                    text = topBarContent(year),
                    maxLines = 1,
                    fontSize = 15.sp,
                    overflow = TextOverflow.Visible
                )
            }
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
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
                    overflow = TextOverflow.Visible
                )
                Spacer(modifier = modifier.width(10.dp))
                Text(
                    text = topBarContent(year),
                    modifier = modifier.height(28.dp),
                    maxLines = 1,
                    fontSize = 15.sp,
                    overflow = TextOverflow.Visible
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
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        titleContentColor = MaterialTheme.colorScheme.primary,
        scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    )

    if (topBarForLowScreen) TopAppBar(
        title = title,
        actions = { actionButton() },
        colors = colors
    ) else {
        MediumTopAppBar(
            colors = colors,
            title = title,
            actions = { actionButton() },
            scrollBehavior = scrollBehavior
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
                    modifier = modifier.padding(top = 22.dp, start = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(modifier = modifier.width(10.dp))
                    Text(
                        text = topBarContent(week),
                        maxLines = 1,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
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
        while(true) {
            delay(setUpdateWaitingTime)
            dateTime = LocalDate.now().format(formatter)
        }
    }
    return dateTime
}
