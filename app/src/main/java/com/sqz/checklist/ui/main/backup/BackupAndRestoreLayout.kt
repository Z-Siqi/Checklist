package com.sqz.checklist.ui.main.backup

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.database.ExportTaskDatabase
import com.sqz.checklist.database.GetUri
import com.sqz.checklist.database.ImportTaskDatabaseAction
import com.sqz.checklist.database.taskDatabaseName

@Composable
fun BackupAndRestoreLayout(
    view: View,
    modifier: Modifier = Modifier,
    dbPath: String = view.context.getDatabasePath(taskDatabaseName).absolutePath
) {
    val backupCardLayout: @Composable ColumnScope.() -> Unit = {
        var mode by rememberSaveable { mutableIntStateOf(0) }
        val list = listOf(
            ListData(0, "Export to file"), ListData(1, "Export by share")
        )
        Text(
            text = "Select a backup method",
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
        Button(modifier = modifier
            .padding(8.dp)
            .align(Alignment.End), onClick = {
            onClick = true
        }) {
            Text(text = "Export")
            ExportTaskDatabase(onClick, mode == 1, view, dbPath) {
                onClick = false
            }
        }
    }

    val restoreCardLayout: @Composable ColumnScope.() -> Unit = {
        Text(
            text = "Select a backup file to restore",
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
        var onClick by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
            onClick = { selectUri = true }
        ) {
            val text = if (uri == null) "Click to select a file to import" else {
                var selected by rememberSaveable { mutableStateOf("Selected!") }
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
        Button(modifier = modifier
            .padding(8.dp)
            .align(Alignment.End), onClick = {
            if (uri != null) onClick = true
        }) {
            if (onClick) {
                ImportTaskDatabaseAction(uri, view) {
                    Log.i("TEST", "$it")
                }
                onClick = false
            }
            Text(text = "Import")
        }
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        val cardModifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            TitleText(
                text = "Backup",
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedCard(
                modifier = cardModifier,
                content = backupCardLayout
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
            TitleText(
                text = "Restore",
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedCard(
                modifier = cardModifier,
                content = restoreCardLayout
            )
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

@Preview(showBackground = true)
@Composable
private fun Preview() {
    BackupAndRestoreLayout(LocalView.current, dbPath = "")
}
