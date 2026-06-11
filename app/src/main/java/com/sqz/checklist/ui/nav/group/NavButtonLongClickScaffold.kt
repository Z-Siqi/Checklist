package com.sqz.checklist.ui.nav.group

import androidx.collection.intListOf
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.sqz.checklist.ui.common.TextTooltipBox
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * **This method expected to be called only within this package and its sub-packages.**
 *
 * Wraps a navigation button with optional long-press behavior.
 *
 * Supported modes:
 * - Plain button when both [tooltip] and [menu] are `null`
 * - Tooltip on long press when [tooltip] is not `null`
 * - Popup menu on long press when [menu] is not `null`
 *
 * [tooltip] and [menu] are mutually exclusive. Passing both throws an
 * [IllegalStateException].
 *
 * The [navButton] slot receives:
 * - [MutableInteractionSource] so the caller can connect press interactions to the button UI
 * - `longClicked` to reflect whether the menu-triggering long press is currently active
 *
 * Example with tooltip:
 * ```kotlin
 * NavButtonLongClickScaffold(
 *     tooltip = "Groups", navButton = { interactionSource, _ ->
 *         IconButton(
 *             onClick = onGroupsClick,
 *             interactionSource = interactionSource
 *         ) { Icon(Icons.Default.List, contentDescription = "Groups") }
 *     }
 * )
 * ```
 *
 * Example with menu:
 * ```kotlin
 * NavButtonLongClickScaffold(
 *     tooltip = null, menu = {
 *         DropdownMenuItem(
 *             text = { Text("Rename") }, onClick = {
 *                 onRenameClick()
 *                 true
 *             }
 *         )
 *         false
 *     }, navButton = { interactionSource, longClicked ->
 *         NavButton(
 *             interactionSource = interactionSource,
 *             selected = longClicked,
 *             onClick = onButtonClick
 *         )
 *     }
 * )
 * ```
 */
@Composable
internal fun NavButtonLongClickScaffold(
    tooltip: String? = null,
    menu: @Composable (() -> Boolean)? = null,
    navButton: @Composable (
        interactionSource: MutableInteractionSource, longClicked: Boolean
    ) -> Unit
) {
    if (tooltip != null && menu != null) {
        throw IllegalStateException("Long click cannot handle tooltip & menu at same time!")
    }
    val interactionSource = remember { MutableInteractionSource() }
    if (menu != null) {
        val viewConfiguration = LocalViewConfiguration.current
        var longClicked by remember { mutableStateOf(false) }
        LaunchedEffect(interactionSource) {
            val jobs = mutableMapOf<PressInteraction.Press, Job>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        longClicked = false
                        val job = launch {
                            delay(viewConfiguration.longPressTimeoutMillis)
                            longClicked = true
                        }
                        jobs[interaction] = job
                    }

                    is PressInteraction.Release -> {
                        jobs.remove(interaction.press)?.cancel()
                    }

                    is PressInteraction.Cancel -> {
                        jobs.remove(interaction.press)?.cancel()
                    }
                }
            }
        }
        DropdownMenuPopup(
            expanded = longClicked,
            onDismissRequest = { longClicked = false },
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures { longClicked = false }
            },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                dropdownMenuAnchorPosition = MenuAnchorPosition.Custom(
                    xCandidates = { anchorBounds, windowSize, menuSize ->
                        val centeredX =
                            anchorBounds.left + (anchorBounds.width - menuSize.width) / 2
                        intListOf(
                            centeredX.coerceIn(0, windowSize.width - menuSize.width)
                        )
                    },
                    yCandidates = { anchorBounds, _, menuSize ->
                        intListOf(
                            anchorBounds.top - menuSize.height,
                            anchorBounds.bottom
                        )
                    }
                ),
            ),
        ) {
            menu().let { LaunchedEffect(it) { longClicked = it } }
            Spacer(modifier = Modifier.padding(vertical = 42.dp))
        }
        navButton(interactionSource, longClicked)
    }
    if (tooltip != null) {
        TextTooltipBox(
            text = tooltip,
            content = { navButton(interactionSource, false) },
        )
    }
    if (menu == null && tooltip == null) {
        navButton(interactionSource, false)
    }
}
