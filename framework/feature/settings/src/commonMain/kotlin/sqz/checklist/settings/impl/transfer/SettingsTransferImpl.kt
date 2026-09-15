package sqz.checklist.settings.impl.transfer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import sqz.checklist.data.settings.repository.SettingsRepository
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.settings.api.transfer.SettingsImportResult
import sqz.checklist.settings.api.transfer.SettingsTransfer

/** Default JSON transfer implementation for cross-platform settings backups. */
internal class SettingsTransferImpl(
    private val repository: SettingsRepository,
) : SettingsTransfer {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Serializes the repository's latest snapshot with explicit default values. */
    override fun export(): String = json.encodeToString(
        SettingsBackupPayload(settings = repository.settings.value),
    )

    /** Parses, checks the format version, and atomically replaces persisted settings. */
    override suspend fun import(payload: String): SettingsImportResult {
        val decoded = try {
            json.decodeFromString<SettingsBackupPayload>(payload)
        } catch (error: SerializationException) {
            return SettingsImportResult.InvalidData(error.message ?: "Invalid settings payload")
        } catch (error: IllegalArgumentException) {
            return SettingsImportResult.InvalidData(error.message ?: "Invalid settings values")
        }
        if (decoded.formatVersion != SettingsBackupPayload.CURRENT_FORMAT_VERSION) {
            return SettingsImportResult.UnsupportedFormat(decoded.formatVersion)
        }
        return try {
            repository.replace(decoded.settings)
            SettingsImportResult.Success
        } catch (error: IllegalArgumentException) {
            SettingsImportResult.InvalidData(error.message ?: "Invalid settings values")
        }
    }

    /** Replaces persisted settings from a trusted decoded compatibility payload. */
    override suspend fun restore(settings: AppSettings) {
        repository.replace(settings)
    }
}
