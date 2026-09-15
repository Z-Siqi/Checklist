package sqz.checklist.data.settings.datastore

import kotlin.test.Test
import kotlin.test.assertFailsWith
import sqz.checklist.model.settings.AppSettings

class SettingsValidationTest {
    @Test
    fun invalidCompressionIsRejectedBeforePersistence() {
        val invalid = AppSettings().copy(
            media = AppSettings().media.copy(pictureCompressionPercent = 101),
        )

        assertFailsWith<IllegalArgumentException> { invalid.requireValid() }
    }
}
