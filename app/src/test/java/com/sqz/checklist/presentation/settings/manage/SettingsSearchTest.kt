package com.sqz.checklist.presentation.settings.manage

import com.sqz.checklist.presentation.settings.manage.component.matchesSettingsQuery
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    @Test
    fun emptyQueryMatchesEverySetting() {
        assertTrue(matchesSettingsQuery("Theme", ""))
    }

    @Test
    fun matchingIgnoresCaseAndLineBreaks() {
        assertTrue(matchesSettingsQuery("Picture\nCompression", "picturecompression"))
        assertTrue(matchesSettingsQuery("NOTIFICATION", "notification"))
    }

    @Test
    fun unrelatedQueryDoesNotMatch() {
        assertFalse(matchesSettingsQuery("Theme", "history"))
    }
}
