package com.sqz.checklist.ui.main.task.layout

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqz.checklist.R
import com.sqz.checklist.database.Task
import com.sqz.checklist.database.TaskDetail
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.TaskLayoutViewModelPreview
import com.sqz.checklist.ui.main.task.layout.function.CheckTaskAction
import com.sqz.checklist.ui.main.task.layout.function.EditTask
import com.sqz.checklist.ui.main.task.layout.function.ReminderHandlerListener
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailData
import com.sqz.checklist.ui.main.task.layout.function.TaskModifyDialog
import com.sqz.checklist.ui.main.task.layout.item.LazyList
import com.sqz.checklist.ui.main.task.layout.item.ListData
import com.sqz.checklist.ui.common.dialog.InfoAlertDialog
import com.sqz.checklist.ui.common.dialog.InfoDialogWithURL
import com.sqz.checklist.ui.common.dialog.OpenExternalAppDialog
import com.sqz.checklist.ui.common.media.AudioViewDialog
import com.sqz.checklist.ui.common.media.PictureViewDialog
import com.sqz.checklist.ui.common.media.VideoViewDialog
import com.sqz.checklist.ui.theme.Theme
import com.sqz.checklist.ui.common.unit.screenIsWidthAndAPI34Above
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Top layout of TaskLayout.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLayout(
    scrollBehavior: TopAppBarScrollBehavior,
    context: Context, view: View,
    modifier: Modifier = Modifier,
    taskState: TaskLayoutViewModel = viewModel(),
    listState: ListData = taskState.listState.collectAsState().value
) {
    val colors = Theme.color
    val lazyState = rememberLazyListState(
        navConnector = taskState.navExtendedConnector.collectAsState().value,
        scrollBehavior = scrollBehavior,
        updateNavConnector = taskState::updateNavConnector
    )
    val coroutineScope = rememberCoroutineScope()
    Surface(
        modifier = modifier,
        color = colors.backgroundColor
    ) {
        val left = WindowInsets.displayCutout.asPaddingValues()
            .calculateLeftPadding(LocalLayoutDirection.current)
        val safePaddingForFullscreen = if (screenIsWidthAndAPI34Above()) modifier.padding(
            start = left, end = if (left / 3 > 15.dp) 15.dp else left / 3
        ) else modifier

        var currentHeight by remember { mutableIntStateOf(0) }
        val searchBarSpace = if (currentHeight > 50) (currentHeight + 22) else 72
        LazyList( // LazyColumn lists
            listState = listState,
            lazyState = lazyState,
            isInSearch = { // Search function
                taskSearchBar(
                    searchState = listState.searchView,
                    taskState = taskState,
                    modifier = safePaddingForFullscreen
                ) { currentHeight = it }
            },
            context = context,
            taskState = taskState,
            modifier = safePaddingForFullscreen,
            searchBarSpace = searchBarSpace
        )
        if (!listState.unLoading && listState.item.isEmpty()) Column( // Show text if not any task
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.nothing_need_do),
                fontWeight = FontWeight.Medium, fontSize = 24.sp,
                color = MaterialTheme.colorScheme.outline,
                lineHeight = 30.sp, textAlign = TextAlign.Center
            )
        }
        if (!listState.unLoading) CheckTaskAction( // processing check & undo
            taskState = taskState,
            lazyState = lazyState,
            context = context
        )
    }
    taskState.modifyHandler.inModifyTask.collectAsState().value.let { // Edit Task
        if (it != null) TaskModifyDialog(
            editTask = EditTask(
                it.inModifyTask!!.id, it.inModifyTask.description, it.inModifyDetail
            ),
            confirm = { confirm ->
                taskState.modifyHandler.editTask(
                    confirm.description, confirm.detail?.type, confirm.detail?.dataString,
                    confirm.detail?.dataByte, view.context
                )
                TaskDetailData.instance().releaseMemory()
            },
            onDismissRequest = { taskState.modifyHandler.requestEditTask(null) },
            view = view
        )
    }
    ReminderHandlerListener(
        reminderHandler = taskState.reminderHandler,
        context = context,
        view = view,
        coroutineScope = coroutineScope
    )
    TaskDetailInfoDialog(
        onDismissRequest = { taskState.taskDetailData(-1L) },
        detail = taskState.taskDetailData().collectAsState().value
    )
}

