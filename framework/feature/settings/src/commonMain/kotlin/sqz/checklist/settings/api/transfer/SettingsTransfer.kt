package sqz.checklist.settings.api.transfer

import sqz.checklist.model.settings.AppSettings

/** Converts settings between the runtime model and the stable logical backup representation. */
interface SettingsTransfer {
    /** Serializes the current settings snapshot as a versioned JSON payload. */
    fun export(): String
    /** Validates and restores a versioned JSON [payload] without restarting the process. */
    suspend fun import(payload: String): SettingsImportResult
    /** Restores an already decoded settings snapshot, such as a legacy backup conversion. */
    suspend fun restore(settings: AppSettings)
}

/** Describes the result of parsing and restoring a logical settings backup. */
sealed interface SettingsImportResult {
    data object Success : SettingsImportResult
    data class UnsupportedFormat(val version: Int) : SettingsImportResult
    data class InvalidData(val reason: String) : SettingsImportResult
}
