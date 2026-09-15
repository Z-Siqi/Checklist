package sqz.checklist.model.settings

import kotlinx.serialization.Serializable

/** Settings that control media compression. */
@Serializable
data class MediaSettings(
    val pictureCompressionPercent: Int = 20,
    val videoCompressionPercent: Int = 0,
)
