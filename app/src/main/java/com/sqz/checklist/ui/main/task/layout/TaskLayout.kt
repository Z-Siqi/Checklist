package com.sqz.checklist.ui.main.task.layout

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import com.sqz.checklist.ui.main.task.layout.function.CheckTaskAction
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailDialog
import com.sqz.checklist.ui.main.task.layout.item.EditState
import com.sqz.checklist.ui.main.task.layout.item.LazyList
import com.sqz.checklist.ui.main.task.layout.item.ListData
import com.sqz.checklist.ui.material.dialog.InfoAlertDialog
import com.sqz.checklist.ui.material.dialog.InfoDialogWithURL
import com.sqz.checklist.ui.material.dialog.OpenExternalAppDialog
import com.sqz.checklist.ui.material.dialog.TaskChangeContentDialog
import com.sqz.checklist.ui.material.TextTooltipBox
import com.sqz.checklist.ui.main.task.layout.function.ReminderAction
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailData
import com.sqz.checklist.ui.main.task.layout.function.toByteArray
import com.sqz.checklist.ui.main.task.layout.function.toUri
import com.sqz.checklist.ui.material.media.PictureViewDialog
import com.sqz.checklist.ui.material.media.errUri
import com.sqz.checklist.ui.material.media.insertPicture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    listState: ListData = taskState.listState.collectAsState().value,
    isPreview: Boolean = false
) {
    val lazyState = rememberLazyListState(
        navConnector = taskState.navExtendedConnector.collectAsState().value,
        scrollBehavior = scrollBehavior,
        updateNavConnector = taskState::updateNavConnector
    )
    val coroutineScope = rememberCoroutineScope()
    var undoTask by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        val localConfig = LocalConfiguration.current
        val screenIsWidth = localConfig.screenWidthDp > localConfig.screenHeightDp * 1.2
        val left = WindowInsets.displayCutout.asPaddingValues()
            .calculateLeftPadding(LocalLayoutDirection.current)
        val safePaddingForFullscreen = if (
            Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenIsWidth
        ) modifier.padding(
            start = left, end = if (left / 3 > 15.dp) 15.dp else left / 3
        ) else modifier

        LazyList( // LazyColumn lists
            listState = listState,
            lazyState = lazyState,
            undoTask = { state ->
                if (undoTask) coroutineScope.launch {
                    state.reset()
                    undoTask = false
                }
            },
            isInSearch = { // Search function
                taskSearchBar(
                    searchState = listState.searchView,
                    taskState = taskState,
                    modifier = safePaddingForFullscreen
                )
            },
            context = context,
            taskState = taskState,
            modifier = safePaddingForFullscreen,
            isPreview = isPreview
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
            whenUndo = { undoTask = true },
            taskState = taskState,
            lazyState = lazyState,
            context = context
        )
    }
    EditTask(
        editState = taskState.taskData.collectAsState().value.editState,
        editTask = taskState::editTask,
        resetState = { taskState.resetTaskData() },
        detailData = taskState.taskDetailDataSaver(),
        view = view
    )
    if (!isPreview) ReminderAction(
        reminder = taskState.taskData.collectAsState().value.reminder,
        context = context,
        view = view,
        taskState = taskState,
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
    }
}

@Composable
private fun EditTask(
    editState: EditState,
    editTask: (
        id: Long, edit: String, detailType: TaskDetailType?,
        detailDataString: String?, detailByteArray: ByteArray?, context: Context
    ) -> Unit,
    detailData: TaskDetailData,
    resetState: () -> Unit,
    view: View
) {
    if (editState.state) {
        val textState = rememberTextFieldState()
        var detail by rememberSaveable { mutableStateOf(false) }
        var remember by rememberSaveable { mutableStateOf(false) }
        if (!remember) LaunchedEffect(true) {
            textState.clearText()
            textState.edit { insert(0, editState.task.description) }
            if (editState.detail?.type != null) { // when found task detail
                detailData.detailType(editState.detail.type)
                detailData.detailString(editState.detail.dataString)
                if (editState.detail.dataByte != null) {
                    detailData.detailUri(editState.detail.dataByte.toUri())
                }
            }
            remember = true
        }
        val noChangeDoNothing = stringResource(R.string.no_change_do_nothing)
        var confirmState by rememberSaveable { mutableIntStateOf(0) }
        if (confirmState != 0) {
            if (textState.text.toString() != "") {
                val uri = if (detailData.detailType() == TaskDetailType.Picture) {
                    val insertPicture = insertPicture(view.context, detailData.detailUri()!!)
                    val picture = insertPicture?.toByteArray()
                    if (insertPicture != null) confirmState = 2
                    if (insertPicture != errUri) picture else {
                        detailData.detailType(TaskDetailType.Text)
                        null
                    }
                } else {
                    confirmState = 2
                    detailData.detailUri()?.toByteArray()
                }
                if (confirmState == 2) {
                    editTask(
                        editState.task.id, textState.text.toString(), detailData.detailType(),
                        detailData.detailString(), uri, view.context
                    )
                    resetState().also { confirmState = 0 }
                }
            } else {
                Toast.makeText(view.context, noChangeDoNothing, Toast.LENGTH_SHORT).show()
                confirmState = 0
            }
        }
        TaskChangeContentDialog(
            onDismissRequest = {
                resetState()
                detailData.releaseMemory(view.context)
            },
            confirm = { confirmState = 1 },
            state = textState,
            title = stringResource(R.string.edit_task),
            confirmText = stringResource(R.string.edit),
            extraButtonBottom = {
                TextTooltipBox(textRid = R.string.create_task_detail) {
                    IconButton(
                        onClick = { detail = !detail },
                        colors = if (detailData.detailType() != null) {
                            IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        } else IconButtonDefaults.iconButtonColors()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.attach),
                            contentDescription = stringResource(R.string.create_task_detail)
                        )
                    }
                }
            },
            doneImeAction = true
        )
        if (detail) TaskDetailDialog(
            onDismissRequest = { onDismissClick ->
                if (onDismissClick != null && onDismissClick) {
                    detailData.releaseMemory(view.context)
                }
                detail = false
            },
            confirm = { type, string, uri ->
                detailData.setter(type, string, uri)
                detail = false
            },
            title = stringResource(R.string.create_task_detail),
            detailData = detailData,
            view = view
        )
    }
}

@Composable
private fun taskSearchBar(
    searchState: Boolean,
    taskState: TaskLayoutViewModel,
    modifier: Modifier = Modifier
): Boolean {
    val undo = taskState.undo.collectAsState().value
    if (searchState) Column(modifier = modifier.fillMaxSize()) {
        val textFieldState = rememberTextFieldState()
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp)
                .height(50.dp),
            shape = ShapeDefaults.ExtraLarge
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.padding(start = 10.dp),
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.search)
                )
                BasicTextField(
                    modifier = Modifier
                        .fillMaxSize()
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
                if (textFieldState.text.toString() != oldText || undo.checkTaskAction) {
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
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val rememberTopBarHeight = rememberSaveable { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { // this LaunchedEffect is used to fix a crash when rotate screen in auto scroll
        delay(200)
        rememberTopBarHeight.floatValue = scrollBehavior.state.heightOffsetLimit
    }
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.layoutInfo.totalItemsCount * 120 > screenHeight }.collect {
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    val item = listOf(Task(0, "The quick brown fox jumps over the lazy dog.", LocalDate.now()))
    TaskLayout(
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        LocalContext.current, LocalView.current, listState = ListData(false, item, item, item),
        taskState = TaskLayoutViewModel(), isPreview = true
    )
}