@Composable
private fun TaskDetailInfoDialog(onDismissRequest: () -> Unit, detail: TaskDetail) {
    if (detail.id != 0L) when (detail.type) {
        TaskDetailType.Text -> InfoAlertDialog(
            onDismissRequest = onDismissRequest,
            text = detail.dataString, title = stringResource(R.string.detail)
        )

        TaskDetailType.URL -> InfoDialogWithURL(
            onDismissRequest = onDismissRequest,
            url = detail.dataString, title = stringResource(R.string.url)
        )

        TaskDetailType.Application -> OpenExternalAppDialog(
            onDismissRequest = onDismissRequest,
            packageName = detail.dataString, title = stringResource(R.string.application)
        )

        TaskDetailType.Picture -> PictureViewDialog(
            onDismissRequest = onDismissRequest,
            byteArray = detail.dataByte!!, imageName = detail.dataString,
            title = stringResource(R.string.picture)
        )

        TaskDetailType.Video -> VideoViewDialog(
            onDismissRequest = onDismissRequest,
            byteArray = detail.dataByte!!, videoName = detail.dataString,
            title = stringResource(R.string.video)
        )

        TaskDetailType.Audio -> AudioViewDialog(
            onDismissRequest = onDismissRequest,
            byteArray = detail.dataByte!!, audioName = detail.dataString,
            title = stringResource(R.string.audio)
        )
    }
}

@Composable
private fun taskSearchBar(
    searchState: Boolean,
    taskState: TaskLayoutViewModel,
    modifier: Modifier = Modifier,
    currentHeight: (Int) -> Unit = {}
): Boolean {
    val undo = taskState.undo.collectAsState().value
    val density = LocalDensity.current
    if (searchState) Column(modifier = modifier.fillMaxSize()) {
        val textFieldState = rememberTextFieldState()
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp)
                .heightIn(min = 50.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    val heightPx = layoutCoordinates.size.height
                    currentHeight(with(density) { heightPx.toDp() }.value.toInt())
                },
            shape = ShapeDefaults.ExtraLarge
        ) {
            Row(Modifier.heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.padding(start = 10.dp),
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.search)
                )
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 9.dp, end = 9.dp, top = 10.dp, bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    state = textFieldState,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant)
                )
                var oldText by remember { mutableStateOf("") }
                if (textFieldState.text.toString() != oldText || undo.onCheckTask) {
                    LaunchedEffect(key1 = true) {
                        taskState.searchingText = textFieldState.text.toString()
                        taskState.updateInSearch(taskState.searchingText)
                        oldText = textFieldState.text.toString()
                    }
                } else if (textFieldState.text.toString().isEmpty()) LaunchedEffect(key1 = true) {
                    taskState.updateInSearch(initWithAll = true)
                }
            }
        }
    }
    if (searchState) BackHandler {
        taskState.updateNavConnector(
            NavConnectData(searchState = false),
            NavConnectData(searchState = true)
        )
    }
    return searchState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberLazyListState(
    navConnector: NavConnectData,
    scrollBehavior: TopAppBarScrollBehavior,
    updateNavConnector: (data: NavConnectData, updateSet: NavConnectData) -> Unit,
): LazyListState {
    val lazyState = rememberLazyListState()
    val rememberTopBarHeight = rememberSaveable { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { // this LaunchedEffect is used to fix a crash when rotate screen in auto scroll
        delay(200)
        rememberTopBarHeight.floatValue = scrollBehavior.state.heightOffsetLimit
    }
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.canScrollForward || lazyState.canScrollBackward }.collect {
            updateNavConnector(
                NavConnectData(canScroll = it), NavConnectData(canScroll = true)
            )
        }
    }
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.canScrollForward }.collect {
            updateNavConnector(
                NavConnectData(canScrollForward = it), NavConnectData(canScrollForward = true)
            )
        }
    }
    if (navConnector.scrollToFirst) LaunchedEffect(Unit) {
        lazyState.animateScrollToItem(0)
        scrollBehavior.state.heightOffset = 0f
        updateNavConnector(
            NavConnectData(scrollToFirst = false), NavConnectData(scrollToFirst = true)
        )
    }
    if (navConnector.scrollToBottom) LaunchedEffect(Unit) {
        lazyState.animateScrollToItem(lazyState.layoutInfo.totalItemsCount)
        scrollBehavior.state.heightOffset = rememberTopBarHeight.floatValue
        updateNavConnector(
            NavConnectData(scrollToBottom = false), NavConnectData(scrollToBottom = true)
        )
    }
    LaunchedEffect(navConnector.scrollToFirst || navConnector.scrollToBottom) {
        delay(850) // Timeout for scroll state change
        if (navConnector.scrollToFirst || navConnector.scrollToBottom) updateNavConnector(
            NavConnectData(scrollToFirst = false, scrollToBottom = false),
            NavConnectData(scrollToFirst = true, scrollToBottom = true)
        )
    }
    return lazyState
}

@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    val item = listOf(Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()))
    TaskLayout(
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        LocalContext.current, LocalView.current, listState = ListData(false, item, item, item),
        taskState = TaskLayoutViewModelPreview()
    )
}
