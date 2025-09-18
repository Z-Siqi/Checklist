package com.sqz.checklist.ui.main

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sqz.checklist.R
import kotlinx.coroutines.delay

enum class OnClickType {
    ScrollDown, ScrollUp, Search
}

@Composable
fun NavTooltipContent(
    mode: NavMode,
    onClickType: (OnClickType) -> Unit,
    view: View,
    modifier: Modifier = Modifier,
    scrollUp: Boolean = false
) {
    OutlinedCard(
        shape = CircleShape,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceDim),
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright)
    ) {
        var onLongClickType by remember { mutableStateOf<OnClickType?>(null) }
        ButtonsLayout(
            modifier = modifier.padding(
                start = 8.dp, end = 8.dp, top = (8.5).dp, bottom = (8.5).dp
            ),
            mode = mode
        ) {
            NavButton(onClick = {
                onClickType(OnClickType.Search)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }, onLongClick = {
                onLongClickType = OnClickType.Search
            }) {
                if (onLongClickType == OnClickType.Search) Text(
                    text = stringResource(R.string.search),
                    lineHeight = 12.sp / view.context.resources.configuration.fontScale,
                    fontSize = 12.sp / view.context.resources.configuration.fontScale,
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Center
                ) else Icon(
                    modifier = modifier.size(25.dp),
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }
            Spacer(modifier = modifier.size(height = 8.dp, width = 8.dp))
            NavButton(onClick = {
                if (!scrollUp) onClickType(OnClickType.ScrollDown) else onClickType(OnClickType.ScrollUp)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }, onLongClick = {
                onLongClickType = OnClickType.ScrollDown
            }) {
                if (onLongClickType == OnClickType.ScrollDown) Text(
                    text = stringResource(if (!scrollUp) R.string.scroll_to_end else R.string.scroll_to_top),
                    lineHeight = 12.sp / view.context.resources.configuration.fontScale,
                    fontSize = 12.sp / view.context.resources.configuration.fontScale,
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Center
                ) else {
                    if (!scrollUp) Icon(
                        modifier = modifier.size(30.dp),
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.scroll_to_end)
                    ) else Icon(
                        modifier = modifier.size(30.dp),
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.scroll_to_top)
                    )
                }
            }
        }
        if (onLongClickType != null) LaunchedEffect(Unit) {
            delay(3500)
            onLongClickType = null
        }
    }
    var clickFeedback by rememberSaveable { mutableStateOf(false) }
    @Suppress("AssignedValueIsNeverRead")
    if (!clickFeedback) {
        createVibration(view)
        clickFeedback = true
    }
}

private fun createVibration(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.getSystemService(
        view.context, Vibrator::class.java
    )?.vibrate(
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    ) else view.playSoundEffect(SoundEffectConstants.CLICK)
}

@Composable
private fun ButtonsLayout(
    mode: NavMode,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = when (mode) {
    NavMode.Disable -> {}
    NavMode.NavBar -> Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        content = { content() }
    )

    NavMode.NavRail -> Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        content = { content() }
    )
}

@Composable
private fun NavButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.size(58.dp, 55.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .combinedClickable(onLongClick = onLongClick, onClick = onClick)
                .padding(top = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    NavTooltipContent(NavMode.NavBar, {}, LocalView.current)
}
