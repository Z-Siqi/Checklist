package com.sqz.checklist.ui.common.media

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sqz.checklist.ui.common.unit.pxToDp
import com.sqz.checklist.ui.common.unit.pxToDpInt

val errUri = "ERROR".toUri()

const val pictureMediaPath = "media/picture/"

const val videoMediaPath = "media/video/"

const val audioMediaPath = "media/audio/"

fun Uri.toByteArray(): ByteArray {
    return this.toString().toByteArray(Charsets.UTF_8)
}

fun ByteArray.toUri(): Uri {
    return String(this, Charsets.UTF_8).toUri()
}

fun ByteArray.toUri(filesDir: String): Uri {
    val regex = Regex("file:///.*/files/")
    return String(this, Charsets.UTF_8).replace(regex, "file://$filesDir/").toUri()
}

@ReadOnlyComposable
@Composable
internal fun mediaDialogContentHeight(): Dp {
    val containerSize = LocalWindowInfo.current.containerSize
    val heightPx: Int = containerSize.height
    val widthPx: Int = containerSize.width
    val heightDp = heightPx.pxToDp()
    val widthDp = widthPx.pxToDp()
    return when {
        heightDp >= 700.dp -> (heightPx / 4.1f).pxToDp().let { if (it > widthDp) widthDp else it }
        heightDp < (widthPx / 1.2f).pxToDp() -> (heightPx / 3.2f).pxToDp()
        else -> (heightPx / 5.1f).pxToDp()
    }
}

@ReadOnlyComposable
@Composable
internal fun mediaDialogWidth(): Dp {
    val containerSize = LocalWindowInfo.current.containerSize
    return (containerSize.width.pxToDpInt() * 0.9).dp
}
