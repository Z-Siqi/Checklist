package transfer

import kotlinx.coroutines.test.runTest
import manage.FakeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.model.settings.HistorySettings
import sqz.checklist.settings.api.settingsProvider
import sqz.checklist.settings.api.transfer.SettingsImportResult

class SettingsTransferTest {
    @Test
    fun exportedSettingsCanBeRestoredIntoAnotherRepository() = runTest {
        val expected = AppSettings(history = HistorySettings(allowedNumberOfHistory = 19))
        val source = settingsProvider(FakeSettingsRepository(expected)).transfer
        val destinationRepository = FakeSettingsRepository()
        val destination = settingsProvider(destinationRepository).transfer

        val result = destination.import(source.export())

        assertIs<SettingsImportResult.Success>(result)
        assertEquals(expected, destinationRepository.settings.value)
    }

    @Test
    fun unsupportedBackupFormatDoesNotReplaceSettings() = runTest {
        val repository = FakeSettingsRepository()
        val transfer = settingsProvider(repository).transfer
        val payload = transfer.export().replace("\"formatVersion\":1", "\"formatVersion\":2")

        val result = transfer.import(payload)

        assertEquals(SettingsImportResult.UnsupportedFormat(2), result)
        assertEquals(AppSettings(), repository.settings.value)
    }
}
