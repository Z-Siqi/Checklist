package sqz.checklist.settings.api

import sqz.checklist.data.settings.repository.SettingsRepository
import sqz.checklist.settings.api.manage.Settings
import sqz.checklist.settings.api.transfer.SettingsTransfer
import sqz.checklist.settings.impl.manage.SettingsImpl
import sqz.checklist.settings.impl.transfer.SettingsTransferImpl

/** Groups the settings management and transfer entry points assembled over one repository. */
data class SettingsFeature(
    val settings: Settings,
    val transfer: SettingsTransfer,
)

/** Creates the settings feature backed by [repository]. */
fun settingsProvider(repository: SettingsRepository): SettingsFeature = SettingsFeature(
    settings = SettingsImpl(repository),
    transfer = SettingsTransferImpl(repository),
)
