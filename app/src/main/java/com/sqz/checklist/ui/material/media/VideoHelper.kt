package com.sqz.checklist.ui.material.media

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntRange
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.coroutineScope
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.cache.deleteCacheFileByName
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailData
import com.sqz.checklist.ui.main.task.layout.function.toUri
import com.sqz.checklist.ui.material.TextTooltipBox
import io.sanghun.compose.video.RepeatMode
import io.sanghun.compose.video.VideoPlayer
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

@Composable
fun VideoSelector(
    detailData: TaskDetailData,
    view: View,
    modifier: Modifier = Modifier,
) {
    val detailDataUri by detailData.detailUri().collectAsState()
    val detailDataString by detailData.detailString().collectAsState()
    var checkSize by remember { mutableStateOf<Long?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            try {
                uri?.let {
                    view.context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            checkSize = cursor.getLong(sizeIndex)
                        }
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            detailData.detailString(cursor.getString(nameIndex))
                        }
                    }
                    detailData.detailUri(uri)
                }
            } catch (e: Exception) {
                detailData.releaseMemory()
                Log.e("VideoHelper", "Failed to select a video: $e")
                Toast.makeText(
                    view.context, view.context.getString(R.string.failed_large_file_size, "350"),
                    Toast.LENGTH_LONG
                ).show()
                Toast.makeText(
                    view.context, view.context.getString(R.string.report_normal_file_size, "350"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                launcher.launch("video/*")
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val inPreviewState by detailData.inPreviewState().collectAsState()
        val inPreviewVideo = inPreviewState ?: false
        if (detailDataUri != null) detailDataUri?.let {
            if (!inPreviewVideo) Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                VideoPlayer(
                    mediaItems = listOf(VideoPlayerMediaItem.StorageMediaItem(it)),
                    repeatMode = RepeatMode.ALL, usePlayerController = false, volume = 0f
                )
                Button(modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp), onClick = { detailData.inPreviewState(true) }) {
                    Text(stringResource(R.string.play_video))
                }
            } else VideoViewDialog(
                onDismissRequest = { detailData.inPreviewState(false) },
                videoName = detailDataString, videoUri = it, title = detailDataString
            )
        } else Text(
            stringResource(R.string.click_select_video), color = MaterialTheme.colorScheme.outline
        )
    }
    if (checkSize != null) {
        val size = checkSize ?: 1
        if ((size / 1024 / 1024) > 350) {
            detailData.releaseMemory()
            Toast.makeText(
                view.context, stringResource(R.string.video_size_limit), Toast.LENGTH_SHORT
            ).show()
        }
        checkSize = null
    }
}

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
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val height = when {
        screenHeightDp >= 700 -> (screenHeightDp / 3.8).toInt()
        screenHeightDp < (LocalConfiguration.current.screenWidthDp / 1.2) -> (screenHeightDp / 2.1).toInt()
        else -> (screenHeightDp / 3.1).toInt()
    }
    val coroutineScope = rememberCoroutineScope()
    var openVideoBySystem by rememberSaveable { mutableStateOf(false) }
    if (openVideoBySystem) ProcessingDialog {
        coroutineScope.launch(Dispatchers.IO) {
            openVideoBySystem(videoName, videoUri, view.context)
            openVideoBySystem = false
        }
    }
    AlertDialog(
        onDismissRequest = onDismissRequest, confirmButton = {
            Row {
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
            }
        }, text = {
            Column {
                OutlinedCard(
                    modifier = modifier.fillMaxWidth() then modifier.height(height.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    VideoPlayer(
                        modifier = modifier.fillMaxSize(),
                        mediaItems = listOf(VideoPlayerMediaItem.StorageMediaItem(videoUri)),
                        autoPlay = false,
                        controllerConfig = VideoPlayerControllerConfig(
                            showSpeedAndPitchOverlay = true, showSubtitleButton = false,
                            showCurrentTimeAndTotalTime = true, showBufferingProgress = false,
                            showForwardIncrementButton = true, showBackwardIncrementButton = true,
                            showBackTrackButton = false, showNextTrackButton = false,
                            showRepeatModeButton = false, controllerShowTimeMilliSeconds = 5_000,
                            controllerAutoShow = true, showFullScreenButton = false,
                        ),
                    )
                }
                if (openBySystem) Text(
                    videoName, modifier.align(Alignment.End) then modifier.padding(end = 10.dp)
                )
            }
        }, title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.widthIn(max = (LocalConfiguration.current.screenWidthDp * 0.9).dp),
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
        videoUri = byteArray.toUri(MainActivity.appDir),
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
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun insertVideo(
    context: Context, uri: Uri, @IntRange(0, 100) compressionRate: Int,
    onComplete: (Uri) -> Unit, loading: (Double) -> Unit, time: Long?
) {
    val mediaDir = File(context.filesDir, videoMediaPath)
    if (!mediaDir.exists()) mediaDir.mkdirs()
    val fileName = when {
        uri.path?.endsWith(".mp4") ?: false -> "VIDEO_${time}.mp4"
        else -> "VIDEO_${time}"
    }
    val cache = PreferencesInCache(context)
    fun errFileNameSaver(name: String?) { // clear invalid file
        cache.errFileNameSaver()?.let {
            val file = File(mediaDir, it)
            if (file.exists()) file.delete()
        }
        cache.errFileNameSaver(name)
    }
    errFileNameSaver(fileName)
    val file = File(mediaDir, fileName)
    try { // insert action
        if (compressionRate == 0) { // just copy
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            cache.errFileNameSaver(null)
            onComplete(Uri.fromFile(file))
        } else { // with compression
            val defaultBitrate = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)!!.toLong()
                    .let {
                        Log.d("VideoBitrate", "$it")
                        it
                    }
            } catch (e: Exception) {
                Log.d("VideoBitrate", "Failed to get! Use default: 5800000L")
                5800000L
            }
            fun targetBitrate(bitrate: Long, rate: Int): Long {
                val recursion = (rate / 1.5).toInt()
                return if (recursion > 1) targetBitrate(
                    (bitrate * (100 - rate) / 100), recursion
                ) else bitrate
            }
            Transcoder.into(file.absolutePath).apply {
                addDataSource(context, uri)
                setVideoTrackStrategy(
                    DefaultVideoStrategy.Builder()
                        .bitRate(targetBitrate(defaultBitrate, compressionRate))
                        .build()
                )
                setListener(object : TranscoderListener {
                    override fun onTranscodeProgress(progress: Double) {
                        loading(progress * 100)
                    }

                    override fun onTranscodeCompleted(successCode: Int) {
                        cache.errFileNameSaver(null)
                        onComplete(Uri.fromFile(file))
                    }

                    override fun onTranscodeCanceled() {
                        errFileNameSaver(null)
                        onComplete(errUri)
                    }

                    override fun onTranscodeFailed(exception: Throwable) {
                        errFileNameSaver(null)
                        onComplete(errUri)
                        Log.e("insertVideo", "$exception")
                    }
                })
                transcode()
            }
        }
    } catch (e: FileNotFoundException) {
        Toast.makeText(
            context, context.getString(R.string.detail_file_not_found), Toast.LENGTH_LONG
        ).show()
        errFileNameSaver(null)
        onComplete(errUri)
    } catch (e: Exception) {
        e.printStackTrace()
        errFileNameSaver(null)
        onComplete(errUri)
    }
} //TODO: ERR process (file) / program close within process will make invalid file

@Composable
fun insertVideo(context: Context, uri: Uri, ignoreCompressSettings: Boolean = false): Uri? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var rememberUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var rememberLoading by rememberSaveable { mutableStateOf<Int?>(null) }
    val preference = PrimaryPreferences(context)
    val compression = when {
        ignoreCompressSettings -> 0
        preference.videoCompressionRate() > 100 -> 100
        preference.videoCompressionRate() <= 0 -> 0
        else -> preference.videoCompressionRate()
    }
    if (rememberUri == null) {
        var timeState by rememberSaveable { mutableStateOf<Long?>(null) }
        var run by rememberSaveable { mutableStateOf(false) }
        ProcessingDialog(rememberLoading) {
            lifecycleOwner.lifecycle.coroutineScope.launch(Dispatchers.IO) {
                if (!run) {
                    run = true
                    timeState = System.currentTimeMillis()
                    insertVideo(context, uri, compression, onComplete = {
                        rememberUri = it
                    }, loading = { rememberLoading = it.toInt() }, time = timeState)
                }
            }
        }
    }
    if (rememberUri == errUri) Toast.makeText(
        context, stringResource(R.string.failed_add_video), Toast.LENGTH_LONG
    ).show()
    return rememberUri
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
