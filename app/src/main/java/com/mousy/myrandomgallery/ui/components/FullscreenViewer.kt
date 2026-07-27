package com.mousy.myrandomgallery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MediaType
import com.mousy.myrandomgallery.data.model.SlideshowSpeeds
import com.mousy.myrandomgallery.ui.theme.FavouriteHeart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val MenuShape = RoundedCornerShape(20.dp)

/** DEVICE-ONLY: Fullscreen viewer with ExoPlayer for video and swipe / pinch-zoom for images. */
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
    modifier: Modifier = Modifier,
) {
    var navDirection by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AnimatedContent(
            targetState = item,
            transitionSpec = {
                val dir = navDirection
                if (dir == 0) {
                    fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                } else {
                    val enter = slideInHorizontally(
                        animationSpec = tween(280),
                        initialOffsetX = { full -> if (dir > 0) full else -full },
                    ) + fadeIn(tween(200))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(240),
                        targetOffsetX = { full -> if (dir > 0) -full / 3 else full / 3 },
                    ) + fadeOut(tween(180))
                    enter togetherWith exit
                }
            },
            label = "viewerPage",
            modifier = Modifier.fillMaxSize(),
        ) { pageItem ->
            when (pageItem?.mediaType) {
                MediaType.VIDEO, MediaType.AUDIO -> MediaPlayerSurface(
                    item = pageItem,
                    chromeVisible = chromeVisible,
                    onToggleChrome = onToggleChrome,
                    onNavigate = { delta ->
                        navDirection = delta
                        onNavigate(delta)
                    },
                    onSwipeUpDelete = onSwipeUpDelete,
                    disableSwipeDelete = disableSwipeDelete,
                )
                null -> Unit
                else -> ZoomableImageSurface(
                    item = pageItem,
                    onToggleChrome = onToggleChrome,
                    onNavigate = { delta ->
                        navDirection = delta
                        onNavigate(delta)
                    },
                    onSwipeUpDelete = onSwipeUpDelete,
                    disableSwipeDelete = disableSwipeDelete,
                )
            }
        }

        if (chromeVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.6f), Color.Transparent)))
                    .padding(horizontal = 4.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter),
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
                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = Color.White,
                        )
                    }
                    // Speed control: icon + label outside IconButton so "Off" isn't clipped
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .widthIn(min = 56.dp)
                            .clickable(onClick = onToggleSpeedMenu)
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
                IconButton(onClick = onToggleFavourite) {
                    Icon(
                        Icons.Default.Favorite,
                        null,
                        tint = if (isFavourite) FavouriteHeart else Color.White.copy(alpha = 0.85f),
                    )
                }
                IconButton(onClick = onToggleMenu) {
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

        if (slideshowMode) Box(modifier = Modifier.align(Alignment.Center)) {
            DropdownMenu(
                expanded = speedMenuOpen,
                onDismissRequest = onToggleSpeedMenu,
                shape = MenuShape,
            ) {
                SlideshowSpeeds.speeds.forEachIndexed { i, speed ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (i == SlideshowSpeeds.CUSTOM_INDEX) {
                                    "Custom…"
                                } else if (i == SlideshowSpeeds.OFF_INDEX) {
                                    "Off"
                                } else {
                                    speed.label
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

/**
 * Pinch-to-zoom / pan for photos & GIFs.
 * When scale == 1, horizontal swipe navigates next/prev; vertical swipe-up can delete.
 */
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

    val imageModel = remember(item.uri, item.stableKey) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(Size.ORIGINAL)
            .memoryCacheKey(item.stableKey)
            .diskCacheKey(item.stableKey)
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

/** Video/audio playback is independent from slideshow state and begins when opened. */
@Composable
private fun MediaPlayerSurface(
    item: MediaItem,
    chromeVisible: Boolean,
    onToggleChrome: () -> Unit,
    onNavigate: (Int) -> Unit,
    onSwipeUpDelete: () -> Unit,
    disableSwipeDelete: Boolean,
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(item.uri, item.stableKey) {
        val exo = ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            prepare()
            playWhenReady = true
        }
        player = exo
        onDispose {
            exo.release()
            if (player === exo) player = null
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
                        useController = chromeVisible
                        controllerShowTimeoutMs = 0
                        this.player = currentPlayer
                    }
                },
                update = { view ->
                    if (view.player !== currentPlayer) {
                        view.player = currentPlayer
                    }
                    view.useController = chromeVisible
                    view.controllerShowTimeoutMs = 0
                    if (chromeVisible) view.showController() else view.hideController()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
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
