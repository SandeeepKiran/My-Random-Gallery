package com.mousy.myrandomgallery.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.mousy.myrandomgallery.data.model.GridMode
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MediaType
import com.mousy.myrandomgallery.ui.theme.FavouriteHeart
import kotlin.math.abs

private const val LONG_PRESS_MS = 2_500L
private const val DOUBLE_TAP_MS = 280L

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
    modifier: Modifier = Modifier,
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val thumbPx = with(LocalDensity.current) {
        // Bound thumbnail decode size to cell roughly (~120–400dp depending on columns)
        ((360.dp / columns.coerceIn(1, 6)).coerceIn(120.dp, 400.dp)).roundToPx()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns.coerceIn(1, 6)),
        userScrollEnabled = gridMode == GridMode.SCROLL,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (gridMode == GridMode.SWIPE) {
                    Modifier.pointerInput(gridMode) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragAccum > 80f) onSwipeShuffle(-1)
                                else if (dragAccum < -80f) onSwipeShuffle(1)
                                dragAccum = 0f
                            },
                            onHorizontalDrag = { _, delta -> dragAccum += delta },
                        )
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
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items, key = { it.stableKey }) { item ->
            MediaGridCell(
                item = item,
                thumbPx = thumbPx,
                isFavourite = favouriteKeys.contains(item.stableKey),
                isSelected = selectedKeys.contains(item.stableKey),
                onClick = { onItemClick(item) },
                onDoubleTap = { onItemDoubleTap(item) },
                onLongPress = { onItemLongPress(item) },
            )
        }
    }
}

@Composable
private fun MediaGridCell(
    item: MediaItem,
    thumbPx: Int,
    isFavourite: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val request = remember(item.uri, thumbPx) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(Size(thumbPx, thumbPx))
            .build()
    }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier,
            )
            .pointerInput(item.stableKey) {
                awaitEachGesture {
                    awaitFirstDown()
                    // Hold ~2.5s → multi-select; shorter press → tap / double-tap
                    val up = withTimeoutOrNull(LONG_PRESS_MS) {
                        waitForUpOrCancellation()
                    }
                    if (up == null) {
                        onLongPress()
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    val secondDown = withTimeoutOrNull(DOUBLE_TAP_MS) {
                        awaitFirstDown(requireUnconsumed = false)
                    }
                    if (secondDown != null) {
                        waitForUpOrCancellation()
                        onDoubleTap()
                    } else {
                        onClick()
                    }
                }
            },
    ) {
        AsyncImage(
            model = request,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
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
