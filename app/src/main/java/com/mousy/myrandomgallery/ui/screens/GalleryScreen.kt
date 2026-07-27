package com.mousy.myrandomgallery.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import com.mousy.myrandomgallery.data.model.GridMode
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.ui.components.EmptyFoldersState
import com.mousy.myrandomgallery.ui.components.MediaGrid
import com.mousy.myrandomgallery.ui.components.ThumbSpec
import com.mousy.myrandomgallery.ui.components.gridThumbRequest
import java.text.NumberFormat

@Composable
fun GalleryScreen(
    items: List<MediaItem>,
    columns: Int,
    gridMode: GridMode,
    noFolders: Boolean,
    favouriteKeys: Set<String>,
    selectedKeys: Set<String>,
    onToggleGridMode: () -> Unit,
    onCycleColumns: () -> Unit,
    onShuffle: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onItemDoubleTap: (MediaItem) -> Unit,
    onItemLongPress: (MediaItem) -> Unit,
    onSwipeShuffle: (Int) -> Unit,
    onSetColumns: (Int) -> Unit,
    thumbnailPadding: Boolean = true,
    hapticsEnabled: Boolean = true,
    /** Everything matching the current filters; [items] is a random slice of it. */
    totalCount: Int = items.size,
    /** First page of the sets a swipe will land on, decoded in advance. */
    prefetch: List<MediaItem> = emptyList(),
    onReachedEnd: (Int) -> Unit = {},
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val thumbPx = remember(columns, landscape, density) {
        ThumbSpec.gridBucket(columns, landscape, density)
    }

    // Decode the next / previous random sets while this one is on screen, so a swipe lands on
    // ready thumbnails instead of an empty grid.
    LaunchedEffect(prefetch, thumbPx) {
        if (prefetch.isEmpty()) return@LaunchedEffect
        val loader = SingletonImageLoader.get(context)
        prefetch.forEach { item -> loader.enqueue(gridThumbRequest(context, item, thumbPx)) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gallery", style = MaterialTheme.typography.titleLarge)
                if (totalCount > 0) {
                    Text(
                        "${formatCount(totalCount)} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onToggleGridMode) {
                Icon(
                    if (gridMode == GridMode.SWIPE) Icons.Default.Swipe else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = "Grid mode",
                )
            }
            IconButton(onClick = onCycleColumns) {
                Icon(Icons.Default.GridView, contentDescription = "Columns")
            }
            FilledIconButton(onClick = onShuffle) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
            }
        }

        when {
            noFolders -> EmptyFoldersState(onChooseFolders = onGoSettings)
            items.isEmpty() -> Text(
                "No media of the selected file types. Adjust File Types in settings.",
                modifier = Modifier.padding(40.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> MediaGrid(
                items = items,
                columns = columns,
                gridMode = gridMode,
                favouriteKeys = favouriteKeys,
                selectedKeys = selectedKeys,
                onItemClick = onItemClick,
                onItemDoubleTap = onItemDoubleTap,
                onItemLongPress = onItemLongPress,
                onSwipeShuffle = onSwipeShuffle,
                onSetColumns = onSetColumns,
                thumbnailPadding = thumbnailPadding,
                hapticsEnabled = hapticsEnabled,
                onReachedEnd = onReachedEnd,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)
