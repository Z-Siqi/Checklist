package com.sqz.checklist.presentation.settings.manage.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sqz.checklist.presentation.settings.manage.SettingsManageScope

/** This method expected to be called only within this package and its sub-packages. **/
@Suppress("UnusedReceiverParameter")
@Composable
internal fun SettingsManageScope.SettingText(
    text: String,
    modifier: Modifier = Modifier,
    supporting: Boolean = false,
    color: Color = if (supporting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = if (supporting) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = textAlign,
    )
}

/** This method expected to be called only within this package and its sub-packages. **/
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsManageScope.SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        SettingText(
            text = title,
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 160.dp),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** This method expected to be called only within this package and its sub-packages. **/
@Composable
internal fun SettingsManageScope.ClickSetting(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    trailingContent: @Composable () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        colors = CardDefaults.cardColors(Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SettingText(title)
                supportingText?.let { SettingText(text = it, supporting = true) }
            }
            trailingContent()
        }
    }
}

/** This method expected to be called only within this package and its sub-packages. **/
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsManageScope.DropdownSetting(
    title: String,
    selectedText: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 180.dp),
        ) {
            SettingText(title)
            supportingText?.let { SettingText(text = it, supporting = true) }
        }
        OutlinedCard(
            modifier = Modifier
                .sizeIn(minWidth = 85.dp, minHeight = 48.dp, maxWidth = 240.dp),
            shape = ShapeDefaults.ExtraSmall,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            onClick = { expanded = !expanded },
        ) {
            Text(
                text = selectedText,
                modifier = Modifier
                    .widthIn(min = 85.dp)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 85.dp, max = 280.dp),
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onOptionSelected(index)
                        },
                    )
                }
            }
        }
    }
}

/** This method expected to be called only within this package and its sub-packages. **/
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsManageScope.FlowChoiceSetting(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingText(title)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEachIndexed { index, option ->
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    label = { Text(option) },
                )
            }
        }
    }
}
