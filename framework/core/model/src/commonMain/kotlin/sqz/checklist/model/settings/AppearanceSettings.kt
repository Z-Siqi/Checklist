package sqz.checklist.model.settings

import kotlinx.serialization.Serializable

/** Stable theme values persisted across supported platforms. */
@Serializable
enum class AppTheme {
    Pastel,
    Colorful,
}

/** Settings that control application appearance. */
@Serializable
data class AppearanceSettings(
    val theme: AppTheme = AppTheme.Pastel,
)
