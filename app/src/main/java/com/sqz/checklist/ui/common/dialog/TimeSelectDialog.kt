package com.sqz.checklist.ui.common.dialog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import com.sqz.checklist.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: (cal: Long) -> Unit,
    onFailed: () -> Unit,
    view: View,
    modifier: Modifier = Modifier,
    allowDismissRequest: Boolean = true,
) {
    val context = view.context
    var isTimeInput by rememberSaveable { mutableStateOf(false) }
    var datePickDialog by rememberSaveable { mutableStateOf(false) }
    var isDatePick by rememberSaveable { mutableStateOf(false) }
    var rememberDays by rememberSaveable { mutableIntStateOf(0) }

    val timePickLimit: Boolean =
        !isLandscape() && getScreenHeightDpInt() < 580 || isLandscape() && getScreenWidthDpInt() < 595
    if (timePickLimit) LaunchedEffect(Unit) {
        Log.d("Limit", "Disabled some UI due to screen is too small!")
    }
    val timePickerState = rememberTimePickerState()
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
    cal.set(Calendar.MINUTE, timePickerState.minute)
    if (cal.before(now) && !isDatePick) {
        cal.add(Calendar.DATE, 1)
    }

    CheckPermission(context = context, onFailed = onFailed)
    val widthIn: Int = (getScreenWidthDpInt() * 0.8).toInt().let { if (it > 730) it else 630 }
    AlertDialog(
        modifier = modifier
            .widthIn(min = widthIn.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        onDismissRequest = { if (allowDismissRequest) onDismissRequest() },
        confirmButton = {
            Row(modifier = modifier.fillMaxWidth()) {
                if (!timePickLimit) IconButton(onClick = {
                    isTimeInput = !isTimeInput
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    fun icon(): Pair<Int, String> = if (isTimeInput)
                        Pair(R.drawable.schedule, context.getString(R.string.pick))
                    else Pair(R.drawable.keyboard, context.getString(R.string.input))
                    val (icon, type) = icon()
                    Icon(
                        painter = painterResource(id = icon), contentDescription = type,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = modifier.weight(1f))
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.dismiss))
                }
                Spacer(modifier = modifier.width(10.dp))
                TextButton(onClick = {
                    onConfirmClick((cal.timeInMillis - now.timeInMillis))
                }) {
                    Text(text = stringResource(R.string.confirm))
                }
            }
        },
        text = {
            Column(
                modifier = modifier.verticalScroll(rememberScrollState()) then if (isLandscape()) {
                    modifier.border(
                        2.dp, MaterialTheme.colorScheme.outlineVariant, ShapeDefaults.Medium
                    )
                } else modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = if (isLandscape()) modifier.padding(
                        start = 16.dp, end = 16.dp, top = 8.dp
                    ) else modifier,
                    propagateMinConstraints = false
                ) {
                    if (timePickLimit || isTimeInput) TimeInput(state = timePickerState) else {
                        TimePicker(
                            state = timePickerState,
                            layoutType = if (isLandscape()) TimePickerLayoutType.Horizontal else {
                                TimePickerLayoutType.Vertical
                            }
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            var enable by remember { mutableStateOf(false) }
                            LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                                if (!enable) enable = true else ContextCompat.getSystemService(
                                    context, Vibrator::class.java
                                )?.vibrate(
                                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = {
                        if (!isDatePick) {
                            datePickDialog = true
                        } else {
                            cal.clear()
                            rememberDays = 0
                            isDatePick = false
                            ContextCompat.getSystemService(context, Vibrator::class.java)?.vibrate(
                                VibrationEffect.createOneShot(8L, 70)
                            )
                        }
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    },
                    colors = if (isDatePick) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) else ButtonDefaults.outlinedButtonColors(),
                    shape = ShapeDefaults.Medium,
                    modifier = modifier.fillMaxWidth(0.7f)
                ) {
                    Text(text = stringResource(R.string.select_date))
                }
                Spacer(modifier = modifier.height(5.dp))

                val time = cal.timeInMillis - now.timeInMillis
                val (hour, minutes) = convertTime(time)
                Text(text = stringResource(R.string.remind_at_preview, hour, minutes))

                val remindTime = now.timeInMillis + time
                val fullDateShort = stringResource(R.string.full_date_short)
                val formatter = remember { SimpleDateFormat(fullDateShort, Locale.getDefault()) }
                Text(
                    text = stringResource(
                        R.string.remind_at, formatter.format(remindTime)
                    )
                )
                Spacer(modifier = if (isLandscape()) modifier.height(8.dp) else modifier)
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = !isLandscape())
    )
    if (datePickDialog) {
        DatePickDialog(
            onDismissRequest = {
                datePickDialog = false
                view.playSoundEffect(SoundEffectConstants.CLICK)
            },
            onConfirm = {
                isDatePick = true
                datePickDialog = false
                view.playSoundEffect(SoundEffectConstants.CLICK)
            },
            selectedDate = { rememberDays = it },
            view = view
        )
    }
    if (isDatePick && rememberDays <= 0) isDatePick = false
    if (isDatePick) {
        cal.add(Calendar.DATE, rememberDays)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    selectedDate: (day: Int) -> Unit,
    view: View,
    modifier: Modifier = Modifier
) {
    var invalid by remember { mutableStateOf(false) }
    var tooLarge by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    DatePickerDialog(
        modifier = modifier.verticalScroll(rememberScrollState()),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    if (!invalid) onConfirm()
                    if (tooLarge) dialog = true
                    if (invalid && !tooLarge) Toast.makeText(
                        view.context, getString(view.context, R.string.cannot_set_past),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                colors = if (invalid) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.outlineVariant)
                } else ButtonDefaults.textButtonColors()
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.dismiss))
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val datePickerState = rememberDatePickerState()
        if (datePickerState.selectedDateMillis != null) {
            val daysDiff by remember { // get selected days
                derivedStateOf {
                    val zone = ZoneId.systemDefault()
                    val selectedMillis = datePickerState.selectedDateMillis!!
                    val today: LocalDate = LocalDate.now(zone)
                    val selectedDate: LocalDate =
                        Instant.ofEpochMilli(selectedMillis).atZone(zone).toLocalDate()
                    ChronoUnit.DAYS.between(today, selectedDate).toInt()
                }
            }
            if (daysDiff < 0) { // if set in past
                invalid = true
                tooLarge = false
            } else if (daysDiff == 0) { // if is current day
                invalid = false
                tooLarge = false
                selectedDate(0)
            } else { // is a future date
                invalid = false
                if (daysDiff <= 180) selectedDate(daysDiff) else { // limit
                    invalid = true
                    tooLarge = true
                    selectedDate(daysDiff)
                }
            }
        }
        var old by remember { mutableStateOf(datePickerState.selectedDateMillis) }
        if (old != datePickerState.selectedDateMillis) LaunchedEffect(true) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            old = datePickerState.selectedDateMillis
        }
        if (dialog) {
            WarningAlertDialog(
                onDismissRequest = { dialog = false },
                onConfirmButtonClick = {
                    dialog = false
                    onConfirm()
                },
                textString = stringResource(R.string.setting_remind_warning)
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = if (getScreenHeightDpInt() < 565) {
                    datePickerState.displayMode = DisplayMode.Input
                    false
                } else {
                    datePickerState.displayMode = DisplayMode.Picker
                    true
                }
            )
        }
    }
}

private fun convertTime(milliseconds: Long): Pair<Int, Int> {
    val seconds = milliseconds / 1000L
    val minutes = seconds / 60L
    val hours = (minutes / 60L).toInt()
    val remainingMinutes = (minutes % 60).toInt()
    return Pair(hours, remainingMinutes)
}

@ReadOnlyComposable
@Composable
private fun isLandscape(): Boolean {
    val containerSize = LocalWindowInfo.current.containerSize
    return containerSize.width > (containerSize.height * 1.1)
}

@ReadOnlyComposable
@Composable
private fun getScreenHeightDpInt(): Int {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    return with(density) { containerSize.height.toDp().value.toInt() }
}

@ReadOnlyComposable
@Composable
private fun getScreenWidthDpInt(): Int {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    return with(density) { containerSize.width.toDp().value.toInt() }
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
                context, context.getString(R.string.no_set_reminder_permission),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(true) {
            when (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
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
private fun TimePreview() {
    TimeSelectDialog(
        onDismissRequest = {}, onConfirmClick = {}, onFailed = {},
        view = LocalView.current
    )
}

@Preview
@Composable
private fun DatePreview() {
    DatePickDialog({}, {}, {}, LocalView.current)
}
