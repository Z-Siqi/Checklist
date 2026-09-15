package sqz.checklist.settings.impl.transfer

import kotlinx.serialization.Serializable
import sqz.checklist.model.settings.AppSettings

/**
 * Versioned envelope used only for external settings backup interchange.
 *
 * Adding an optional setting with a model default or deleting an ignored setting does not require
 * incrementing [formatVersion]. Increase the version only when the serialized representation or
 * meaning becomes incompatible and add corresponding import handling before doing so.
 */
@Serializable
internal data class SettingsBackupPayload(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val settings: AppSettings,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}
