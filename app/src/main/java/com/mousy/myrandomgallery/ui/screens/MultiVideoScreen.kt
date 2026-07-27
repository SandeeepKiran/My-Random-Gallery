package com.mousy.myrandomgallery.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MultiVideoCell
import com.mousy.myrandomgallery.data.model.MultiVideoState
import com.mousy.myrandomgallery.ui.components.EmptyFoldersState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.net.Uri

/** DEVICE-ONLY: Multi-video grid with ExoPlayer instances and landscape lock handled in Activity. */
@Composable
fun MultiVideoScreen(
    state: MultiVideoState,
    videos: List<MediaItem>,
    noFolders: Boolean,
    onToggleLandscape: () -> Unit,
    onExitLandscape: () -> Unit,
    onSetCount: (Int) -> Unit,
    onPlayAll: () -> Unit,
    onPauseAll: () -> Unit,
    onMuteAll: () -> Unit,
    onCellTap: (Int) -> Unit,
    onTogglePlay: (Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onChooseVideo: (Int) -> Unit,
    onProgress: (Int, Float) -> Unit,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (state.landscape) Modifier else Modifier.padding(horizontal = 12.dp)),
        ) {
            if (!state.landscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Multi-Video", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onToggleLandscape) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = "Landscape")
                    }
                    Row(
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.shapes.large,
                        ),
                    ) {
                        listOf(1, 2, 4).forEach { count ->
                            val selected = state.count == count
                            Surface(
                                onClick = { onSetCount(count) },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Text(
                                    "$count",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (noFolders) {
                EmptyFoldersState(onChooseFolders = onGoSettings)
            } else {
                if (!state.landscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onPlayAll, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, null)
                            Text("Play all", modifier = Modifier.padding(start = 4.dp))
                        }
                        OutlinedButton(onClick = onPauseAll, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Pause, null)
                            Text("Pause all", modifier = Modifier.padding(start = 4.dp))
                        }
                        OutlinedButton(onClick = onMuteAll) {
                            Icon(
                                if (state.muteAll) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute all",
                            )
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val cols = if (state.count == 4) 2 else 1
                    val rows = if (state.count == 4) 2 else state.count
                    val cellHeight = if (state.landscape) maxHeight / rows else null
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (state.landscape) Modifier else Modifier.padding(top = 12.dp)),
                        horizontalArrangement = Arrangement.spacedBy(if (state.landscape) 0.dp else 14.dp),
                        verticalArrangement = Arrangement.spacedBy(if (state.landscape) 0.dp else 14.dp),
                    ) {
                        itemsIndexed(state.cells.take(state.count)) { index, cell ->
                            MultiVideoCellCard(
                                cell = cell,
                                videoUri = cell.uri?.let { Uri.parse(it) }
                                    ?: videos.find { it.id == cell.mediaId }?.uri,
                                overlayVisible = state.overlayVisible && (!state.landscape || state.chromeVisible),
                                landscape = state.landscape,
                                cellHeight = cellHeight,
                                onTap = { onCellTap(index) },
                                onTogglePlay = { onTogglePlay(index) },
                                onToggleMute = { onToggleMute(index) },
                                onChoose = { onChooseVideo(index) },
                                onProgress = { p -> onProgress(index, p) },
                            )
                        }
                    }
                }
            }
        }

        if (state.landscape && state.chromeVisible) {
            Surface(
                onClick = onExitLandscape,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Text("Exit", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun MultiVideoCellCard(
    cell: MultiVideoCell,
    videoUri: Uri?,
    overlayVisible: Boolean,
    landscape: Boolean,
    cellHeight: androidx.compose.ui.unit.Dp?,
    onTap: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onChoose: () -> Unit,
    onProgress: (Float) -> Unit,
) {
    val hasVideo = videoUri != null
    Surface(
        shape = if (landscape) androidx.compose.foundation.shape.RoundedCornerShape(0.dp) else MaterialTheme.shapes.large,
        tonalElevation = if (landscape) 0.dp else 2.dp,
        modifier = cellHeight?.let { Modifier.height(it) } ?: Modifier,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (landscape) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f))
                    .clickable(onClick = onTap),
                contentAlignment = Alignment.Center,
            ) {
                if (!hasVideo) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (cell.isAudio) Icons.Default.AudioFile else Icons.Default.AddToQueue,
                            null,
                            modifier = Modifier.padding(8.dp),
                        )
                        Text(
                            if (cell.isAudio) "Choose audio" else "Choose media",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                } else {
                    MultiVideoPlayer(
                        uri = videoUri!!,
                        muted = cell.muted,
                        playing = cell.playing,
                        onProgress = onProgress,
                    )
                    if (cell.isAudio) {
                        Icon(
                            Icons.Default.AudioFile,
                            contentDescription = "Audio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                if (overlayVisible && hasVideo) {
                    Icon(
                        if (cell.playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        null,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            if (overlayVisible && hasVideo) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onTogglePlay) {
                        Icon(if (cell.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    }
                    IconButton(onClick = onToggleMute) {
                        Icon(if (cell.muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null)
                    }
                    LinearProgressIndicator(
                        progress = { cell.progress },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                    IconButton(onClick = onChoose) {
                        Icon(Icons.Default.Movie, contentDescription = "Change video")
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiVideoPlayer(
    uri: Uri,
    muted: Boolean,
    playing: Boolean,
    onProgress: (Float) -> Unit,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }

    LaunchedEffect(player, playing) {
        while (isActive) {
            delay(250)
            val dur = player.duration
            if (dur > 0) {
                onProgress((player.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f))
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}
