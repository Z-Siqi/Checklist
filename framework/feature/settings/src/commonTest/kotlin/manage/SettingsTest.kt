package manage

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import sqz.checklist.model.settings.AppTheme
import sqz.checklist.settings.api.settingsProvider

class SettingsTest {
    @Test
    fun updatesArePublishedThroughStateFlow() = runTest {
        val settings = settingsProvider(FakeSettingsRepository()).settings

        settings.setTheme(AppTheme.Colorful)
        settings.setAllowedNumberOfHistory(12)
        settings.setUndoEnabled(false)

        assertEquals(AppTheme.Colorful, settings.state.value.appearance.theme)
        assertEquals(12, settings.state.value.history.allowedNumberOfHistory)
        assertEquals(false, settings.state.value.history.undoEnabled)
    }

    @Test
    fun invalidCompressionIsRejected() = runTest {
        val settings = settingsProvider(FakeSettingsRepository()).settings

        assertFailsWith<IllegalArgumentException> {
            settings.setPictureCompressionPercent(101)
        }
    }
}
