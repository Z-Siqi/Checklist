package sqz.checklist.data

import sqz.checklist.data.database.model.Platform

actual fun platform() = Platform.Desktop.toString()
