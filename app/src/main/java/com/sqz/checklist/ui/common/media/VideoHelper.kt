package com.sqz.checklist.ui.common.media

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.coroutineScope
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.ScreenResize
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.cache.deleteCacheFileByName
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.preferences.PrimaryPreferences
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.PrimaryDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

@Composable
fun VideoSelector(
    handler: VideoSelector,
    view: View,
    modifier: Modifier = Modifier,
) {
    val dataUri by handler.dataUri.collectAsState()
    val videoName by handler.videoName.collectAsState()
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
                            handler.setVideoName(cursor.getString(nameIndex))
                        }
                    }
                    handler.setDataUri(uri)
                }
            } catch (e: Exception) {
                handler.clear()
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
        val inPreviewState by handler.inPreviewState.collectAsState()
        val inPreviewVideo = inPreviewState ?: false
        if (dataUri != null) dataUri?.let {
            if (!inPreviewVideo) Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                var rememberUri by remember { mutableStateOf(it) }
                var previewView by remember { mutableStateOf(false) }
                if (rememberUri != it) LaunchedEffect(Unit) {
                    previewView = !previewView
                    rememberUri = it
                }
                if (previewView) {
                    VideoPreviewView(rememberUri)
                } else {
                    VideoPreviewView(rememberUri)
                }
                Spacer(modifier = Modifier.fillMaxSize() then Modifier.clickable {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    launcher.launch("video/*")
                })
                Button(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp), onClick = { handler.setInPreviewState(true) }) {
                    Text(stringResource(R.string.play_video))
                }
            } else VideoViewDialog(
                onDismissRequest = { handler.setInPreviewState(false) },
                videoName = videoName ?: "Unknown", videoUri = it, title = videoName ?: "Unknown"
            )
        } else Text(
            stringResource(R.string.click_select_video), color = MaterialTheme.colorScheme.outline
        )
    }
    val videoSizeLimitStr = stringResource(R.string.video_size_limit)
    if (checkSize != null) LaunchedEffect(Unit) {
        val size = checkSize ?: 1
        if ((size / 1024 / 1024) > 350) {
            handler.clear()
            Toast.makeText(
                view.context, videoSizeLimitStr, Toast.LENGTH_SHORT
            ).show()
        }
        checkSize = null
    }
}

@Composable
private fun VideoPreviewView(uri: Uri) {
    val playerHost = remember {
        MediaPlayerHost(
            mediaUrl = uri.toString(),
            isLooping = true,
            isMuted = true,
            initialVideoFitMode = ScreenResize.FIT
        )
    }
    val playerConfig = VideoPlayerConfig(
        showControls = false,
        isZoomEnabled = false,
        isFullScreenEnabled = false,
        enablePIPControl = false,
    )
    VideoPlayerComposable(
        modifier = Modifier.fillMaxSize(),
        playerHost = playerHost,
        playerConfig = playerConfig
    )
}

class VideoSelector {
    constructor() // default constructor

    constructor(uriIn: Uri?, nameIn: String?) { // constructor with parameters
        this._dataUri.value = uriIn
        this._videoName.value = nameIn
    }

    private var _dataUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val dataUri = _dataUri.asStateFlow()

    fun setDataUri(uri: Uri) {
        this._dataUri.update { uri }
    }

    private var _videoName: MutableStateFlow<String?> = MutableStateFlow(null)
    val videoName = _videoName.asStateFlow()

    fun setVideoName(string: String) {
        this._videoName.update { string }
    }

    private var _inPreviewState: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val inPreviewState = _inPreviewState.asStateFlow()

    fun setInPreviewState(boolean: Boolean) {
        this._inPreviewState.update { boolean }
    }

    fun clear() {
        this._dataUri.value = null
        this._videoName.value = null
        this._inPreviewState.value = null
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
        e.printStackTrace()
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
            } catch (_: Exception) {
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
        e.printStackTrace()
        onComplete(errUri)
    } catch (e: Exception) {
        e.printStackTrace()
        errFileNameSaver(null)
        onComplete(errUri)
    }
}

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
