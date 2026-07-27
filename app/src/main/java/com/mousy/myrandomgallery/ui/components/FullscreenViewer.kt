package com.mousy.myrandomgallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.compose.ui.graphics.painter.ColorPainter
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Dimension
import coil3.size.Size
import coil3.video.preferVideoFrameEmbeddedThumbnailKey
import coil3.video.videoFrameMillis
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MediaType
import com.mousy.myrandomgallery.data.model.SlideshowSpeeds
import com.mousy.myrandomgallery.ui.theme.FavouriteHeart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val MenuShape = RoundedCornerShape(20.dp)
private const val CHROME_AUTO_HIDE_MS = 5_000L
/** Snappy enough to feel instant, long enough to read as a swipe. */
private const val PAGE_ANIM_MS = 150
/**
 * Decode headroom above the screen size. Full-resolution decodes of a 12MP photo were the
 * cause of the black flash on swipe; 1.5x still leaves detail for pinch-zoom.
 */
private const val VIEWER_DECODE_SCALE = 1.5f

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
    prefetch: List<MediaItem> = emptyList(),
    gridThumbBucketPx: Int = 256,
    controlsBottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var navDirection by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val decodeSize = rememberViewerDecodeSize()

    // Auto-hide chrome after inactivity while visible
    LaunchedEffect(chromeVisible, item?.stableKey, menuOpen, speedMenuOpen, chromeAutoHideNonce) {
        if (!chromeVisible || menuOpen || speedMenuOpen) return@LaunchedEffect
        delay(CHROME_AUTO_HIDE_MS)
        onToggleChrome()
    }

    // Warm the neighbours so a swipe lands on an already-decoded bitmap.
    LaunchedEffect(item?.stableKey, decodeSize) {
        if (prefetch.isEmpty()) return@LaunchedEffect
        val loader = SingletonImageLoader.get(context)
        prefetch.forEach { neighbour ->
            if (neighbour.mediaType == MediaType.AUDIO) return@forEach
            loader.enqueue(neighbour.viewerRequest(context, decodeSize, gridThumbBucketPx))
        }
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
                val fade = tween<Float>(PAGE_ANIM_MS)
                if (dir == 0) {
                    fadeIn(fade) togetherWith fadeOut(fade)
                } else {
                    val enter = slideInHorizontally(
                        animationSpec = tween(PAGE_ANIM_MS),
                        initialOffsetX = { full -> if (dir > 0) full else -full },
                    ) + fadeIn(fade)
                    val exit = slideOutHorizontally(
                        animationSpec = tween(PAGE_ANIM_MS),
                        targetOffsetX = { full -> if (dir > 0) -full / 3 else full / 3 },
                    ) + fadeOut(fade)
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
                    controlsBottomPadding = controlsBottomPadding,
                )
                null -> Unit
                else -> ZoomableImageSurface(
                    item = pageItem,
                    decodeSize = decodeSize,
                    gridThumbBucketPx = gridThumbBucketPx,
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
                    // Zero while system bars are hidden; keeps chrome clear if they reappear.
                    .statusBarsPadding()
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

/** Screen-sized decode target, so the viewer never asks Coil for a full-resolution bitmap. */
@Composable
private fun rememberViewerDecodeSize(): Size {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    return remember(config.screenWidthDp, config.screenHeightDp, density) {
        with(density) {
            val w = (config.screenWidthDp.dp.toPx() * VIEWER_DECODE_SCALE).roundToInt()
            val h = (config.screenHeightDp.dp.toPx() * VIEWER_DECODE_SCALE).roundToInt()
            Size(w.coerceAtLeast(1), h.coerceAtLeast(1))
        }
    }
}

/**
 * Full-view request for an item. The grid's cached thumbnail is named as the placeholder so
 * something appears on the very first frame instead of black while the large decode runs.
 */
private fun MediaItem.viewerRequest(
    context: android.content.Context,
    decodeSize: Size,
    gridThumbBucketPx: Int,
): ImageRequest {
    val cacheKey = ThumbSpec.fullKey(stableKey, decodeSize.width.pxOrZero(), decodeSize.height.pxOrZero())
    return ImageRequest.Builder(context)
        .data(uri)
        .size(decodeSize)
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .placeholderMemoryCacheKey(MemoryCache.Key(ThumbSpec.thumbKey(stableKey, gridThumbBucketPx)))
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .apply {
            if (mediaType == MediaType.VIDEO) {
                videoFrameMillis(0)
                preferVideoFrameEmbeddedThumbnailKey(true)
            }
        }
        .build()
}

private fun Dimension.pxOrZero(): Int = (this as? Dimension.Pixels)?.px ?: 0

@Composable
private fun ZoomableImageSurface(
    item: MediaItem,
    decodeSize: Size,
    gridThumbBucketPx: Int,
    onToggleChrome: () -> Unit,
    onNavigate: (Int) -> Unit,
    onSwipeUpDelete: () -> Unit,
    disableSwipeDelete: Boolean,
) {
    var scale by remember(item.stableKey) { mutableFloatStateOf(1f) }
    var offset by remember(item.stableKey) { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current

    val placeholder = remember { ColorPainter(Color.Black) }
    val imageModel = remember(item.uri, item.stableKey, decodeSize, gridThumbBucketPx) {
        item.viewerRequest(context, decodeSize, gridThumbBucketPx)
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
 * Uses media3-ui-compose ContentFrame; chrome stays Compose overlay so layout stays stable.
 */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
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
    controlsBottomPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var playing by remember(item.stableKey) { mutableStateOf(true) }
    var positionMs by remember(item.stableKey) { mutableLongStateOf(0L) }
    var durationMs by remember(item.stableKey) { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    // Survives process death / activity recreation so playback resumes where it left off
    // rather than restarting. (Rotation itself is handled in-process via configChanges.)
    var resumePositionMs by rememberSaveable(item.stableKey) { mutableStateOf(0L) }
    var resumePlayWhenReady by rememberSaveable(item.stableKey) { mutableStateOf(true) }

    // A looping video never reaches STATE_ENDED, so in a slideshow it would play forever and
    // the show would never move on. Looping is a view-mode behaviour only.
    val repeatVideo = loopVideos && !slideshowMode

    // minSdk 30: LifecycleStartEffect (onStart/onStop) is the recommended player gate.
    LifecycleStartEffect(item.uri, item.stableKey, repeatVideo, slideshowMode) {
        val exo = ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            repeatMode = if (repeatVideo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            if (resumePositionMs > 0L) seekTo(resumePositionMs)
            prepare()
            playWhenReady = resumePlayWhenReady
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !repeatVideo) {
                    onVideoEnded()
                }
            }
        }
        exo.addListener(listener)
        player = exo
        onStopOrDispose {
            resumePositionMs = exo.currentPosition.coerceAtLeast(0L)
            resumePlayWhenReady = exo.playWhenReady
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
        if (item.mediaType == MediaType.AUDIO) {
            // Audio has no video surface to draw, so the viewer was just black.
            AudioArtwork(item = item, modifier = Modifier.fillMaxSize())
        } else {
            ContentFrame(
                player = player,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        // M3 tonal media control bar — overlay only
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                // Sit above the overlaid tab bar instead of behind it.
                .padding(bottom = controlsBottomPadding),
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

/**
 * Cover art for audio items. Reads the embedded picture with [MediaMetadataRetriever] (Coil has
 * no audio decoder) and falls back to a tinted note card when a track has no artwork.
 */
@Composable
private fun AudioArtwork(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var art by remember(item.stableKey) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.stableKey) {
        art = withContext(Dispatchers.IO) { loadEmbeddedArtwork(context, item.uri) }
    }

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    scheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    Color.Black,
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        val cover = art
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainerHighest,
                tonalElevation = 6.dp,
                modifier = Modifier.size(240.dp),
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover,
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = item.displayName,
                            tint = scheme.primary,
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }
            }
            Text(
                text = item.displayName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

private fun loadEmbeddedArtwork(context: android.content.Context, uri: android.net.Uri): ImageBitmap? =
    runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_ARTWORK_PX)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
        }
    }.getOrNull()

private const val MAX_ARTWORK_PX = 1_024

private fun sampleSizeFor(width: Int, height: Int, target: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w / 2 >= target && h / 2 >= target) {
        w /= 2
        h /= 2
        sample *= 2
    }
    return sample
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
