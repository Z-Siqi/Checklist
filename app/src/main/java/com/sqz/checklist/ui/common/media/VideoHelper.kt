package com.sqz.checklist.ui.common.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import com.sqz.checklist.R
import com.sqz.checklist.cache.deleteCacheFileByName
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.PrimaryDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import sqz.checklist.data.preferences.PreferencesInCache
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

@Composable
fun VideoViewDialog(
    onDismissRequest: () -> Unit,
    videoName: String,
    videoUri: Uri,
    title: String,
    modifier: Modifier = Modifier,
    openBySystem: Boolean = false,
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var openVideoBySystem by rememberSaveable { mutableStateOf(false) }
    if (openVideoBySystem) ProcessingDialog {
        coroutineScope.launch(Dispatchers.IO) {
            openVideoBySystem(videoName, videoUri, view.context)
            openVideoBySystem = false
        }
    }
    PrimaryDialog(
        onDismissRequest = onDismissRequest, actionButton = {
            if (openBySystem) TextTooltipBox(R.string.open_with) {
                IconButton(onClick = { openVideoBySystem = true }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painterResource(R.drawable.open_in_new),
                            stringResource(R.string.open_with)
                        )
                    }
                }
            }
            Spacer(modifier.weight(1f))
            TextButton(onClick = {
                onDismissRequest()
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }) { Text(text = stringResource(R.string.cancel)) }
        }, content = {
            Column {
                OutlinedCard(
                    modifier = Modifier
                        .requiredHeightIn(min = 100.dp)
                        .height(mediaDialogContentHeight()),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.inverseSurface)
                ) {
                    val playerHost = remember {
                        MediaPlayerHost(
                            mediaUrl = videoUri.toString(),
                            isLooping = false,
                            initialVideoFitMode = ScreenResize.FIT
                        )
                    }
                    val playerConfig = VideoPlayerConfig(
                        isZoomEnabled = false,
                        isFullScreenEnabled = false,
                        showVideoQualityOptions = false,
                        enableFullEdgeToEdge = false,
                        enablePIPControl = false,
                        fastForwardBackwardIntervalSeconds = 5F,
                    )
                    VideoPlayerComposable(
                        modifier = Modifier.fillMaxSize(),
                        playerHost = playerHost,
                        playerConfig = playerConfig
                    )
                }
                if (openBySystem) Text(
                    videoName, modifier.align(Alignment.End) then modifier.padding(end = 10.dp)
                )
            }
        }, title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.widthIn(max = mediaDialogWidth()),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
fun VideoViewDialog(
    onDismissRequest: () -> Unit,
    byteArray: ByteArray,
    videoName: String,
    title: String,
) {
    if (byteArray.size <= 1) throw IllegalStateException("Invalid byteArray data!")
    VideoViewDialog(
        onDismissRequest = onDismissRequest,
        videoName = videoName,
        videoUri = Uri.fromFile((byteArray.decodeToString().toPath()).toFile()),
        title = title,
        openBySystem = true
    )
}

fun openVideoBySystem(videoName: String, uri: Uri, context: Context) {
    val name = if (videoName == "") "unknown_name" else {
        if (uri.path.toString().endsWith("mp4")
        ) videoName.replace(videoName.substringAfterLast('.', ""), "mp4") else videoName
    }
    val cache = PreferencesInCache(context)
    val getCacheName = cache.waitingDeletedCacheName()
    if (getCacheName != null && getCacheName != name) {
        deleteCacheFileByName(context, getCacheName)
        cache.waitingDeletedCacheName(null)
    }
    try {
        val file = File(context.cacheDir, name)
        fun uri(file: File): Uri {
            val saved = File(uri.path!!)
            val inputStream = FileInputStream(saved)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri(file), "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)))
        cache.waitingDeletedCacheName(name)
    } catch (e: FileNotFoundException) {
        Toast.makeText(context, context.getString(R.string.failed_open), Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
private fun ProcessingDialog(loading: Int? = null, run: () -> Unit) {
    AlertDialog(onDismissRequest = {}, confirmButton = {}, text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.padding(8.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.padding(5.dp))
            if (loading != null) {
                Text("$loading %")
            }
            Text(stringResource(R.string.processing))
        }
    })
    run()
}
