package com.mousy.myrandomgallery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.graphics.painter.ColorPainter
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import coil3.video.preferVideoFrameEmbeddedThumbnailKey
import coil3.video.videoFrameMillis
import com.mousy.myrandomgallery.data.model.GridMode
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MediaType
import com.mousy.myrandomgallery.ui.theme.FavouriteHeart
import com.mousy.myrandomgallery.util.GalleryHaptics
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val LONG_PRESS_MS = 2_500L
private const val DOUBLE_TAP_MS = 280L
/** Higher than default so scroll/swipe is preferred over opening a photo. */
private const val SWIPE_TRIGGER_PX = 140f

@Composable
fun MediaGrid(
    items: List<MediaItem>,
    columns: Int,
    gridMode: GridMode,
    favouriteKeys: Set<String>,
    selectedKeys: Set<String>,
    onItemClick: (MediaItem) -> Unit,
    onItemDoubleTap: (MediaItem) -> Unit,
    onItemLongPress: (MediaItem) -> Unit,
    onSwipeShuffle: (Int) -> Unit,
    onPinchColumns: (Float) -> Unit,
    thumbnailPadding: Boolean = true,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Landscape: denser columns so more than ~one row is visible.
    val cols = remember(columns, landscape) {
        val base = columns.coerceIn(1, 6)
        if (landscape) (base + 2).coerceIn(3, 8) else base
    }
    val thumbPx = with(LocalDensity.current) {
        ((360.dp / cols.coerceIn(1, 8)).coerceIn(96.dp, 400.dp)).roundToPx()
    }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val swipeOffsetX = remember { Animatable(0f) }
    val swipeOffsetY = remember { Animatable(0f) }
    var dragAccumX by remember { mutableFloatStateOf(0f) }
    var dragAccumY by remember { mutableFloatStateOf(0f) }
    var dragAxis by remember { mutableIntStateOf(0) } // 0 none, 1 horizontal, 2 vertical

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Pre-toggle (padded-on) values: 4dp gap, light content pad — no extra per-cell inset.
        val hSpacing = if (thumbnailPadding) 4.dp else 0.dp
        val vSpacing = if (thumbnailPadding) 4.dp else 0.dp
        val hPad = if (thumbnailPadding) 12.dp else 0.dp
        val cellWidth = (maxWidth - hPad - hSpacing * (cols - 1).coerceAtLeast(0)) / cols
        val cellHeight = cellWidth // square cells
        val density = LocalDensity.current
        val availableHeight = maxHeight
        val boundedHeight = availableHeight.value.isFinite() && availableHeight > 0.dp

        val displayItems = if (gridMode == GridMode.SWIPE && boundedHeight) {
            val rowPitch = cellHeight + vSpacing
            val fullRows = with(density) {
                val usable = (availableHeight - if (thumbnailPadding) 8.dp else 0.dp).toPx().coerceAtLeast(0f)
                val pitch = rowPitch.toPx().coerceAtLeast(1f)
                (usable / pitch).toInt().coerceAtLeast(1)
            }
            val capacity = (fullRows * cols).coerceAtLeast(cols)
            items.take(capacity)
        } else {
            items
        }

        val gridHeight = if (gridMode == GridMode.SWIPE && boundedHeight) {
            val rows = ((displayItems.size + cols - 1) / cols).coerceAtLeast(1)
            cellHeight * rows + vSpacing * (rows - 1).coerceAtLeast(0) + if (thumbnailPadding) 8.dp else 0.dp
        } else {
            availableHeight
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (gridMode == GridMode.SWIPE) Modifier.height(availableHeight)
                    else Modifier.fillMaxSize(),
                ),
            contentAlignment = if (gridMode == GridMode.SWIPE) Alignment.Center else Alignment.TopCenter,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                userScrollEnabled = gridMode == GridMode.SCROLL,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (gridMode == GridMode.SWIPE) Modifier.height(gridHeight) else Modifier.fillMaxSize(),
                    )
                    .offset {
                        IntOffset(swipeOffsetX.value.roundToInt(), swipeOffsetY.value.roundToInt())
                    }
                    .then(
                        if (gridMode == GridMode.SWIPE) {
                            Modifier.pointerInput(gridMode, displayItems.size, hapticsEnabled) {
                                val width = size.width.toFloat().coerceAtLeast(1f)
                                val height = size.height.toFloat().coerceAtLeast(1f)
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var totalX = 0f
                                    var totalY = 0f
                                    var dragging = false
                                    dragAccumX = 0f
                                    dragAccumY = 0f
                                    dragAxis = 0
                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                        if (change != null && change.pressed) {
                                            val dx = change.position.x - change.previousPosition.x
                                            val dy = change.position.y - change.previousPosition.y
                                            totalX += dx
                                            totalY += dy
                                            if (!dragging) {
                                                val pastX = abs(totalX) > viewConfiguration.touchSlop * 1.5f
                                                val pastY = abs(totalY) > viewConfiguration.touchSlop * 1.5f
                                                when {
                                                    pastX && abs(totalX) > abs(totalY) * 1.15f -> {
                                                        dragging = true
                                                        dragAxis = 1
                                                    }
                                                    pastY && abs(totalY) > abs(totalX) * 1.15f -> {
                                                        dragging = true
                                                        dragAxis = 2
                                                    }
                                                }
                                            }
                                            if (dragging) {
                                                if (dragAxis == 1) {
                                                    dragAccumX += dx
                                                    scope.launch {
                                                        swipeOffsetX.snapTo(
                                                            (dragAccumX * 0.85f).coerceIn(-width * 0.45f, width * 0.45f),
                                                        )
                                                    }
                                                } else {
                                                    dragAccumY += dy
                                                    scope.launch {
                                                        swipeOffsetY.snapTo(
                                                            (dragAccumY * 0.85f).coerceIn(-height * 0.45f, height * 0.45f),
                                                        )
                                                    }
                                                }
                                                if (change.positionChanged()) change.consume()
                                            }
                                        }
                                    } while (event.changes.fastAny { it.pressed })

                                    if (dragging) {
                                        scope.launch {
                                            when (dragAxis) {
                                                1 -> {
                                                    val commitLeft = dragAccumX < -SWIPE_TRIGGER_PX
                                                    val commitRight = dragAccumX > SWIPE_TRIGGER_PX
                                                    when {
                                                        commitLeft -> {
                                                            swipeOffsetX.animateTo(-width, tween(220))
                                                            onSwipeShuffle(1)
                                                            GalleryHaptics.tick(view, hapticsEnabled)
                                                            swipeOffsetX.snapTo(width * 0.35f)
                                                            swipeOffsetX.animateTo(0f, spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessMediumLow,
                                                            ))
                                                        }
                                                        commitRight -> {
                                                            swipeOffsetX.animateTo(width, tween(220))
                                                            onSwipeShuffle(-1)
                                                            GalleryHaptics.tick(view, hapticsEnabled)
                                                            swipeOffsetX.snapTo(-width * 0.35f)
                                                            swipeOffsetX.animateTo(0f, spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessMediumLow,
                                                            ))
                                                        }
                                                        else -> swipeOffsetX.animateTo(0f, spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMedium,
                                                        ))
                                                    }
                                                }
                                                2 -> {
                                                    // Swipe up = forward (same as left); swipe down = back (same as right)
                                                    val commitUp = dragAccumY < -SWIPE_TRIGGER_PX
                                                    val commitDown = dragAccumY > SWIPE_TRIGGER_PX
                                                    when {
                                                        commitUp -> {
                                                            swipeOffsetY.animateTo(-height, tween(220))
                                                            onSwipeShuffle(1)
                                                            GalleryHaptics.tick(view, hapticsEnabled)
                                                            swipeOffsetY.snapTo(height * 0.35f)
                                                            swipeOffsetY.animateTo(0f, spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessMediumLow,
                                                            ))
                                                        }
                                                        commitDown -> {
                                                            swipeOffsetY.animateTo(height, tween(220))
                                                            onSwipeShuffle(-1)
                                                            GalleryHaptics.tick(view, hapticsEnabled)
                                                            swipeOffsetY.snapTo(-height * 0.35f)
                                                            swipeOffsetY.animateTo(0f, spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessMediumLow,
                                                            ))
                                                        }
                                                        else -> swipeOffsetY.animateTo(0f, spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMedium,
                                                        ))
                                                    }
                                                }
                                            }
                                            dragAccumX = 0f
                                            dragAccumY = 0f
                                            dragAxis = 0
                                        }
                                    } else {
                                        scope.launch {
                                            swipeOffsetX.snapTo(0f)
                                            swipeOffsetY.snapTo(0f)
                                        }
                                        dragAccumX = 0f
                                        dragAccumY = 0f
                                        dragAxis = 0
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    )
                    // Pinch with 2+ fingers only — does not consume single-finger scroll
                    .pointerInput(columns) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var pastSlop = false
                            var lastCentroidSize = 0f
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    val centroidSize = pressed
                                        .map { it.position }
                                        .let { pts ->
                                            val c = Offset(
                                                pts.map { it.x }.average().toFloat(),
                                                pts.map { it.y }.average().toFloat(),
                                            )
                                            pts.map { (it - c).getDistance() }.average().toFloat()
                                        }
                                    if (lastCentroidSize == 0f) {
                                        lastCentroidSize = centroidSize
                                    } else if (centroidSize > 0f && lastCentroidSize > 0f) {
                                        val zoom = centroidSize / lastCentroidSize
                                        if (abs(zoom - 1f) > 0.04f) {
                                            pastSlop = true
                                            onPinchColumns(zoom)
                                            lastCentroidSize = centroidSize
                                        }
                                    }
                                    if (pastSlop) {
                                        pressed.fastForEach {
                                            if (it.positionChanged()) it.consume()
                                        }
                                    }
                                } else {
                                    lastCentroidSize = 0f
                                }
                            } while (event.changes.fastAny { it.pressed })
                        }
                    },
                contentPadding = if (thumbnailPadding) {
                    PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                } else {
                    PaddingValues(0.dp)
                },
                horizontalArrangement = Arrangement.spacedBy(hSpacing),
                verticalArrangement = Arrangement.spacedBy(vSpacing),
            ) {
                items(displayItems, key = { it.stableKey }) { item ->
                    MediaGridCell(
                        item = item,
                        thumbPx = thumbPx,
                        isFavourite = favouriteKeys.contains(item.stableKey),
                        isSelected = selectedKeys.contains(item.stableKey),
                        rounded = thumbnailPadding,
                        onClick = { onItemClick(item) },
                        onDoubleTap = { onItemDoubleTap(item) },
                        onLongPress = { onItemLongPress(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaGridCell(
    item: MediaItem,
    thumbPx: Int,
    isFavourite: Boolean,
    isSelected: Boolean,
    rounded: Boolean = true,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val placeholder = remember(placeholderColor) { ColorPainter(placeholderColor) }
    val request = remember(item.uri, item.stableKey, thumbPx, item.mediaType) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(Size(thumbPx, thumbPx))
            .memoryCacheKey("${item.stableKey}_thumb_$thumbPx")
            .diskCacheKey("${item.stableKey}_thumb_$thumbPx")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .apply {
                if (item.mediaType == MediaType.VIDEO) {
                    videoFrameMillis(0)
                    preferVideoFrameEmbeddedThumbnailKey(true)
                }
            }
            .build()
    }
    // Pre-toggle look: 6dp corners, no extra per-cell padding (gap comes from grid spacing).
    val shape = if (rounded) RoundedCornerShape(6.dp) else RoundedCornerShape(0.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier,
            )
            .pointerInput(item.stableKey, touchSlop) {
                val slop = touchSlop * 2.5f
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var moved = false
                    // Hold ~2.5s → multi-select; movement beyond slop cancels tap
                    val up = withTimeoutOrNull(LONG_PRESS_MS) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                            val dist = (change.position - down.position).getDistance()
                            if (dist > slop) {
                                moved = true
                                return@withTimeoutOrNull null
                            }
                            if (change.changedToUp()) return@withTimeoutOrNull change
                            if (!change.pressed) return@withTimeoutOrNull change
                        }
                        @Suppress("UNREACHABLE_CODE")
                        null
                    }
                    if (up == null && !moved) {
                        onLongPress()
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    if (moved) {
                        // Scroll/swipe in progress — never open the photo
                        return@awaitEachGesture
                    }
                    val secondDown = withTimeoutOrNull(DOUBLE_TAP_MS) {
                        awaitFirstDown(requireUnconsumed = false)
                    }
                    if (secondDown != null) {
                        var secondMoved = false
                        withTimeoutOrNull(LONG_PRESS_MS) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == secondDown.id } ?: continue
                                if ((change.position - secondDown.position).getDistance() > slop) {
                                    secondMoved = true
                                    return@withTimeoutOrNull null
                                }
                                if (change.changedToUp() || !change.pressed) return@withTimeoutOrNull change
                            }
                            @Suppress("UNREACHABLE_CODE")
                            null
                        }
                        if (!secondMoved) onDoubleTap()
                    } else {
                        onClick()
                    }
                }
            },
    ) {
        if (item.mediaType == MediaType.AUDIO) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.AudioFile,
                    contentDescription = item.displayName,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
        } else {
            AsyncImage(
                model = request,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                placeholder = placeholder,
                error = placeholder,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(22.dp),
            )
        }
        if (isFavourite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = FavouriteHeart,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(19.dp),
            )
        }
        when (item.mediaType) {
            MediaType.VIDEO -> BadgeIcon(Icons.Default.PlayCircle, Modifier.align(Alignment.BottomEnd))
            MediaType.GIF -> BadgeIcon(Icons.Default.Gif, Modifier.align(Alignment.BottomEnd))
            MediaType.AUDIO -> BadgeIcon(Icons.Default.AudioFile, Modifier.align(Alignment.BottomEnd))
            else -> Unit
        }
    }
}

@Composable
private fun BadgeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(5.dp),
        color = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(2.dp)
                .size(19.dp),
        )
    }
}
