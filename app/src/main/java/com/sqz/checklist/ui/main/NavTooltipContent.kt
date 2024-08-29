package com.sqz.checklist.ui.main

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sqz.checklist.R

@Composable
fun NavTooltipContent(
    onScrollDownClick: () -> Unit,
    onScrollUpClick: () -> Unit,
    view: View,
    modifier: Modifier = Modifier,
    scrollUp: Boolean = false
) {
    ElevatedCard(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = modifier.padding(
                start = 10.dp,
                end = 10.dp,
                top = 20.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = ShapeDefaults.Medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = modifier.size(55.dp, 52.dp)
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .clickable {
                            if (!scrollUp) onScrollDownClick() else onScrollUpClick()
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                        }
                        .padding(top = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(if (!scrollUp) R.string.scroll_to_end else R.string.scroll_to_top),
                        lineHeight = 12.sp,
                        fontSize = 10.sp,
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

@Preview(showBackground = true)
@Composable
private fun Preview() {
    NavTooltipContent({}, {}, LocalView.current)
}
