package com.sqz.checklist.ui.common.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Mode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.cache.deleteCacheFileByName
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.ui.common.TextTooltipBox
import com.sqz.checklist.ui.common.dialog.PrimaryDialog
import com.sqz.checklist.ui.common.unit.pxToDpInt
import com.sqz.checklist.ui.common.verticalColumnScrollbar
import com.sqz.checklist.ui.theme.extraSmallInSmallestEdgeSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

@Composable
fun AudioSelector(
    handler: AudioSelector,
    view: View,
    modifier: Modifier = Modifier,
) {
    val dataUri by handler.dataUri.collectAsState()
    val audioName by handler.audioName.collectAsState()
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
                            handler.setPictureName(cursor.getString(nameIndex))
                        }
                    }
                    handler.setDataUri(uri)
                }
            } catch (e: Exception) {
                handler.clear()
                Log.e("AudioHelper", "Failed to select a audio: $e")
                Toast.makeText(
                    view.context, view.context.getString(R.string.failed_large_file_size, "55"),
                    Toast.LENGTH_LONG
                ).show()
                Toast.makeText(
                    view.context, view.context.getString(R.string.report_normal_file_size, "55"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                launcher.launch("audio/*")
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val inPreviewState by handler.inPreviewState.collectAsState()
        val inPreviewAudio = inPreviewState ?: false
        if (dataUri != null) dataUri?.let {
            if (!inPreviewAudio) Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtCard(it, view.context)
                Button(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp), onClick = { handler.setInPreviewState(true) }) {
                    Text(stringResource(R.string.play_audio))
                }
            } else AudioViewDialog(
                onDismissRequest = { handler.setInPreviewState(false) },
                audioName = audioName ?: "Unknown", audioUri = it, title = audioName ?: "Unknown"
            )
        } else Text(
            stringResource(R.string.click_select_audio), color = MaterialTheme.colorScheme.outline
        )
    }
    val audioSizeLimitStr = stringResource(R.string.audio_size_limit)
    if (checkSize != null) LaunchedEffect(Unit) {
        val size = checkSize ?: 1
        if ((size / 1024 / 1024) > 55) {
            handler.clear()
            Toast.makeText(
                view.context, audioSizeLimitStr, Toast.LENGTH_SHORT
            ).show()
        }
        checkSize = null
    }
}

class AudioSelector {
    constructor() // default constructor

    constructor(uriIn: Uri?, nameIn: String?) { // constructor with parameters
        this._dataUri.value = uriIn
        this._audioName.value = nameIn
    }

    private var _dataUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val dataUri = _dataUri.asStateFlow()

    fun setDataUri(uri: Uri) {
        this._dataUri.update { uri }
    }

    private var _audioName: MutableStateFlow<String?> = MutableStateFlow(null)
    val audioName = _audioName.asStateFlow()

    fun setPictureName(string: String) {
        this._audioName.update { string }
    }

    private var _inPreviewState: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val inPreviewState = _inPreviewState.asStateFlow()

    fun setInPreviewState(boolean: Boolean) {
        this._inPreviewState.update { boolean }
    }

    fun clear() {
        this._dataUri.value = null
        this._audioName.value = null
        this._inPreviewState.value = null
    }
}

@Composable
private fun AlbumArtCard(
    uri: Uri, context: Context, modifier: Modifier = Modifier, size: Int = 80
) {
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val artBytes = retriever.embeddedPicture
            albumArt = if (artBytes != null) BitmapFactory.decodeByteArray(
                artBytes, 0, artBytes.size
            ) else null
        } catch (e: Exception) {
            albumArt = null
            e.printStackTrace()
        } finally {
            retriever.release()
        }
    }
    val contentDescription = stringResource(R.string.album_art)
    Card(modifier.size(size.dp, size.dp)) {
        if (albumArt != null) Image(
            albumArt!!.asImageBitmap(), contentDescription
        ) else Image(
            painterResource(R.drawable.music_note), modifier = Modifier.fillMaxSize(),
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun AudioPlayer(uri: Uri, context: Context) = Row {
    val containerSize = LocalWindowInfo.current.containerSize
    val isLandScape =
        containerSize.width > containerSize.height && containerSize.width.pxToDpInt() > 400
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(500L)
        }
    }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) duration = exoPlayer.duration
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    if (isLandScape) AlbumArtCard(uri, context, Modifier.padding(16.dp))
    Column(modifier = Modifier.padding(16.dp)) {
        if (!isLandScape) AlbumArtCard(uri, context, Modifier.align(Alignment.CenterHorizontally))
        Slider(value = currentPosition.toFloat(), onValueChange = { value ->
            currentPosition = value.toLong()
            exoPlayer.seekTo(currentPosition)
        }, valueRange = 0f..duration.toFloat(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            fun Long.toStringSecond(): String {
                val second = this.toSecond()
                return if (second < 10) "0$second" else "$second"
            }
            Text("${currentPosition.toMinute()}:${currentPosition.toStringSecond()} / ${duration.toMinute()}:${duration.toStringSecond()}")
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                } else {
                    exoPlayer.play()
                    isPlaying = true
                }
            }) {
                val buttonText = if (exoPlayer.isPlaying) {
                    stringResource(R.string.pause)
                } else {
                    stringResource(R.string.play)
                }
                Text(
                    text = buttonText,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 5.sp,
                        maxFontSize = LocalTextStyle.current.fontSize
                    )
                )
            }
        }
    }
}

