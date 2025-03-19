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
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.sqz.checklist.ui.material.UrlText
import com.sqz.checklist.ui.material.dialog.EditableContentDialog
import com.sqz.checklist.ui.material.verticalColumnScrollbar

@Composable
fun settingsList(
    view: View, type: SettingsType? = null, height: (Int) -> Unit = {}
): List<SettingsItem> {
    val primaryPreferences = PrimaryPreferences(view.context)
    var heightDp by rememberSaveable { mutableIntStateOf(0) }
    val supportState = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val list = listOf(
        SettingsItem(
            SettingsType.Notification, 64,
            stringResource(R.string.disable_remove_notify_in_reminded)
        ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var setting by remember { mutableStateOf(primaryPreferences.disableRemoveNotifyInReminded()) }
                OptionText(it, 64)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = setting, onCheckedChange = {
                        setting = primaryPreferences.disableRemoveNotifyInReminded(it)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    }
                )
            }
        },
        SettingsItem(
            SettingsType.Notification, 64,
            stringResource(R.string.disable_no_schedule_exact_alarm_notice)
        ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var setting by remember { mutableStateOf(primaryPreferences.disableNoScheduleExactAlarmNotice()) }
                OptionText(it, 64)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = setting, onCheckedChange = {
                        setting = primaryPreferences.disableNoScheduleExactAlarmNotice(it)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    }
                )
            }
        },
        SettingsItem(
            SettingsType.History, 90, stringResource(R.string.allowed_number_of_history)
        ) {
            var sliderPosition by remember {
                mutableFloatStateOf(primaryPreferences.allowedNumberOfHistory().toFloat())
            }
            var old by remember { mutableFloatStateOf(sliderPosition) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && old != sliderPosition) {
                old = sliderPosition
                vibrationEffect(view)
            }
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
                OptionText(it, 90)
                val text = when (sliderPosition.toInt()) {
                    0 -> stringResource(R.string.disable)
                    21 -> stringResource(R.string.unlimited)
                    else -> sliderPosition.toInt().toString()
                }
                OptionText(
                    text, 20, Modifier.fillMaxWidth(),
                    textColor = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center
                )
                Slider(
                    value = sliderPosition, onValueChange = {
                        sliderPosition = primaryPreferences.allowedNumberOfHistory(
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
            }
        },
        SettingsItem(
            SettingsType.General, 70, stringResource(R.string.picture_compression_rate)
        ) {
            var setting by remember { mutableIntStateOf(primaryPreferences.pictureCompressionRate()) }
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
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var parentWidthDp by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                Column {
                    OptionText(it, 20)
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
                                scrollState = scrollState,
                                width = 5.dp,
                                scrollBarCornerRadius = 25f,
                                showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward,
                                scrollBarTrackColor = Color.Transparent,
                                scrollBarColor = MaterialTheme.colorScheme.outline,
                                endPadding = 25f,
                                topBottomPadding = 25f
                            ),
                        scrollState = scrollState, onDismissRequest = { expanded = false }) {
                        list.forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = {
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                if (it.value != -1) {
                                    setting = primaryPreferences.pictureCompressionRate(it.value)
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
                                setting = primaryPreferences.pictureCompressionRate(
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
        },
        SettingsItem(
            SettingsType.General, if (supportState) 60 else 64, stringResource(R.string.language)
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
                modifier = Modifier.fillMaxWidth() then Modifier.height(60.dp)
            ) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        OptionText(it, 20)
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
                        OptionText(stringResource(R.string.app_display_language), 18, padding, true)
                    }
                }
            }
        },
        SettingsItem(
            SettingsType.General, 64, stringResource(R.string.about)
        ) {
            val url = "https://github.com/Z-Siqi"
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    OptionText(it, 34)
                    Row {
                        OptionText(stringResource(R.string.developed_by), 30)
                        OptionText(
                            "Z-Siqi", 30, url = url, view = view,
                            textColor = MaterialTheme.colorScheme.primary
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

private data class CompressionItem(val name: String, val value: Int)

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
    if (url == null || view == null) Text(
        text = text, fontSize = fontSize, fontWeight = FontWeight.SemiBold,
        modifier = modifier.sizeIn(maxWidth = (width * 0.7).dp, maxHeight = maxHeight.dp),
        lineHeight = (fontSize.value + 5.sp.value).sp, overflow = TextOverflow.Ellipsis,
        color = textColor, textAlign = textAlign
    ) else UrlText(
        url = url, view = view, text = text, fontSize = fontSize, fontWeight = FontWeight.SemiBold,
        modifier = modifier.sizeIn(maxWidth = (width * 0.7).dp, maxHeight = maxHeight.dp),
        lineHeight = (fontSize.value + 5.sp.value).sp, overflow = TextOverflow.Ellipsis,
        color = textColor, textAlign = textAlign
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun vibrationEffect(view: View) {
    ContextCompat.getSystemService(view.context, Vibrator::class.java)?.vibrate(
        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    )
}
