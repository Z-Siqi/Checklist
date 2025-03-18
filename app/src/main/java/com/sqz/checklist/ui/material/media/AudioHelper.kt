package com.sqz.checklist.ui.material.media

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.cache.deleteCacheFileByName
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailData
import com.sqz.checklist.ui.main.task.layout.function.toUri
import com.sqz.checklist.ui.material.TextTooltipBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

@Composable
fun AudioSelector(
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
        val inPreviewState by detailData.inPreviewState().collectAsState()
        val inPreviewVideo = inPreviewState ?: false
        if (detailDataUri != null) detailDataUri?.let {
            if (!inPreviewVideo) Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtCard(it, view.context)
                Button(modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp), onClick = { detailData.inPreviewState(true) }) {
                    Text(stringResource(R.string.play_audio))
                }
            } else AudioViewDialog(
                onDismissRequest = { detailData.inPreviewState(false) },
                audioName = detailDataString, audioUri = it, title = detailDataString
            )
        } else Text(
            stringResource(R.string.click_select_audio), color = MaterialTheme.colorScheme.outline
        )
    }
    if (checkSize != null) {
        val size = checkSize ?: 1
        if ((size / 1024 / 1024) > 350) {
            detailData.releaseMemory()
            Toast.makeText(
                view.context, stringResource(R.string.audio_size_limit), Toast.LENGTH_SHORT
            ).show()
        }
        checkSize = null
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
    val config = LocalConfiguration.current
    val isLandScape = config.screenWidthDp > config.screenHeightDp && config.screenWidthDp > 400
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
    fun Long.toMinute(): Long = this / 1000 / 60
    fun Long.toSecond(): Long = (this / 1000).let {
        fun second(second: Long): Long = if (second > 59) second(second - 60) else second
        second(it)
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
            Button(
                onClick = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                }
            ) { Text(if (exoPlayer.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play)) }
        }
    }
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
            openAudioBySystem(audioName, audioUri, view.context)
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
                ) { AudioPlayer(audioUri, view.context) }
                if (openBySystem) Text(
                    audioName, modifier.align(Alignment.End) then modifier.padding(end = 10.dp)
                )
            }
        }, title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.widthIn(max = (LocalConfiguration.current.screenWidthDp * 0.9).dp),
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
    } catch (e: FileNotFoundException) {
        Toast.makeText(context, context.getString(R.string.failed_open), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun insertAudio(context: Context, uri: Uri, filesDir: String): Uri {
    val mediaDir = File(filesDir, "media/audio/")
    if (!mediaDir.exists()) mediaDir.mkdirs()
    val fileName = "AUDIO_${System.currentTimeMillis()}"
    val file = File(mediaDir, fileName)
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: FileNotFoundException) {
        Toast.makeText(
            context, context.getString(R.string.detail_file_not_found), Toast.LENGTH_LONG
        ).show()
        errUri
    } catch (e: Exception) {
        e.printStackTrace()
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
