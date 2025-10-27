package com.sqz.checklist.ui.common.media

import android.net.Uri
import androidx.core.net.toUri

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
