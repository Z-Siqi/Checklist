package manage

import kotlinx.coroutines.flow.MutableStateFlow
import sqz.checklist.data.settings.repository.SettingsRepository
import sqz.checklist.model.settings.AppSettings

internal class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.value = transform(settings.value)
    }

    override suspend fun replace(settings: AppSettings) {
        this.settings.value = settings
    }
}
