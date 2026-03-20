package sqz.checklist.data.database.model

enum class Platform {
    Android, IOS, Desktop;

    override fun toString(): String {
        return when (this) {
            Android -> "Android"
            IOS -> "iOS"
            Desktop -> "Desktop"
        }
    }
}