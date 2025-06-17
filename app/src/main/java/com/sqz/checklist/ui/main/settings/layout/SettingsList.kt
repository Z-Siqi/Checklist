package com.sqz.checklist.ui.main.settings.layout

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sqz.checklist.ui.material.TextTooltipBox
import com.sqz.checklist.ui.material.UrlText
import com.sqz.checklist.ui.material.verticalColumnScrollbar
import com.sqz.checklist.ui.theme.unit.pxToDpInt
import com.sqz.checklist.ui.theme.unit.screenIsWidth

open class SettingsList {

    @Composable
    protected fun OptionText(
        text: String, modifier: Modifier = Modifier, miniTitle: Boolean = false,
        textColor: Color = MaterialTheme.colorScheme.secondary, textAlign: TextAlign? = null,
        fullWidth: Boolean = false, url: String? = null, view: View? = null
    ) = Box {
        val config = LocalWindowInfo.current.containerSize
        val width = config.width.pxToDpInt()
        val sizeModifierWidth = if (screenIsWidth()) width * 0.5 else width * 0.7
        val fontSize = when {
            miniTitle -> if (width >= 385) 14.sp else 11.sp
            else -> if (width >= 385) 15.sp else 12.sp
        }
        var overflow by remember { mutableStateOf(false) }
        val sizeModifier = if (!fullWidth) modifier.sizeIn(
            maxWidth = sizeModifierWidth.dp
        ) else modifier
        TextTooltipBox(text = text, enable = overflow) {
            if (url == null || view == null) Text(
                text = text, fontSize = fontSize, fontWeight = FontWeight.SemiBold,
                modifier = sizeModifier, lineHeight = (fontSize.value + 5.sp.value).sp,
                overflow = TextOverflow.Ellipsis, color = textColor,
                textAlign = textAlign, onTextLayout = {
                    overflow = it.hasVisualOverflow
                }
            ) else UrlText(
                url = url, view = view, text = text, fontSize = fontSize,
                fontWeight = FontWeight.SemiBold, modifier = sizeModifier,
                lineHeight = (fontSize.value + 5.sp.value).sp, overflow = TextOverflow.Ellipsis,
                color = textColor, textAlign = textAlign
            )
        }
    }

    @Composable
    protected fun SwitchView(
        text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, view: View,
    ) = Row(
        Modifier.padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        OptionText(text)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = {
                onCheckedChange(it)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        )
    }

    @Composable
    protected fun ClickView(
        title: String,
        onClick: () -> Unit,
        view: View,
        icon: @Composable () -> Unit = {},
        text: String? = null,
        fullWidth: Boolean = false,
    ) = Card(
        onClick = {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                OptionText(title, fullWidth = fullWidth)
                if (text != null) OptionText(
                    text = text,
                    miniTitle = true,
                    textColor = MaterialTheme.colorScheme.outline,
                    fullWidth = fullWidth
                )
            }
            Spacer(Modifier.weight(1f))
            icon()
        }
    }

    @Composable
    protected fun DropdownMenuView(
        expanded: MutableState<Boolean>,
        title: String,
        menuShowText: String,
        dropdownMenuList: @Composable (ColumnScope.() -> Unit),
        view: View,
        text: String? = null,
    ) = Row(
        Modifier.padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        var parentWidthDp by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current
        Column {
            OptionText(title)
            if (text != null) OptionText(
                text = text, miniTitle = true, textColor = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedCard(
            modifier = Modifier
                .sizeIn(85.dp, 40.dp) then Modifier.onGloballyPositioned { coordinates ->
                val widthPx = coordinates.size.width
                parentWidthDp = with(density) { widthPx.toDp() }
            },
            shape = ShapeDefaults.ExtraSmall,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            onClick = {
                expanded.value = !expanded.value
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        ) {
            Column(
                Modifier.sizeIn(85.dp, 40.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = menuShowText,
                    modifier = Modifier.widthIn(min = 85.dp), textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
                val scrollState = rememberScrollState()
                val screenHeight = LocalWindowInfo.current.containerSize.height.pxToDpInt()
                DropdownMenu(
                    expanded = expanded.value,
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
                    scrollState = scrollState, onDismissRequest = { expanded.value = false },
                    content = dropdownMenuList
                )
            }
        }
    }

    @Composable
    protected fun segmentedButton(
        list: Array<out Any>, label: (Any) -> String, initSetter: Int, modifier: Modifier = Modifier
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
                    label = { Text(label(item), overflow = TextOverflow.Ellipsis, maxLines = 1) }
                )
            }
        }
        return selectedIndex
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    protected fun vibrationEffect(view: View) {
        ContextCompat.getSystemService(view.context, Vibrator::class.java)?.vibrate(
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        )
    }

    @Composable
    fun settingsList(
        view: View, type: SettingsType? = null
    ): List<SettingsItem> {
        val list = HistorySettingItems(
            view = view
        ).list() + NotificationSettingItems(
            view = view
        ).list() + GeneralSettingItems(
            view = view
        ).list()
        return if (type == null) list else list.filter {
            it.type == type
        }
    }
}
