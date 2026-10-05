package com.sqz.checklist.presentation.settings.manage.component

import java.util.Locale

internal fun matchesSettingsQuery(title: String, query: String): Boolean {
    val normalizedQuery = query.normalizeSettingsSearchText()
    return normalizedQuery.isEmpty() || title.normalizeSettingsSearchText().contains(normalizedQuery)
}

private fun String.normalizeSettingsSearchText(): String {
    return replace("\n", "").lowercase(Locale.getDefault())
}
