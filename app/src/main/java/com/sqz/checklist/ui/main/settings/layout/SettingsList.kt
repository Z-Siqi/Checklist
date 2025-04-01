package com.sqz.checklist.ui.main.settings.layout

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sqz.checklist.R
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.material.TextTooltipBox
import com.sqz.checklist.ui.material.UrlText
import com.sqz.checklist.ui.material.dialog.EditableContentDialog
import com.sqz.checklist.ui.theme.unit.timeDisplay
import com.sqz.checklist.ui.material.verticalColumnScrollbar
import java.util.Locale

@Composable
fun settingsList(
    view: View, type: SettingsType? = null, height: (Int) -> Unit = {}
): List<SettingsItem> {
    val primaryPreferences = PrimaryPreferences(view.context)
    var heightDp by rememberSaveable { mutableIntStateOf(0) }
    val list = listOf(
        // History
        allowedNumberOfHistory(primaryPreferences, view),
        clearHistoryWhenLeaved(primaryPreferences, view),
        disableUndoButton(primaryPreferences, view),
        // Notification
        disableRemoveNotifyInReminded(primaryPreferences, view),
        disableNoScheduleExactAlarmNotice(primaryPreferences, view),
        recentlyRemindedKeepTime(primaryPreferences, view),
        removeNoticeInAutoDelReminded(primaryPreferences, view),
        // General
        pictureCompressionRate(primaryPreferences, view),
        videoCompressionRate(primaryPreferences, view),
        language(view),
        about(view),
        donate(view)
    )
    if (heightDp == 0) LaunchedEffect(Unit) {
        if (type == null) list.forEach { heightDp += it.heightDp } else {
            list.filter { it.type == type }.forEach { heightDp += it.heightDp }
        }
    }
    height(heightDp)
    return if (type == null) list else list.filter {
        it.type == type
    }
}

@Composable
private fun OptionText(
    text: String, maxHeight: Int, modifier: Modifier = Modifier, miniTitle: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.secondary, textAlign: TextAlign? = null,
    url: String? = null, view: View? = null
) {
    val width = LocalConfiguration.current.screenWidthDp
    val fontSize = when {
        miniTitle -> if (width >= 385) 14.sp else 11.sp
        else -> if (width >= 385) 15.sp else 12.sp
    }
    var overflow by remember { mutableStateOf(false) }
    TextTooltipBox(text = text, enable = overflow) {
        if (url == null || view == null) Text(
            text = text, fontSize = fontSize, fontWeight = FontWeight.SemiBold,
            modifier = modifier.sizeIn(maxWidth = (width * 0.7).dp, maxHeight = maxHeight.dp),
            lineHeight = (fontSize.value + 5.sp.value).sp, overflow = TextOverflow.Ellipsis,
            color = textColor, textAlign = textAlign, onTextLayout = {
                overflow = it.hasVisualOverflow
            }
        ) else UrlText(
            url = url, view = view, text = text, fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier.sizeIn(maxWidth = (width * 0.7).dp, maxHeight = maxHeight.dp),
            lineHeight = (fontSize.value + 5.sp.value).sp, overflow = TextOverflow.Ellipsis,
            color = textColor, textAlign = textAlign
        )
    }
}

@Composable
private fun segmentedButton(
    list: Array<out Any>, label: (Any) -> String, initSetter: Int,
    modifier: Modifier = Modifier,
): Int {
    var selectedIndex by remember { mutableIntStateOf(initSetter) }
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        list.forEachIndexed { index, item ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = list.size
                ),
                onClick = { selectedIndex = index },
                selected = index == selectedIndex,
                label = { Text(label(item)) }
            )
        }
    }
    return selectedIndex
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun vibrationEffect(view: View) {
    ContextCompat.getSystemService(view.context, Vibrator::class.java)?.vibrate(
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    )
}

@Composable
private fun disableRemoveNotifyInReminded(
    preferences: PrimaryPreferences, view: View
): SettingsItem {
    return SettingsItem(
        SettingsType.Notification, 64,
        stringResource(R.string.disable_remove_notify_in_reminded)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var setting by remember { mutableStateOf(preferences.disableRemoveNotifyInReminded()) }
            OptionText(it, 48)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = setting, onCheckedChange = {
                    setting = preferences.disableRemoveNotifyInReminded(it)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            )
        }
    }
}

