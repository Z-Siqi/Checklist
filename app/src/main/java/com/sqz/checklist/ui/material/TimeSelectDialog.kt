package com.sqz.checklist.ui.material

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.sqz.checklist.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: (cal: Long) -> Unit,
    onFailed: () -> Unit,
    context: Context
) {
    val timePickerState = rememberTimePickerState()
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
    cal.set(Calendar.MINUTE, timePickerState.minute)
    if (cal.before(now)) {
        cal.add(Calendar.DATE, 1)
    }
    CheckPermission(
        context = context,
        onFailed = onFailed
    )
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.dismiss))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmClick((cal.timeInMillis - now.timeInMillis))
            }) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
                val time = cal.timeInMillis - now.timeInMillis
                val (hour, minutes) = convertTime(time.toInt())
                Text(text = stringResource(R.string.remind_at_preview, hour, minutes))
                val remindTime = now.timeInMillis + time
                val fullDateShort = stringResource(R.string.full_date_short)
                val formatter = remember { SimpleDateFormat(fullDateShort, Locale.getDefault()) }
                Text(
                    text = stringResource(
                        R.string.remind_at,
                        formatter.format(remindTime)
                    )
                )
            }
        }
    )
}

private fun convertTime(milliseconds: Int): Pair<Int, Int> {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return Pair(hours, remainingMinutes)
}

@Composable
private fun CheckPermission(
    context: Context,
    onFailed: () -> Unit,
) {
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            onFailed()
            Toast.makeText(
                context,
                context.getString(R.string.no_set_reminder_permission),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(true) {
            when (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )) {
                PackageManager.PERMISSION_GRANTED -> {
                    Log.d("Permission invoked", "POST_NOTIFICATIONS")
                }

                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    TimeSelectDialog(
        onDismissRequest = {},
        onConfirmClick = {},
        onFailed = {},
        context = LocalContext.current
    )
}