package com.sqz.checklist.ui.main

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sqz.checklist.R

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
    ElevatedCard(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright)
    ) {
        ButtonsLayout(
            modifier = modifier.padding(
                start = 8.dp,
                end = 8.dp,
                top = 10.dp,
                bottom = 10.dp
            ),
            mode = mode
        ) {
            NavButton(onClick = {
                onClickType(OnClickType.Search)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search)
                )
                Spacer(modifier = modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.search),
                    lineHeight = 12.sp / view.context.resources.configuration.fontScale,
                    fontSize = 12.sp / view.context.resources.configuration.fontScale,
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = modifier.size(height = 10.dp, width = 10.dp))
            NavButton(
                onClick = {
                    if (!scrollUp) onClickType(OnClickType.ScrollDown) else onClickType(OnClickType.ScrollUp)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            ) {
                Text(
                    text = stringResource(if (!scrollUp) R.string.scroll_to_end else R.string.scroll_to_top),
                    lineHeight = 12.sp / view.context.resources.configuration.fontScale,
                    fontSize = 12.sp / view.context.resources.configuration.fontScale,
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Center
                )
                if (!scrollUp) Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.scroll_to_end)
                ) else Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.scroll_to_top)
                )
            }
        }
    }
    var clickFeedback by rememberSaveable { mutableStateOf(false) }
    if (!clickFeedback) {
        val context = LocalContext.current
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextCompat.getSystemService(
            context, Vibrator::class.java
        )?.vibrate(
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        ) else view.playSoundEffect(SoundEffectConstants.CLICK)
        clickFeedback = true
    }
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
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = ShapeDefaults.Medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.size(58.dp, 55.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .clickable { onClick() }
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
