package sqz.checklist.data.storage

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Source

// not implemented

@Suppress("unused")
actual fun appInternalDirPath(type: AppDirType): String {
    val home = System.getProperty("user.home")
    val app = ".Checklist"
    val path = when (type) {
        AppDirType.Data -> "$home/$app"
        AppDirType.Cache -> "$home/.cache/$app"
        AppDirType.Temp -> System.getProperty("java.io.tmpdir")
    }
    java.io.File(path).mkdirs()
    return path
}

internal actual fun getFileLastModified(path: Path): Long {
    return path.toFile().lastModified()
}

@Suppress("unused")
fun sourceFromPath(path: String): Source {
    return FileSystem.SYSTEM.source(path.toPath())
}
