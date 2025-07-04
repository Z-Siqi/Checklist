package com.sqz.checklist.ui.main.history.task

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.ui.theme.Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SelectData(
    val selectedId: Long = -0,
    val onSelect: Boolean = false,
    val hideSelected: Boolean = false // Hide in a short time before remove (for animation)
)

/**
 * Task history screen layout.
 **/
@Composable
fun TaskHistory(
    modifier: Modifier = Modifier,
    historyState: TaskHistoryViewModel = viewModel(),
    item: List<Task> = historyState.taskHistoryData.collectAsState().value.also { historyState.updateTaskHistoryData() }
) {
    val view = LocalView.current
    val colors = Theme.color
    val localConfig = LocalWindowInfo.current.containerSize
    val screenIsWidth = localConfig.width > localConfig.height * 1.2
    val left = WindowInsets.displayCutout.asPaddingValues()
        .calculateLeftPadding(LocalLayoutDirection.current)
    val safePaddingForFullscreen = if (
        screenIsWidth
    ) modifier.padding(
        start = left, end = if (left / 3 > 15.dp) 15.dp else left / 3
    ) else modifier
    Surface(
        modifier = modifier,
        color = colors.backgroundColor
    ) {
        val selectState = historyState.selectState.collectAsState().value
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = modifier.height(20.dp))
            }
            items(item, key = { it.id }) {
                ItemBox(
                    item = it,
                    selectState = selectState,
                    onItemClick = { historyState.setSelectTask(it.id) },
                    hide = selectState.hideSelected && selectState.selectedId == it.id,
                    view = view,
                    modifier = safePaddingForFullscreen
                )
            }
            item {
                Spacer(modifier = modifier.height(10.dp))
            }
        }
        var value by rememberSaveable { mutableIntStateOf(-1) }
        if (value == 0) {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.nothing_here),
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else LaunchedEffect(item) {
            value = MainActivity.taskDatabase.taskDao().getIsHistorySum()
        }
    }
}

@Composable
private fun ItemBox(
    item: Task,
    selectState: SelectData,
    hide: Boolean,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    view: View
) {
    val colors = Theme.color
    val border = if (selectState.selectedId == item.id) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.tertiary)
    } else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceDim)
    Column(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) then Modifier.height(if (hide) 0.dp else 120.dp)
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            colors = CardDefaults.cardColors(colors.taskBackgroundColor),
            border = border,
            shape = ShapeDefaults.ExtraLarge
        ) {
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
            val padding = Modifier.padding(bottom = 8.dp, top = 12.dp, start = 12.dp, end = 11.dp)
            val onClick = Modifier.clickable {
                onItemClick()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
            Box(modifier = Modifier.fillMaxSize() then onClick) {
                val time =
                    stringResource(R.string.task_creation_time, item.createDate.format(formatter))
                Box(modifier = padding) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(50.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = item.description,
                                modifier = Modifier.padding(top = 0.dp),
                                fontSize = 21.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = time,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    val item = listOf(
        Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()),
        Task(1, "The quick brown fox jumps over the lazy dog.", LocalDate.now())
    )
    TaskHistory(item = item)
}
