package com.sqz.checklist.common.storage

import android.content.Context
import com.sqz.checklist.assembly.settings.SettingsAssembly
import sqz.checklist.settings.api.transfer.SettingsImportResult
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal const val SETTINGS_BACKUP_ENTRY = "settings.json"
internal const val LEGACY_SETTINGS_BACKUP_ENTRY = "primary_preferences.xml"

/** Writes the current logical settings payload as one ZIP entry. */
internal fun ZipOutputStream.writeSettingsBackup(context: Context) {
    val payload = SettingsAssembly.get(context).transfer.export()
    putNextEntry(ZipEntry(SETTINGS_BACKUP_ENTRY))
    write(payload.encodeToByteArray())
    closeEntry()
}

/**
 * Restores a current or legacy settings entry.
 *
 * @return `true` when settings were restored, or `null` when [entryName] is unrelated.
 */
internal suspend fun ZipInputStream.restoreSettingsEntry(
    context: Context,
    entryName: String,
): Boolean? = when (entryName) {
    SETTINGS_BACKUP_ENTRY -> {
        val result = SettingsAssembly.get(context).transfer.import(readBytes().decodeToString())
        require(result is SettingsImportResult.Success) { "Invalid settings backup: $result" }
        true
    }

    LEGACY_SETTINGS_BACKUP_ENTRY -> {
        @Suppress("DEPRECATION")
        val settings = LegacySettingsBackupImporter.decode(readBytes())
        SettingsAssembly.get(context).transfer.restore(settings)
        true
    }

    else -> null
}
