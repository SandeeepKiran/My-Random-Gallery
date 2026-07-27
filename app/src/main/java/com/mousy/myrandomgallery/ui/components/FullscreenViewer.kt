package com.mousy.myrandomgallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.graphics.painter.ColorPainter
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MediaType
import com.mousy.myrandomgallery.data.model.SlideshowSpeeds
import com.mousy.myrandomgallery.ui.theme.FavouriteHeart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val MenuShape = RoundedCornerShape(20.dp)
private const val CHROME_AUTO_HIDE_MS = 5_000L

/**
 * Fullscreen viewer. Top/bottom chrome is an overlay (fade only) so media stays full-bleed
 * and does not re-layout when chrome appears or vanishes.
 */
@Composable
fun FullscreenViewer(
    item: MediaItem?,
    index: Int,
    count: Int,
    isPlaying: Boolean,
    chromeVisible: Boolean,
    menuOpen: Boolean,
    speedMenuOpen: Boolean,
    speedIndex: Int,
    customMs: Long,
    isFavourite: Boolean,
    disableSwipeDelete: Boolean,
    deleteEnabled: Boolean = true,
    slideshowMode: Boolean = true,
    muted: Boolean = false,
    loopVideos: Boolean = true,
    onClose: () -> Unit,
    onToggleChrome: () -> Unit,
    onNavigate: (Int) -> Unit,
    onSwipeUpDelete: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleMenu: () -> Unit,
    onToggleSpeedMenu: () -> Unit,
    onSpeedSelected: (Int) -> Unit,
    onToggleFavourite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onToggleMute: () -> Unit = {},
    onVideoEnded: () -> Unit = {},
    onUserInteracted: () -> Unit = {},
    chromeAutoHideNonce: Int = 0,
    modifier: Modifier = Modifier,
) {
    var navDirection by remember { mutableIntStateOf(0) }

    // Auto-hide chrome after inactivity while visible
    LaunchedEffect(chromeVisible, item?.stableKey, menuOpen, speedMenuOpen, chromeAutoHideNonce) {
        if (!chromeVisible || menuOpen || speedMenuOpen) return@LaunchedEffect
        delay(CHROME_AUTO_HIDE_MS)
        onToggleChrome()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Media layer — always full-bleed; never padded by chrome
        AnimatedContent(
            targetState = item,
            transitionSpec = {
                val dir = navDirection
                val enterSpring = spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
                val exitSpring = spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
                if (dir == 0) {
                    fadeIn(enterSpring) togetherWith fadeOut(exitSpring)
                } else {
                    val enter = slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        initialOffsetX = { full -> if (dir > 0) full else -full },
                    ) + fadeIn(enterSpring)
                    val exit = slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        targetOffsetX = { full -> if (dir > 0) -full / 3 else full / 3 },
                    ) + fadeOut(exitSpring)
                    enter togetherWith exit
                }
            },
            label = "viewerPage",
            modifier = Modifier.fillMaxSize(),
        ) { pageItem ->
            when (pageItem?.mediaType) {
                MediaType.VIDEO, MediaType.AUDIO -> MediaPlayerSurface(
                    item = pageItem,
                    muted = muted,
                    loopVideos = loopVideos,
                    slideshowMode = slideshowMode,
                    controlsVisible = chromeVisible,
                    onToggleChrome = {
                        onUserInteracted()
                        onToggleChrome()
                    },
                    onNavigate = { delta ->
                        navDirection = delta
                        onNavigate(delta)
                    },
                    onSwipeUpDelete = onSwipeUpDelete,
                    disableSwipeDelete = disableSwipeDelete,
                    onToggleMute = onToggleMute,
                    onTogglePlayLocal = { onUserInteracted() },
                    onVideoEnded = onVideoEnded,
                    onSeekInteracted = onUserInteracted,
                )
                null -> Unit
                else -> ZoomableImageSurface(
                    item = pageItem,
                    onToggleChrome = {
                        onUserInteracted()
                        onToggleChrome()
                    },
                    onNavigate = { delta ->
                        navDirection = delta
                        onNavigate(delta)
                    },
                    onSwipeUpDelete = onSwipeUpDelete,
                    disableSwipeDelete = disableSwipeDelete,
                )
            }
        }

        // Top chrome overlay — fades without changing media layout
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.62f), Color.Transparent)))
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Text(
                    text = item?.let { formatDate(it.dateTakenMs, it.dateAddedMs) } ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (slideshowMode) {
                    IconButton(onClick = {
                        onUserInteracted()
                        onTogglePlay()
                    }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = Color.White,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .widthIn(min = 56.dp)
                            .clickable(onClick = {
                                onUserInteracted()
                                onToggleSpeedMenu()
                            })
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Speed, null, tint = Color.White)
                        Text(
                            speedLabel(speedIndex, customMs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp),
                            maxLines = 1,
                        )
                    }
                }
                IconButton(onClick = {
                    onUserInteracted()
                    onToggleFavourite()
                }) {
                    Icon(
                        Icons.Default.Favorite,
                        null,
                        tint = if (isFavourite) FavouriteHeart else Color.White.copy(alpha = 0.85f),
                    )
                }
                IconButton(onClick = {
                    onUserInteracted()
                    onToggleMenu()
                }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 8.dp),
        ) {
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = onToggleMenu,
                shape = MenuShape,
            ) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Default.Share, null) },
                    onClick = { onShare(); onToggleMenu() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    enabled = deleteEnabled,
                    onClick = {
                        if (deleteEnabled) {
                            onDelete()
                            onToggleMenu()
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text("Details") },
                    leadingIcon = { Icon(Icons.Default.Info, null) },
                    onClick = { onDetails(); onToggleMenu() },
                )
            }
        }

        if (slideshowMode) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                DropdownMenu(
                    expanded = speedMenuOpen,
                    onDismissRequest = onToggleSpeedMenu,
                    shape = MenuShape,
                ) {
                    SlideshowSpeeds.speeds.forEachIndexed { i, speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (i) {
                                        SlideshowSpeeds.CUSTOM_INDEX -> "Custom…"
                                        SlideshowSpeeds.OFF_INDEX -> "Off"
                                        else -> speed.label
                                    },
                                )
                            },
                            onClick = { onSpeedSelected(i) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImageSurface(
    item: MediaItem,
    onToggleChrome: () -> Unit,
    onNavigate: (Int) -> Unit,
    onSwipeUpDelete: () -> Unit,
    disableSwipeDelete: Boolean,
) {
    var scale by remember(item.stableKey) { mutableFloatStateOf(1f) }
    var offset by remember(item.stableKey) { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current

    val placeholder = remember { ColorPainter(Color.Black) }
    val imageModel = remember(item.uri, item.stableKey) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(Size.ORIGINAL)
            .memoryCacheKey("${item.stableKey}_full")
            .diskCacheKey("${item.stableKey}_full")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.stableKey) {
                detectTapGestures(
                    onTap = { onToggleChrome() },
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            offset = Offset(
                                (cx - tapOffset.x) * (scale - 1f),
                                (cy - tapOffset.y) * (scale - 1f),
                            )
                        }
                    },
                )
            }
            .pointerInput(item.stableKey, disableSwipeDelete) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalPan = Offset.Zero
                    var multipoint = false
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            multipoint = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            if (newScale > 1f) {
                                val maxX = (size.width * (newScale - 1f)) / 2f
                                val maxY = (size.height * (newScale - 1f)) / 2f
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                            } else {
                                offset = Offset.Zero
                            }
                            scale = newScale
                            pressed.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pressed.size == 1 && scale > 1.01f) {
                            val pan = event.calculatePan()
                            val maxX = (size.width * (scale - 1f)) / 2f
                            val maxY = (size.height * (scale - 1f)) / 2f
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY),
                            )
                            pressed.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pressed.size == 1 && scale <= 1.01f) {
                            val pan = event.calculatePan()
                            totalPan += pan
                        }
                    } while (event.changes.any { it.pressed })

                    if (!multipoint && scale <= 1.01f) {
                        val absX = abs(totalPan.x)
                        val absY = abs(totalPan.y)
                        when {
                            absX > 72f && absX > absY * 1.15f ->
                                onNavigate(if (totalPan.x < 0) 1 else -1)
                            absY > 100f && absY > absX && totalPan.y < 0 && !disableSwipeDelete ->
                                onSwipeUpDelete()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            placeholder = placeholder,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}

/**
 * Video/audio with custom Material 3 control bar (mute, scrubber, play/pause, immersive).
 * PlayerView controllers are disabled — chrome is Compose overlay so layout stays stable.
 */
@Composable
private fun MediaPlayerSurface(
    item: MediaItem,
    muted: Boolean,
    loopVideos: Boolean,
    slideshowMode: Boolean,
    controlsVisible: Boolean,
    onToggleChrome: () -> Unit,
    onNavigate: (Int) -> Unit,
    onSwipeUpDelete: () -> Unit,
    disableSwipeDelete: Boolean,
    onToggleMute: () -> Unit,
    onTogglePlayLocal: () -> Unit,
    onVideoEnded: () -> Unit,
    onSeekInteracted: () -> Unit,
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var playing by remember(item.stableKey) { mutableStateOf(true) }
    var positionMs by remember(item.stableKey) { mutableLongStateOf(0L) }
    var durationMs by remember(item.stableKey) { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    DisposableEffect(item.uri, item.stableKey, loopVideos, slideshowMode) {
        val exo = ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            // View mode: loop setting loops or stops. Slideshow: loop in place when enabled.
            repeatMode = if (loopVideos) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !loopVideos) {
                    onVideoEnded()
                }
            }
        }
        exo.addListener(listener)
        player = exo
        onDispose {
            exo.removeListener(listener)
            exo.release()
            if (player === exo) player = null
        }
    }

    LaunchedEffect(muted, player) {
        player?.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(player) {
        val exo = player ?: return@LaunchedEffect
        while (isActive) {
            if (!scrubbing) {
                positionMs = exo.currentPosition.coerceAtLeast(0L)
                durationMs = exo.duration.coerceAtLeast(0L)
                playing = exo.isPlaying
            }
            delay(250)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.stableKey) {
                detectTapGestures(onTap = { onToggleChrome() })
            }
            .pointerInput(item.stableKey, disableSwipeDelete) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalPan = Offset.Zero
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } == 1) {
                            totalPan += event.calculatePan()
                        }
                    } while (event.changes.any { it.pressed })
                    val absX = abs(totalPan.x)
                    val absY = abs(totalPan.y)
                    when {
                        absX > 72f && absX > absY * 1.15f ->
                            onNavigate(if (totalPan.x < 0) 1 else -1)
                        absY > 100f && absY > absX && totalPan.y < 0 && !disableSwipeDelete ->
                            onSwipeUpDelete()
                    }
                }
            },
    ) {
        val currentPlayer = player
        if (currentPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = currentPlayer
                    }
                },
                update = { view ->
                    if (view.player !== currentPlayer) view.player = currentPlayer
                    view.useController = false
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // M3 tonal media control bar — overlay only
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val scheme = MaterialTheme.colorScheme
            Surface(
                color = scheme.surfaceContainerHighest.copy(alpha = 0.92f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    val progress = if (durationMs > 0) {
                        (if (scrubbing) scrubValue else positionMs.toFloat() / durationMs.toFloat())
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Slider(
                        value = progress,
                        onValueChange = {
                            scrubbing = true
                            scrubValue = it
                            onSeekInteracted()
                        },
                        onValueChangeFinished = {
                            val exo = player
                            if (exo != null && durationMs > 0) {
                                exo.seekTo((scrubValue * durationMs).toLong())
                            }
                            scrubbing = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = scheme.primary,
                            activeTrackColor = scheme.primary,
                            inactiveTrackColor = scheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${formatMs(if (scrubbing) (scrubValue * durationMs).toLong() else positionMs)} / ${formatMs(durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                onTogglePlayLocal()
                                val exo = player ?: return@IconButton
                                if (exo.isPlaying) exo.pause() else exo.play()
                                playing = exo.isPlaying
                            }) {
                                Icon(
                                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = scheme.onSurface,
                                )
                            }
                            IconButton(onClick = {
                                onSeekInteracted()
                                onToggleMute()
                            }) {
                                Icon(
                                    if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (muted) "Unmute" else "Mute",
                                    tint = scheme.onSurface,
                                )
                            }
                            IconButton(onClick = onToggleChrome) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = "Hide controls",
                                    tint = scheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val m = TimeUnit.MILLISECONDS.toMinutes(ms)
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(m, s)
}

private fun speedLabel(speedIndex: Int, customMs: Long): String = when {
    speedIndex == SlideshowSpeeds.OFF_INDEX -> "Off"
    speedIndex == SlideshowSpeeds.CUSTOM_INDEX -> "${customMs / 1000}s"
    else -> SlideshowSpeeds.speeds.getOrNull(speedIndex)?.label ?: "5s"
}

private fun formatDate(takenMs: Long, addedMs: Long): String {
    val ms = if (takenMs > 0) takenMs else addedMs
    if (ms <= 0) return ""
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))
}
