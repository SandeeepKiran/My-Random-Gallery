package com.mousy.myrandomgallery.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mousy.myrandomgallery.data.model.GridMode
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.ui.components.EmptyFoldersState
import com.mousy.myrandomgallery.ui.components.MediaGrid

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
    onPinchColumns: (Float) -> Unit,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Gallery",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
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
            else -> {
                MediaGrid(
                    items = items,
                    columns = columns,
                    gridMode = gridMode,
                    favouriteKeys = favouriteKeys,
                    selectedKeys = selectedKeys,
                    onItemClick = onItemClick,
                    onItemDoubleTap = onItemDoubleTap,
                    onItemLongPress = onItemLongPress,
                    onSwipeShuffle = onSwipeShuffle,
                    onPinchColumns = onPinchColumns,
                )
                Text(
                    text = if (gridMode == GridMode.SWIPE) {
                        "Swipe left / right for a new random set · pinch to change columns"
                    } else {
                        "Scroll for more · pinch to change columns"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }
    }
}
