package com.sqz.checklist.ui.main.backup

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import com.sqz.checklist.R
import com.sqz.checklist.database.ExportTaskDatabase
import com.sqz.checklist.database.GetUri
import com.sqz.checklist.database.IOdbState
import com.sqz.checklist.database.ImportTaskDatabaseAction
import com.sqz.checklist.database.taskDatabaseName

/**
 * Backup & Restore layout
 */
@Composable
fun BackupAndRestoreLayout(
    view: View,
    modifier: Modifier = Modifier,
    dbPath: String = view.context.getDatabasePath(taskDatabaseName).absolutePath,
    disableBackHandlerState: (Boolean) -> Unit = {}
) {
    val audioManager = view.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val localConfig = LocalConfiguration.current
    val screenIsWidth = localConfig.screenWidthDp > localConfig.screenHeightDp * 1.2
    val safePaddingForFullscreen = if (screenIsWidth) modifier.padding(
        start = WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LocalLayoutDirection.current),
        end = WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(LocalLayoutDirection.current)
    ) else modifier

    var disableBackHandler by rememberSaveable { mutableStateOf(false) }
    if (disableBackHandler) BackHandler(enabled = true) {
        Toast.makeText(
            view.context, view.context.getString(R.string.back_disabled_notice), Toast.LENGTH_SHORT
        ).show()
    }
    disableBackHandlerState(disableBackHandler)

    val backupCardLayout: @Composable ColumnScope.() -> Unit = {
        var mode by rememberSaveable { mutableIntStateOf(0) }
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
                .height(42.dp)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            list.forEach { index ->
                SegmentedButton(
                    selected = index.index == mode,
                    onClick = {
                        mode = index.index
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index.index, count = list.size
                    ),
                    modifier = Modifier.fillMaxSize()
                ) { Text(text = index.string) }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        var onClick by remember { mutableStateOf(false) }
        Button(modifier = Modifier
            .padding(8.dp)
            .align(Alignment.End), onClick = {
            clickFeedback(view, audioManager)
            onClick = true
        }) {
            Text(text = stringResource(R.string.export))
            ExportTaskDatabase(onClick, mode == 1, view, dbPath) {
                onClick = false
            }
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
        var uri by rememberSaveable { mutableStateOf<Uri?>(null) }
        if (selectUri) GetUri {
            uri = it
            selectUri = false
        }
        var onClick by rememberSaveable { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
            onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                selectUri = true
            }
        ) {
            val text = if (uri == null) stringResource(R.string.click_select_file_import) else {
                var selected by rememberSaveable { mutableStateOf(view.context.getString(R.string.selected_file)) }
                try {
                    selected = uri!!.path.toString().replace("/document/primary:", "")
                } catch (e: Exception) {
                    Log.w("ChecklistDataImport", "Failed to show selected file: $e")
                }
                selected
            }
            Text(
                text = text,
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
            if (uri != null) onClick = true else Toast.makeText(
                view.context, view.context.getString(R.string.select_file_to_import),
                Toast.LENGTH_SHORT
            ).show()
        }) {
            Text(text = stringResource(R.string.import_text))
            if (onClick) ImportTaskDatabaseAction(uri, view) {
                if (it == IOdbState.Error) Toast.makeText(
                    view.context, view.context.getString(R.string.error_try_undo_import),
                    Toast.LENGTH_SHORT
                ).show()
                disableBackHandler = it == IOdbState.Processing
                if (it == IOdbState.Finished) Toast.makeText(
                    view.context, view.context.getString(R.string.finished), Toast.LENGTH_SHORT
                ).show()
                if (it == IOdbState.Finished || it == IOdbState.Error) onClick = false
            }
        }
    }

    val safeBottomForFullscreen =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenIsWidth
        ) (WindowInsets.navigationBars.getBottom(LocalDensity.current) / LocalDensity.current.density).dp else 10.dp
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        val cardModifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
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
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
            TitleText(
                text = stringResource(R.string.restore),
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedCard(
                modifier = cardModifier,
                content = restoreCardLayout
            )
            Spacer(modifier = modifier.height(safeBottomForFullscreen))
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
    BackupAndRestoreLayout(LocalView.current, dbPath = "")
}
