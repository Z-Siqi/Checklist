package sqz.checklist.model

import kotlinx.coroutines.flow.StateFlow

//TODO: Impl this
interface GeneralSettings {

    fun loadSettings()

    val settings: StateFlow<Settings>

    val temporarySettings: StateFlow<TemporarySettings>

    data class Settings(
        val recentlyRemindedKeepTime: Long
    )

    data class TemporarySettings(
        val restoreSettings: Boolean?
    )
}
