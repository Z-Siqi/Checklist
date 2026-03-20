package com.sqz.checklist.ui.common.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqz.checklist.R

/**
 * This Dialog show a loading view, and showing forever in the method which call this.
 *
 * Example:
 * ```
 * if (processState.value) ProcessingDialog {
 *   // Process action
 *   processState.value = false
 * }
 * ```
 *
 * @param modifier The modifier to be applied to the layout.
 * @param loadingPercentage The percentage of loading. If `null`, the loading view will
 *   be [LoadingIndicator], otherwise using [CircularProgressIndicator].
 * @param processText The text to notice user what processing, default as "Processing..."
 * @param run Selectable lambda to execute the process action.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessingDialog(
    modifier: Modifier = Modifier,
    loadingPercentage: Double? = null,
    processText: String? = stringResource(R.string.processing),
    run: () -> Unit = {}
) {
    AlertDialog(onDismissRequest = {}, confirmButton = {}, modifier = modifier, text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.padding(8.dp))
            if (loadingPercentage == null) {
                LoadingIndicator()
            } else {
                CircularProgressIndicator()
            }
            if (loadingPercentage != null || processText != null) {
                Spacer(modifier = Modifier.padding(5.dp))
            }
            loadingPercentage?.let { Text("$it %") }
            processText?.let { Text(it) }
        }
    })
    run()
}

@Preview
@Composable
private fun Preview() {
    ProcessingDialog()
}