@Composable
private fun disableNoScheduleExactAlarmNotice(
    preferences: PrimaryPreferences, view: View
): SettingsItem {
    return SettingsItem(
        SettingsType.Notification, 64,
        stringResource(R.string.disable_no_schedule_exact_alarm_notice)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var setting by remember { mutableStateOf(preferences.disableNoScheduleExactAlarmNotice()) }
            OptionText(it, 48)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = setting, onCheckedChange = {
                    setting = preferences.disableNoScheduleExactAlarmNotice(it)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            )
        }
    }
}

@Composable
private fun recentlyRemindedKeepTime(preferences: PrimaryPreferences, view: View): SettingsItem {
    data class TimeItem(val name: String, val value: Long)

    var setting by remember { mutableLongStateOf(preferences.recentlyRemindedKeepTime()) }
    var custom by remember { mutableStateOf(false) }
    val list = listOf(
        TimeItem(timeDisplay(10800L), 10800000L),
        TimeItem(timeDisplay(18000L), 18000000L),
        TimeItem(timeDisplay(43200L), 43200000L),
        TimeItem(timeDisplay(86400L), 86400000L),
        TimeItem(timeDisplay(604800L), 604800000L),
        TimeItem(stringResource(R.string.disable), 0L),
        TimeItem(stringResource(R.string.custom), -1L)
    )

    fun String.formatTime() = this.replace("1", "").replace(" ", "").replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
    }
    if (custom) {
        val segmentedButton = listOf(
            timeDisplay(60L).formatTime(), // Minute
            timeDisplay(3600L).formatTime(), // Hour
            timeDisplay(86400L).formatTime() // Day
        )
        val state = rememberTextFieldState()
        var isNumeric by remember { mutableStateOf(true) }
        var selectedIndex by remember { mutableIntStateOf(0) }
        EditableContentDialog(
            onDismissRequest = { custom = false }, confirm = {
                setting = preferences.recentlyRemindedKeepTime(
                    state.text.toString().toLong() * when (selectedIndex) {
                        0 -> 60 * 1000 // Minute
                        1 -> 60 * 60 * 1000 // Hour
                        2 -> 24 * 60 * 60 * 1000 // Day
                        else -> throw ArrayStoreException("Item not found!")
                    }
                )
                custom = false
            }, title = stringResource(R.string.custom),
            confirmText = stringResource(R.string.confirm),
            state = state, contentProperties = EditableContentDialog(extraContentBottom = {
                selectedIndex = segmentedButton(
                    list = segmentedButton.toTypedArray(), label = { label ->
                        segmentedButton.find { it == label } ?: "N/A"
                    }, initSetter = selectedIndex, Modifier.fillMaxWidth()
                )
            }), singleLine = true, numberOnly = true,
            disableConform = isNumeric, onDisableConformClick = {
                Toast.makeText(
                    view.context, view.context.getString(R.string.in_0_to_100_only),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        LaunchedEffect(state.text.toString()) {
            isNumeric = try {
                state.text.toString().toInt() !in 0..100
            } catch (e: NumberFormatException) {
                true
            }
        }
    }
    return SettingsItem(
        SettingsType.Notification, 80, stringResource(R.string.recently_reminded_keep_time)
    ) {
        var expanded by remember { mutableStateOf(false) }
        Row(
            Modifier.padding(8.dp) then Modifier.heightIn(min = 55.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var parentWidthDp by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            OptionText(it, 34)
            Spacer(modifier = Modifier.weight(1f))
            OutlinedCard(modifier = Modifier.sizeIn(
                85.dp, 40.dp, maxHeight = 60.dp
            ) then Modifier.onGloballyPositioned {
                val widthPx = it.size.width
                parentWidthDp = with(density) { widthPx.toDp() }
            }, shape = ShapeDefaults.ExtraSmall, border = BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline
            ), onClick = {
                expanded = !expanded
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) {
                Column(
                    Modifier.sizeIn(90.dp, 40.dp, 90.dp), verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = list.find { it.value == setting }?.name
                            ?: timeDisplay((setting * 0.001).toLong(), 3),
                        modifier = Modifier.widthIn(min = 85.dp), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp, fontSize = 15.sp
                    )
                }
                val scrollState = rememberScrollState()
                val screenHeight = LocalConfiguration.current.screenHeightDp
                DropdownMenu(expanded = expanded,
                    modifier = Modifier
                        .width(parentWidthDp)
                        .heightIn(min = 200.dp, max = (screenHeight / 2.1).dp)
                        .verticalColumnScrollbar(
                            scrollState = scrollState, width = 5.dp, scrollBarCornerRadius = 25f,
                            showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward,
                            scrollBarTrackColor = Color.Transparent,
                            scrollBarColor = MaterialTheme.colorScheme.outline,
                            endPadding = 25f, topBottomPadding = 25f
                        ),
                    scrollState = scrollState, onDismissRequest = { expanded = false }) {
                    list.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            if (it.value != -1L) {
                                setting = preferences.recentlyRemindedKeepTime(it.value)
                            } else custom = true
                            expanded = false
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun removeNoticeInAutoDelReminded(
    preferences: PrimaryPreferences, view: View
): SettingsItem {
    var setting by remember { mutableStateOf(preferences.removeNoticeInAutoDelReminded()) }
    return SettingsItem(
        SettingsType.Notification, 61,
        stringResource(R.string.remove_notice_in_auto_del_reminded)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OptionText(it, 45)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = setting, onCheckedChange = {
                    setting = preferences.removeNoticeInAutoDelReminded(it)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            )
        }
    }
}

@Composable
private fun allowedNumberOfHistory(preferences: PrimaryPreferences, view: View): SettingsItem {
    val content: @Composable (String) -> Unit = {
        var sliderPosition by remember {
            mutableFloatStateOf(preferences.allowedNumberOfHistory().toFloat())
        }
        var old by remember { mutableFloatStateOf(sliderPosition) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && old != sliderPosition) {
            old = sliderPosition
            vibrationEffect(view)
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
            OptionText(it, 34)
            val text = when (sliderPosition.toInt()) {
                0 -> stringResource(R.string.disable)
                21 -> stringResource(R.string.unlimited)
                else -> sliderPosition.toInt().toString()
            }
            Spacer(Modifier.weight(1f))
            OptionText(
                text, 34, Modifier.fillMaxWidth(),
                textColor = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center
            )
            Slider(
                value = sliderPosition, onValueChange = {
                    sliderPosition = preferences.allowedNumberOfHistory(
                        it.toInt()
                    ).toFloat()
                }, colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                ), modifier = Modifier.height(20.dp) then Modifier.padding(
                    start = 15.dp, end = 15.dp
                ), steps = 21, valueRange = 0f..21f
            )
            Spacer(Modifier.weight(1f))
        }
    }
    return SettingsItem(
        SettingsType.History, 100, stringResource(R.string.allowed_number_of_history)
    ) {
        Column(Modifier.heightIn(max = 100.dp)) { content(it) }
    }
}

@Composable
private fun clearHistoryWhenLeaved(preferences: PrimaryPreferences, view: View): SettingsItem {
    return SettingsItem(
        SettingsType.History, 64, stringResource(R.string.clear_history_when_leaved)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var setting by remember { mutableStateOf(preferences.clearHistoryWhenLeaved()) }
            OptionText(it, 48)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = setting, onCheckedChange = {
                    setting = preferences.clearHistoryWhenLeaved(it)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            )
        }
    }
}

@Composable
private fun disableUndoButton(preferences: PrimaryPreferences, view: View): SettingsItem {
    return SettingsItem(
        SettingsType.History, 64, stringResource(R.string.disable_undo_button)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var setting by remember { mutableStateOf(preferences.disableUndoButton()) }
            OptionText(it, 48)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = setting, onCheckedChange = {
                    setting = preferences.disableUndoButton(it)
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            )
        }
    }
}

private data class CompressionItem(val name: String, val value: Int)

@Composable
private fun pictureCompressionRate(preferences: PrimaryPreferences, view: View): SettingsItem {
    return SettingsItem(
        SettingsType.General, 100, stringResource(R.string.picture_compression_rate)
    ) {
        var setting by remember { mutableIntStateOf(preferences.pictureCompressionRate()) }
        val list = listOf(
            CompressionItem("100", 100), CompressionItem("90", 90),
            CompressionItem("80", 80), CompressionItem("70", 70),
            CompressionItem("60", 60), CompressionItem("50", 50),
            CompressionItem("40", 40), CompressionItem("30", 30),
            CompressionItem("20", 20), CompressionItem("10", 10),
            CompressionItem(stringResource(R.string.original), 0),
            CompressionItem(stringResource(R.string.custom), -1),
        )
        var expanded by remember { mutableStateOf(false) }
        val height = Modifier.height(80.dp)
        Row(Modifier.padding(8.dp) then height, verticalAlignment = Alignment.CenterVertically) {
            var parentWidthDp by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            Column(Modifier.heightIn(max = 70.dp)) {
                OptionText(it, 34)
                OptionText(
                    stringResource(R.string.compression_rate_describe), 50,
                    miniTitle = true, textColor = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedCard(
                modifier = Modifier
                    .sizeIn(
                        85.dp, 40.dp, maxHeight = 70.dp
                    ) then Modifier.onGloballyPositioned { coordinates ->
                    val widthPx = coordinates.size.width
                    parentWidthDp = with(density) { widthPx.toDp() }
                },
                shape = ShapeDefaults.ExtraSmall,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                onClick = {
                    expanded = !expanded
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            ) {
                var custom by remember { mutableStateOf(false) }
                Column(Modifier.sizeIn(85.dp, 40.dp), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = list.find { it.value == setting }?.name ?: setting.toString(),
                        modifier = Modifier.widthIn(min = 85.dp), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                val scrollState = rememberScrollState()
                val screenHeight = LocalConfiguration.current.screenHeightDp
                DropdownMenu(expanded = expanded,
                    modifier = Modifier
                        .width(parentWidthDp)
                        .heightIn(min = 200.dp, max = (screenHeight / 2.1).dp)
                        .verticalColumnScrollbar(
                            scrollState = scrollState, width = 5.dp, scrollBarCornerRadius = 25f,
                            showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward,
                            scrollBarTrackColor = Color.Transparent,
                            scrollBarColor = MaterialTheme.colorScheme.outline,
                            endPadding = 25f, topBottomPadding = 25f
                        ),
                    scrollState = scrollState, onDismissRequest = { expanded = false }) {
                    list.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            if (it.value != -1) {
                                setting = preferences.pictureCompressionRate(it.value)
                            } else custom = true
                            expanded = false
                        })
                    }
                }
                if (custom) {
                    val state = rememberTextFieldState()
                    var isNumeric by remember { mutableStateOf(true) }
                    EditableContentDialog(
                        onDismissRequest = { custom = false },
                        confirm = {
                            setting = preferences.pictureCompressionRate(
                                state.text.toString().toInt()
                            )
                            custom = false
                        },
                        title = stringResource(R.string.custom),
                        confirmText = stringResource(R.string.confirm),
                        state = state,
                        singleLine = true, numberOnly = true,
                        disableConform = isNumeric,
                        onDisableConformClick = {
                            Toast.makeText(
                                view.context, view.context.getString(R.string.in_0_to_100_only),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    LaunchedEffect(state.text.toString()) {
                        isNumeric = try {
                            state.text.toString().toInt() !in 0..100
                        } catch (e: NumberFormatException) {
                            true
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun videoCompressionRate(preferences: PrimaryPreferences, view: View): SettingsItem {
    return SettingsItem(
        SettingsType.General, 100, stringResource(R.string.video_compression_rate)
    ) {
        var setting by remember { mutableIntStateOf(preferences.videoCompressionRate()) }
        val list = listOf(
            CompressionItem("100", 100), CompressionItem("90", 90),
            CompressionItem("80", 80), CompressionItem("70", 70),
            CompressionItem("60", 60), CompressionItem("50", 50),
            CompressionItem("40", 40), CompressionItem("30", 30),
            CompressionItem("20", 20), CompressionItem("10", 10),
            CompressionItem(stringResource(R.string.original), 0),
            CompressionItem(stringResource(R.string.custom), -1),
        )
        var expanded by remember { mutableStateOf(false) }
        val height = Modifier.height(80.dp)
        Row(Modifier.padding(8.dp) then height, verticalAlignment = Alignment.CenterVertically) {
            var parentWidthDp by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            Column {
                OptionText(it, 34)
                OptionText(
                    stringResource(R.string.compression_rate_describe), 50,
                    miniTitle = true, textColor = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedCard(
                modifier = Modifier
                    .size(85.dp, 40.dp) then Modifier.onGloballyPositioned { coordinates ->
                    val widthPx = coordinates.size.width
                    parentWidthDp = with(density) { widthPx.toDp() }
                },
                shape = ShapeDefaults.ExtraSmall,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                onClick = {
                    expanded = !expanded
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            ) {
                var custom by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = list.find { it.value == setting }?.name ?: setting.toString(),
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                val scrollState = rememberScrollState()
                val screenHeight = LocalConfiguration.current.screenHeightDp
                DropdownMenu(expanded = expanded,
                    modifier = Modifier
                        .width(parentWidthDp)
                        .heightIn(min = 200.dp, max = (screenHeight / 2.1).dp)
                        .verticalColumnScrollbar(
                            scrollState = scrollState, width = 5.dp, scrollBarCornerRadius = 25f,
                            showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward,
                            scrollBarTrackColor = Color.Transparent,
                            scrollBarColor = MaterialTheme.colorScheme.outline,
                            endPadding = 25f, topBottomPadding = 25f
                        ),
                    scrollState = scrollState, onDismissRequest = { expanded = false }) {
                    list.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            if (it.value != -1) {
                                setting = preferences.videoCompressionRate(it.value)
                            } else custom = true
                            expanded = false
                        })
                    }
                }
                if (custom) {
                    val state = rememberTextFieldState()
                    var isNumeric by remember { mutableStateOf(true) }
                    EditableContentDialog(
                        onDismissRequest = { custom = false },
                        confirm = {
                            setting = preferences.videoCompressionRate(
                                state.text.toString().toInt()
                            )
                            custom = false
                        },
                        title = stringResource(R.string.custom),
                        confirmText = stringResource(R.string.confirm),
                        state = state,
                        singleLine = true, numberOnly = true,
                        disableConform = isNumeric,
                        onDisableConformClick = {
                            Toast.makeText(
                                view.context, view.context.getString(R.string.in_0_to_100_only),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    LaunchedEffect(state.text.toString()) {
                        isNumeric = try {
                            state.text.toString().toInt() !in 0..100
                        } catch (e: NumberFormatException) {
                            true
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun language(view: View): SettingsItem {
    val supportState = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val height = if (supportState) 81 else 88
    return SettingsItem(
        SettingsType.General, height, stringResource(R.string.language)
    ) {
        Card(
            colors = CardDefaults.cardColors(Color.Transparent),
            onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                if (supportState) {
                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data = Uri.fromParts("package", view.context.packageName, null)
                    }
                    view.context.startActivity(intent)
                } else Toast.makeText(
                    view.context, view.context.getString(R.string.unsupported_below_api_33),
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(8.dp) then Modifier.height((height - 16).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    OptionText(it, 33)
                    OptionText(
                        if (supportState) stringResource(R.string.click_select_language) else stringResource(
                            R.string.unsupported_api_must_higher_than_32
                        ), if (supportState) 32 else 39, miniTitle = true,
                        textColor = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(Modifier.weight(1f))
                val padding = Modifier.padding(end = 12.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(R.drawable.language), it, padding)
                    OptionText(stringResource(R.string.app_display_language), 28, padding, true)
                }
            }
        }
    }
}

@Composable
private fun about(view: View): SettingsItem {
    return SettingsItem(
        SettingsType.General, 83, stringResource(R.string.about)
    ) {
        val url = "https://github.com/Z-Siqi"
        val height = Modifier.height(67.dp)
        Row(Modifier.padding(8.dp) then height, verticalAlignment = Alignment.CenterVertically) {
            Column {
                OptionText(it, 35)
                Row {
                    OptionText(
                        stringResource(R.string.developed_by), 32, miniTitle = true
                    )
                    OptionText(
                        "Z-Siqi", 32, url = url, view = view,
                        textColor = MaterialTheme.colorScheme.primary, miniTitle = true
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton({
                view.playSoundEffect(SoundEffectConstants.CLICK)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$url/Checklist")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                view.context.startActivity(intent)
            }) {
                Icon(
                    painterResource(R.drawable.github_mark), "Github",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun donate(view: View): SettingsItem {
    val url = "https://github.com/sponsors/Z-Siqi"
    return SettingsItem(
        SettingsType.General, 80, stringResource(R.string.sponsor_me)
    ) {
        Card(
            colors = CardDefaults.cardColors(Color.Transparent),
            onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                view.context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(8.dp) then Modifier.height(68.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    OptionText(it, 34)
                    OptionText(
                        stringResource(R.string.donate_describe), 50,
                        miniTitle = true, textColor = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(painterResource(R.drawable.heart), it, Modifier.padding(end = 12.dp))
            }
        }
    }
}
