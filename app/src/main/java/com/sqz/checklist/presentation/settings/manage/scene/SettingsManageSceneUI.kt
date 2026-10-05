package com.sqz.checklist.presentation.settings.manage.scene

import android.view.View
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sqz.checklist.R
import com.sqz.checklist.presentation.settings.manage.SettingsManageScope
import com.sqz.checklist.presentation.settings.manage.SettingsManageViewModel
import com.sqz.checklist.presentation.settings.manage.component.matchesSettingsQuery
import com.sqz.checklist.presentation.settings.manage.scene.item.AboutSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.AllowedHistorySetting
import com.sqz.checklist.presentation.settings.manage.scene.item.ClearHistorySetting
import com.sqz.checklist.presentation.settings.manage.scene.item.DisableExactAlarmWarningSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.DisableRemoveNotificationSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.DisableUndoSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.DonateSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.LanguageSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.PictureCompressionSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.RecentReminderKeepTimeSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.RemoveNotificationWithHistorySetting
import com.sqz.checklist.presentation.settings.manage.scene.item.SystemNotificationSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.ThemeSetting
import com.sqz.checklist.presentation.settings.manage.scene.item.VideoCompressionSetting
import kotlinx.coroutines.CancellationException
import sqz.checklist.model.settings.AppSettings

/**
 * Display order and localized heading for each settings section.
 *
 * Add a category here before assigning settings to it in [SettingKey]. Enum order is the order
 * rendered on screen and in filtered search results.
 */
private enum class SettingCategory(@StringRes val title: Int) {
    History(R.string.task_history),
    Notification(R.string.notification),
    General(R.string.general_settings),
}

/**
 * Stable inventory used by normal rendering and search.
 *
 * To add a setting, register its category and searchable title here, implement a stateless item
 * composable under `presentation.settings.manage.item`, then add its dispatch branch in
 * [SettingContent]. Keeping metadata separate from composable lambdas provides stable LazyColumn
 * keys and prevents rebuilding UI objects during recomposition.
 */
private enum class SettingKey(
    val category: SettingCategory,
    @StringRes val title: Int,
) {
    AllowedHistory(SettingCategory.History, R.string.allowed_number_of_history),
    ClearHistory(SettingCategory.History, R.string.clear_history_when_leaved),
    DisableUndo(SettingCategory.History, R.string.disable_undo_button),
    DisableRemoveNotification(SettingCategory.Notification, R.string.disable_remove_notify_in_reminded),
    DisableExactAlarmWarning(SettingCategory.Notification, R.string.disable_no_schedule_exact_alarm_notice),
    RecentReminderKeepTime(SettingCategory.Notification, R.string.recently_reminded_keep_time),
    RemoveNotificationWithHistory(SettingCategory.Notification, R.string.remove_notice_in_auto_del_reminded),
    SystemNotification(SettingCategory.Notification, R.string.system_notify_settings),
    Theme(SettingCategory.General, R.string.theme),
    PictureCompression(SettingCategory.General, R.string.picture_compression_rate),
    VideoCompression(SettingCategory.General, R.string.video_compression_rate),
    Language(SettingCategory.General, R.string.language),
    About(SettingCategory.General, R.string.about),
    Donate(SettingCategory.General, R.string.sponsor_me),
}

/**
 * Renders the settings inventory and coordinates search-only presentation behavior.
 *
 * The caller owns whether search is active. This scene owns the query, scroll position and
 * predictive-back progress so navigation code does not need knowledge of presentation details.
 */
@Composable
internal fun SettingsManageScope.SettingsManageSceneUI(
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
    searchEnabled: Boolean,
    onSearchCancelled: () -> Unit,
    view: View,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var searchBackProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = searchEnabled) { progressFlow ->
        try {
            progressFlow.collect { event -> searchBackProgress = event.progress }
            onSearchCancelled()
        } catch (_: CancellationException) {
            searchBackProgress = 0f
        }
    }
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString()
    val visibleKeys = SettingKey.entries.filter { key ->
        !searchEnabled || matchesSettingsQuery(stringResource(key.title), query)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(bottom = 72.dp),
        ) {
            if (searchEnabled) {
                item(key = "search", contentType = "search") {
                    SearchField(state = searchState, backProgress = searchBackProgress)
                }
            }
            SettingCategory.entries.forEach { category ->
                val categoryKeys = visibleKeys.filter { it.category == category }
                if (categoryKeys.isNotEmpty()) {
                    item(key = "header-${category.name}", contentType = "header") {
                        CategoryTitle(stringResource(category.title))
                    }
                    item(key = "category-${category.name}", contentType = "category") {
                        CategoryCard {
                            categoryKeys.forEachIndexed { index, key ->
                                SettingContent(key, settings, viewModel, view)
                                if (index != categoryKeys.lastIndex) Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(searchEnabled) {
            searchBackProgress = 0f
            if (searchEnabled) listState.animateScrollToItem(0)
        }
    }
}

@Composable
private fun SearchField(
    state: androidx.compose.foundation.text.input.TextFieldState,
    backProgress: Float,
) {
    val translation = with(LocalDensity.current) { 48.dp.toPx() }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .heightIn(min = 56.dp)
            .graphicsLayer {
                alpha = 1f - backProgress
                translationY = -translation * backProgress
            },
        shape = ShapeDefaults.ExtraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search),
                modifier = Modifier.padding(start = 12.dp),
            )
            BasicTextField(
                state = state,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun CategoryTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 12.dp, top = 16.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun CategoryCard(content: @Composable () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            content = { content() },
        )
    }
}

/** Maps each stable inventory key to its concrete setting-row composable. */
@Composable
private fun SettingsManageScope.SettingContent(
    key: SettingKey,
    settings: AppSettings,
    viewModel: SettingsManageViewModel,
    view: View,
) {
    when (key) {
        SettingKey.AllowedHistory -> AllowedHistorySetting(settings, viewModel, view)
        SettingKey.ClearHistory -> ClearHistorySetting(settings, viewModel)
        SettingKey.DisableUndo -> DisableUndoSetting(settings, viewModel)
        SettingKey.DisableRemoveNotification -> DisableRemoveNotificationSetting(settings, viewModel)
        SettingKey.DisableExactAlarmWarning -> DisableExactAlarmWarningSetting(settings, viewModel)
        SettingKey.RecentReminderKeepTime -> RecentReminderKeepTimeSetting(settings, viewModel, view)
        SettingKey.RemoveNotificationWithHistory -> RemoveNotificationWithHistorySetting(settings, viewModel)
        SettingKey.SystemNotification -> SystemNotificationSetting(view)
        SettingKey.Theme -> ThemeSetting(settings, viewModel)
        SettingKey.PictureCompression -> PictureCompressionSetting(settings, viewModel, view)
        SettingKey.VideoCompression -> VideoCompressionSetting(settings, viewModel, view)
        SettingKey.Language -> LanguageSetting(view)
        SettingKey.About -> AboutSetting(view)
        SettingKey.Donate -> DonateSetting(view)
    }
}
