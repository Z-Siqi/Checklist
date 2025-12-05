package com.sqz.checklist.ui.common.dialog

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.sqz.checklist.ui.common.unit.pxToDp
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import kotlinx.coroutines.launch

/**
 * DialogWithMenu
 * @param functionalContent Do NOT use rememberSavable or long-term data hold param into here
 */
@Composable
fun DialogWithMenu(
    onDismissRequest: () -> Unit,
    confirm: (type: Any?) -> Unit,
    menuListGetter: Array<out Any>,
    menuText: @Composable (Any?) -> String,
    functionalContent: @Composable (type: Any?) -> Unit,
    modifier: Modifier = Modifier,
    defaultType: Any? = null,
    onDismissClick: () -> Unit = onDismissRequest,
    dialogWithMenuView: DialogWithMenuView,
    currentMenuSelection: (selected: Any?) -> Unit = {},
    view: View = LocalView.current,
) {
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var type by rememberSaveable { mutableStateOf<Any?>(null) }
    if (defaultType != null) LaunchedEffect(Unit) {
        type = defaultType
    }

    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val isLandScape =
        containerSize.width > containerSize.height && containerSize.width.pxToDpInt() > 400
    val shortScreen = containerSize.height.pxToDpInt() <= 358
    val selectContent = @Composable {
        OutlinedCard(
            modifier = modifier.heightIn(min = 47.dp) then if (isLandScape) modifier.width((containerSize.width.pxToDpInt() * 0.2).dp) else modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
            shape = ShapeDefaults.Small
        ) {
            var parentWidthDp by remember { mutableStateOf(0.dp) }
            Column(
                modifier = Modifier
                    .heightIn(min = 47.dp)
                    .fillMaxWidth()
                    .clickable {
                        expanded = !expanded
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    } then modifier.onGloballyPositioned { layoutCoordinates ->
                    val widthPx = layoutCoordinates.size.width
                    parentWidthDp = with(density) { widthPx.toDp() }
                }, verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier
                        .heightIn(max = (containerSize.height * 0.17f).pxToDp())
                        .padding(start = 12.dp, end = 12.dp),
                    text = menuText(type),
                    fontSize = 18.sp,
                    maxLines = 2,
                    autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val scrollState = rememberScrollState()
                DropdownMenu(
                    expanded = expanded,
                    modifier = Modifier
                        .width(parentWidthDp)
                        .heightIn(min = 80.dp, max = 200.dp)
                        .verticalColumnScrollbar(
                            scrollState = scrollState, width = 5.dp, scrollBarCornerRadius = 25f,
                            scrollBarTrackColor = Color.Transparent,
                            scrollBarColor = MaterialTheme.colorScheme.outline,
                            endPadding = 28f, topBottomPadding = 28f
                        ),
                    scrollState = scrollState, onDismissRequest = { expanded = false }) {
                    menuListGetter.forEach {
                        DropdownMenuItem(onClick = {
                            type = it
                            currentMenuSelection(it)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            expanded = false
                        }, text = { Text(text = menuText(it)) })
                    }
                }
            }
        }
    }
    val functionalContent = @Composable {
        val screenHeightDp = containerSize.height.pxToDpInt()
        val height = when {
            isLandScape -> screenHeightDp
            screenHeightDp >= 700 -> (screenHeightDp / 5.8).toInt()
            screenHeightDp < (containerSize.width / 1.1) -> (screenHeightDp / 3.2).toInt()
            else -> (screenHeightDp / 5.1).toInt()
        }
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = 50.dp)
                .height(height.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            functionalContent(type)
        }
    }

    PrimaryDialog(
        onDismissRequest = onDismissRequest,
        actionButton = {
            val screenWidth = containerSize.width.pxToDp()
            var parentWidthDp by remember { mutableStateOf(screenWidth) }
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutCoordinates ->
                        val widthPx = layoutCoordinates.size.width
                        parentWidthDp = with(density) { widthPx.toDp() }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                dialogWithMenuView.extensionActionButton(type)
                Spacer(modifier = modifier.weight(1f))
                val autoSize = TextAutoSize.StepBased(minFontSize = 5.sp, maxFontSize = 14.sp)
                TextButton(onClick = {
                    onDismissClick()
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Text(
                        text = dialogWithMenuView.dismissText,
                        modifier = Modifier.widthIn(max = parentWidthDp / 4),
                        maxLines = 1, autoSize = autoSize
                    )
                }
                Spacer(modifier = modifier.width(8.dp))
                TextButton(onClick = {
                    coroutineScope.launch {
                        confirm(type)
                    }
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }) {
                    Text(
                        text = dialogWithMenuView.confirmText,
                        modifier = Modifier.widthIn(max = parentWidthDp / 4),
                        maxLines = 1, autoSize = autoSize
                    )
                }
            }
        },
        modifier = modifier.sizeIn(
            maxWidth = 720.dp, maxHeight = (containerSize.height.pxToDpInt() / 1.2).dp
        ) then modifier.width((containerSize.width.pxToDpInt() / 1.2).dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dialogWithMenuView.title,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 22.sp),
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.sizeIn(maxHeight = 39.dp, maxWidth = 39.dp)) {
                    dialogWithMenuView.extensionTitleButton(type)
                }
            }
        },
        content = {
            val paddingValue = if (shortScreen) 5.dp else 16.dp
            if (isLandScape) Row {
                selectContent()
                VerticalDivider(modifier.padding(start = paddingValue, end = paddingValue))
                functionalContent()
            } else Column {
                HorizontalDivider(modifier.padding(top = 1.dp, bottom = paddingValue))
                selectContent()
                HorizontalDivider(modifier.padding(top = paddingValue, bottom = paddingValue))
                functionalContent()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

data class DialogWithMenuView(
    val title: String,
    val confirmText: String,
    val dismissText: String,
    val extensionTitleButton: @Composable (type: Any?) -> Unit = {},
    val extensionActionButton: @Composable (type: Any?) -> Unit = {},
)

@Preview
@Composable
private fun DialogWithMenuPreview() {
    data class TestData(val t: String, val v: Int)

    val test = listOf(TestData("TEST A", 0), TestData("TEST B", 1)).toTypedArray()
    DialogWithMenu(
        onDismissRequest = {}, confirm = {}, menuListGetter = test, menuText = {
            when (it) {
                is TestData -> it.t
                else -> "Select a type"
            }
        }, functionalContent = { type ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text("This is the functional content area.")
                Text("Selected type: ${if (type is TestData) type.t else "None"}")
            }
        }, dialogWithMenuView = DialogWithMenuView(
            title = "Preview Dialog", confirmText = "Confirm", dismissText = "Dismiss"
        )
    )
}
