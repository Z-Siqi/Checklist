package com.sqz.checklist.ui.main.backup

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import com.sqz.checklist.R
import com.sqz.checklist.database.DatabaseIO
import com.sqz.checklist.database.ExportTaskDatabase
import com.sqz.checklist.database.IOdbState
import com.sqz.checklist.database.ImportTaskDatabase
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.ui.theme.unit.navBarsBottomDp
import com.sqz.checklist.ui.theme.unit.pxToDpInt
import com.sqz.checklist.ui.theme.unit.screenIsWidth

/**
 * Backup & Restore layout
 */
@Composable
fun BackupAndRestoreLayout(
    view: View,
    modifier: Modifier = Modifier,
    disableBackHandlerState: (Boolean) -> Unit = {}
) {
    val audioManager = view.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val cache = PreferencesInCache(view.context)

    val localConfig = LocalWindowInfo.current.containerSize
    val safePaddingForFullscreen = if (screenIsWidth()) modifier.padding(
        start = WindowInsets.displayCutout.asPaddingValues()
            .calculateLeftPadding(LocalLayoutDirection.current),
        end = WindowInsets.displayCutout.asPaddingValues()
            .calculateRightPadding(LocalLayoutDirection.current)
    ) else modifier

    var disableBackHandler by rememberSaveable { mutableStateOf(false) }
    if (disableBackHandler) BackHandler(enabled = true) {
        Toast.makeText(
            view.context, view.context.getString(R.string.back_disabled_notice), Toast.LENGTH_SHORT
        ).show()
    }
    disableBackHandlerState(disableBackHandler)
    var loadingState by rememberSaveable { mutableIntStateOf(0) }

    val backupCardLayout: @Composable ColumnScope.() -> Unit = {
        var mode by remember { mutableIntStateOf(cache.backupOption()) }
        val list = listOf(
            ListData(0, stringResource(R.string.export_to_file)),
            ListData(1, stringResource(R.string.export_by_share))
        )
        Text(
            text = stringResource(R.string.select_backup_method),
            modifier = Modifier
                .padding(start = 17.dp, top = 10.dp, bottom = 8.dp)
                .align(Alignment.Start),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            val density = LocalDensity.current
            var parentHeight by remember { mutableStateOf(1.dp) }
            list.forEach { index ->
                SegmentedButton(
                    selected = index.index == mode,
                    onClick = {
                        mode = cache.backupOption(index.index)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index.index, count = list.size
                    ),
                    modifier = modifier.heightIn(min = 42.dp)
                ) {
                    Text(
                        text = index.string,
                        modifier.heightIn(
                            min = parentHeight
                        ) then modifier.onGloballyPositioned { layoutCoordinates ->
                            val heightPx = layoutCoordinates.size.height
                            (with(density) { heightPx.toDp() }).let {
                                if (it > parentHeight) parentHeight = it
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        var onClick by rememberSaveable { mutableStateOf(false) }
        Button(modifier = Modifier
            .padding(8.dp)
            .align(Alignment.End), onClick = {
            clickFeedback(view, audioManager)
            onClick = true
        }) {
            Text(text = stringResource(R.string.export))
            if (onClick) ProcessingDialog(loadingState)
            ExportTaskDatabase(onClick, mode == 1, view) { state, loading ->
                loadingState = loading
                if (state == IOdbState.Error || state == IOdbState.Finished || loading == 100) {
                    onClick = false
                }
            }
        }
    }

    val backupOption: @Composable ColumnScope.() -> Unit = {
        var option by remember { mutableStateOf(cache.backupSettings()) }
        Row(modifier.padding(8.dp)) {
            Text(
                stringResource(R.string.backup_app_settings),
                modifier.align(Alignment.CenterVertically), fontWeight = FontWeight.Medium,
                fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier.weight(1f))
            Switch(checked = option, onCheckedChange = { option = cache.backupSettings(it) })
        }
    }

    val restoreCardLayout: @Composable ColumnScope.() -> Unit = {
        Text(
            text = stringResource(R.string.select_backup_file_restore),
            modifier = Modifier
                .padding(start = 17.dp, top = 10.dp, bottom = 12.dp)
                .align(Alignment.Start),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        var selectUri by remember { mutableStateOf(false) }
        val selectingText = view.context.getString(R.string.click_select_file_import)
        var selected by rememberSaveable { mutableStateOf(selectingText) }
        var onClick by rememberSaveable { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
            onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                selectUri = true
            }
        ) {
            Text(
                text = selected,
                modifier = Modifier.padding(
                    start = 5.dp, top = 3.dp, end = 5.dp, bottom = 3.dp
                ),
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(modifier = Modifier
            .padding(8.dp)
            .align(Alignment.End), onClick = {
            clickFeedback(view, audioManager)
            if (selected != selectingText) {
                onClick = true
            } else Toast.makeText(
                view.context, view.context.getString(R.string.select_file_to_import),
                Toast.LENGTH_SHORT
            ).show()
        }) {
            Text(text = stringResource(R.string.import_text))
            if (onClick) ProcessingDialog(loadingState)
            ImportTaskDatabase(
                selectClicked = selectUri, importClicked = onClick,
                selected = {
                    when (it) {
                        "" -> selected = view.context.getString(R.string.selected_file)
                        null -> {
                            selectUri = false
                        }

                        else -> selected = it
                    }
                    selectUri = false
                },
                dbState = { state, loading ->
                    loadingState = loading
                    disableBackHandler = state == IOdbState.Processing
                    when (state) {
                        IOdbState.Error -> {
                            Toast.makeText(
                                view.context, view.context.getString(R.string.failed_in_zip),
                                Toast.LENGTH_LONG
                            ).show()
                            DatabaseIO.instance(Application()).releaseMemory()
                        }

                        IOdbState.Finished -> Toast.makeText(
                            view.context, view.context.getString(R.string.finished),
                            Toast.LENGTH_SHORT
                        ).show()

                        else -> {}
                    }
                    if (state == IOdbState.Error || state == IOdbState.Finished || loading == 100) {
                        onClick = false
                        selected = selectingText
                    }
                },
                view = view
            )
        }
    }

    val restoreOption: @Composable ColumnScope.() -> Unit = {
        var option by remember { mutableStateOf(cache.restoreSettings()) }
        Row(modifier.padding(8.dp)) {
            Column(modifier.align(Alignment.CenterVertically)) {
                Text(
                    stringResource(R.string.restore_settings),
                    fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.restore_settings_describe),
                    modifier.widthIn(max = (localConfig.width.pxToDpInt() / 1.5).dp),
                    fontSize = 12.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier.weight(1f))
            Switch(checked = option, onCheckedChange = { option = cache.restoreSettings(it) })
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        val cardModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()) then safePaddingForFullscreen,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            TitleText(
                text = stringResource(R.string.backup),
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedCard(
                modifier = cardModifier,
                content = backupCardLayout
            )
            OutlinedCard(
                modifier = modifier.padding(16.dp, 1.dp, 16.dp, 24.dp),
                content = backupOption
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
            TitleText(
                text = stringResource(R.string.restore),
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedCard(
                modifier = cardModifier,
                content = restoreCardLayout
            )
            OutlinedCard(
                modifier = modifier.padding(16.dp, 1.dp, 16.dp, 16.dp),
                content = restoreOption
            )
            Spacer(modifier = modifier.height(navBarsBottomDp()))
        }
    }
}

private data class ListData(
    val index: Int,
    val string: String
)

@Composable
private fun TitleText(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    modifier = Modifier.padding(start = 17.dp, top = 12.dp) then modifier,
    color = MaterialTheme.colorScheme.tertiary,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp
)

@Composable
private fun ProcessingDialog(loading: Int) {
    AlertDialog(onDismissRequest = {}, confirmButton = {}, text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.padding(8.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.padding(5.dp))
            Text("$loading %")
            Text(stringResource(R.string.processing))
        }
    })
}

private fun clickFeedback(view: View, audioManager: AudioManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            val vibrate = view.context?.let { getSystemService(it, Vibrator::class.java) }
            vibrate?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else view.playSoundEffect(SoundEffectConstants.CLICK)
    } else {
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }
}

@Preview(showBackground = true, locale = "EN")
@Composable
private fun Preview() {
    BackupAndRestoreLayout(LocalView.current)
}
