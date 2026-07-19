package com.mousy.myrandomgallery.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mousy.myrandomgallery.data.model.FavWindow
import com.mousy.myrandomgallery.data.model.FileTypeFilter
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.ui.components.EmptyFoldersState
import com.mousy.myrandomgallery.ui.components.MediaGrid

@Composable
fun FavouritesScreen(
    items: List<MediaItem>,
    columns: Int,
    noFolders: Boolean,
    favouriteKeys: Set<String>,
    selectedKeys: Set<String>,
    favTypes: FileTypeFilter,
    favWindow: FavWindow,
    favTypeMenuOpen: Boolean,
    onToggleFavTypeMenu: () -> Unit,
    onToggleFavType: (String) -> Unit,
    onCycleFavWindow: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onItemDoubleTap: (MediaItem) -> Unit,
    onItemLongPress: (MediaItem) -> Unit,
    onPinchColumns: (Float) -> Unit,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Favourites", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = onToggleFavTypeMenu) {
                    Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp))
                    Text(favTypeLabel(favTypes), modifier = Modifier.padding(start = 4.dp))
                }
                DropdownMenu(expanded = favTypeMenuOpen, onDismissRequest = onToggleFavTypeMenu) {
                    DropdownMenuItem(text = { Text("Photos") }, onClick = { onToggleFavType("photo") })
                    DropdownMenuItem(text = { Text("Videos") }, onClick = { onToggleFavType("video") })
                    DropdownMenuItem(text = { Text("GIFs") }, onClick = { onToggleFavType("gif") })
                }
            }
            OutlinedButton(onClick = onCycleFavWindow, modifier = Modifier.padding(start = 6.dp)) {
                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                Text(favWindow.label(), modifier = Modifier.padding(start = 4.dp))
            }
        }

        when {
            noFolders -> EmptyFoldersState(onChooseFolders = onGoSettings)
            items.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 2.dp) {
                    Icon(
                        Icons.Default.Favorite,
                        null,
                        modifier = Modifier
                            .padding(24.dp)
                            .size(52.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
                Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                Text(
                    "Double-tap a photo in the gallery to add it, or loosen the filters above.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            else -> MediaGrid(
                items = items,
                columns = columns,
                gridMode = com.mousy.myrandomgallery.data.model.GridMode.SCROLL,
                favouriteKeys = favouriteKeys,
                selectedKeys = selectedKeys,
                onItemClick = onItemClick,
                onItemDoubleTap = onItemDoubleTap,
                onItemLongPress = onItemLongPress,
                onSwipeShuffle = {},
                onPinchColumns = onPinchColumns,
            )
        }
    }
}

private fun favTypeLabel(filter: FileTypeFilter): String {
    val count = listOf(filter.photo, filter.video, filter.gif).count { it }
    return if (count == 3) "All types" else "$count types"
}