private fun Long.toMinute(): Long = this / 1000 / 60
private fun Long.toSecond(): Long = (this / 1000).let {
    fun second(second: Long): Long = if (second > 59) second(second - 60) else second
    second(it)
}

@Composable
fun AudioViewDialog(
    onDismissRequest: () -> Unit,
    audioName: String,
    audioUri: Uri,
    title: String,
    modifier: Modifier = Modifier,
    openBySystem: Boolean = false,
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var openAudioBySystem by rememberSaveable { mutableStateOf(false) }
    if (openAudioBySystem) ProcessingDialog {
        coroutineScope.launch(Dispatchers.IO) {
            openAudioBySystem(audioName, audioUri, view.context)
            openAudioBySystem = false
        }
    }
    PrimaryDialog(
        onDismissRequest = onDismissRequest, actionButton = {
            if (openBySystem) TextTooltipBox(R.string.open_with) {
                IconButton(onClick = { openAudioBySystem = true }) {
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
            val scrollState = rememberScrollState()
            val showScrollBar = scrollState.canScrollBackward || scrollState.canScrollForward
            val widthDp = LocalWindowInfo.current.containerSize.width.pxToDpInt()
            Column {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaDialogContentHeight())
                        .verticalColumnScrollbar(
                            scrollState = scrollState, endPadding = 24f, topBottomPadding = 50f,
                            scrollBarCornerRadius = 12f,
                            scrollBarTrackColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                0.8f
                            ), scrollBarColor = MaterialTheme.colorScheme.secondary.copy(0.7f),
                            showScrollBar = showScrollBar && widthDp > extraSmallInSmallestEdgeSize
                        ),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.verticalScroll(scrollState)) {
                        AudioPlayer(audioUri, view.context)
                    }
                }
                if (openBySystem) Text(
                    text = audioName,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 10.dp),
                    autoSize = TextAutoSize.StepBased(maxFontSize = LocalTextStyle.current.fontSize),
                    style = TextStyle(
                        lineHeightStyle = LineHeightStyle.Default.copy(mode = Mode.Minimum)
                    ),
                    maxLines = 2
                )
            }
        }, title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.widthIn(max = mediaDialogWidth()),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
fun AudioViewDialog(
    onDismissRequest: () -> Unit,
    byteArray: ByteArray,
    audioName: String,
    title: String,
) {
    if (byteArray.size <= 1) throw IllegalStateException("Invalid byteArray data!")
    AudioViewDialog(
        onDismissRequest = onDismissRequest,
        audioName = audioName,
        audioUri = byteArray.toUri(MainActivity.appDir),
        title = title,
        openBySystem = true
    )
}

@Composable
private fun ProcessingDialog(run: () -> Unit) {
    AlertDialog(onDismissRequest = {}, confirmButton = {}, text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.padding(8.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.padding(5.dp))
            Text(stringResource(R.string.processing))
        }
    })
    run()
}

fun openAudioBySystem(audioName: String, uri: Uri, context: Context) {
    val name = if (audioName == "") "unknown_name" else {
        if (uri.path.toString().endsWith("mp3")
        ) audioName.replace(audioName.substringAfterLast('.', ""), "mp3") else audioName
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
            setDataAndType(uri(file), "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)))
        cache.waitingDeletedCacheName(name)
    } catch (_: FileNotFoundException) {
        Toast.makeText(context, context.getString(R.string.failed_open), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun insertAudio(context: Context, uri: Uri, filesDir: String): Uri {
    val mediaDir = File(filesDir, audioMediaPath)
    if (!mediaDir.exists()) mediaDir.mkdirs()
    val fileName = "AUDIO_${System.currentTimeMillis()}"
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
    return try { // copy file
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        cache.errFileNameSaver(null)
        Uri.fromFile(file)
    } catch (_: FileNotFoundException) {
        Toast.makeText(
            context, context.getString(R.string.detail_file_not_found), Toast.LENGTH_LONG
        ).show()
        errUri
    } catch (e: Exception) {
        e.printStackTrace()
        errFileNameSaver(null)
        errUri
    }
}

@Composable
fun insertAudio(context: Context, uri: Uri): Uri? {
    val coroutineScope = rememberCoroutineScope()
    var rememberUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    if (rememberUri == null) {
        var run by remember { mutableStateOf(false) }
        ProcessingDialog {
            if (!run) {
                run = true
                coroutineScope.launch(Dispatchers.IO) {
                    rememberUri = insertAudio(context, uri, MainActivity.appDir)
                }
            }
        }
    }
    if (rememberUri == errUri) Toast.makeText(
        context, stringResource(R.string.failed_add_picture), Toast.LENGTH_LONG
    ).show()
    return rememberUri
}
