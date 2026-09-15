package sqz.checklist.data.settings.datastore

import sqz.checklist.model.settings.AppSettings

/** Returns this snapshot after enforcing value ranges required by persistence. */
internal fun AppSettings.requireValid(): AppSettings = apply {
    require(history.allowedNumberOfHistory in 0..21)
    require(media.pictureCompressionPercent in 0..100)
    require(media.videoCompressionPercent in 0..100)
    require(reminder.recentlyRemindedKeepTimeMillis >= 0L)
}
