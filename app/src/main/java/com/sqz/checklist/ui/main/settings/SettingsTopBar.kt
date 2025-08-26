package com.sqz.checklist.ui.main.settings

import android.os.Build
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    onBack: () -> Unit,
    view: View,
    modifier: Modifier = Modifier
) {
    val localConfig = LocalWindowInfo.current.containerSize
    val screenIsWidth = localConfig.width > localConfig.height * 1.2
    val left = WindowInsets.displayCutout.asPaddingValues()
        .calculateLeftPadding(LocalLayoutDirection.current)
    val safePaddingForFullscreen = if (
        Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenIsWidth
    ) modifier.padding(start = left - if (left > 20.dp) 10.dp else 0.dp) else modifier
    val colors = Theme.color
    TopAppBar(
        colors = colors.topBarBgColors(screenIsWidth),
        title = { Text(text = stringResource(R.string.settings)) },
        modifier = modifier.shadow(
            elevation = 1.dp,
            ambientColor = MaterialTheme.colorScheme.primaryContainer
        ),
        navigationIcon = {
            TextTooltipBox(
                textRid = R.string.back,
                topLeftExtraPadding = true,
            ) {
                IconButton(onClick = {
                    onBack()
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }, modifier = safePaddingForFullscreen) {
                    Icon(
                        painter = painterResource(R.drawable.back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        }
    )
}
