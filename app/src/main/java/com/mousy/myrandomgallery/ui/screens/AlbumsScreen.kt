package com.mousy.myrandomgallery.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mousy.myrandomgallery.data.media.MediaRepository
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.ui.components.EmptyFoldersState
import com.mousy.myrandomgallery.ui.components.MediaGrid

@Composable
fun AlbumsScreen(
    albums: List<MediaRepository.FolderInfo>,
    albumOpen: String?,
    albumItems: List<MediaItem>,
    columns: Int,
    noFolders: Boolean,
    favouriteKeys: Set<String>,
    selectedKeys: Set<String>,
    onOpenAlbum: (String) -> Unit,
    onCloseAlbum: () -> Unit,
    onShuffle: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onItemDoubleTap: (MediaItem) -> Unit,
    onItemLongPress: (MediaItem) -> Unit,
    onPinchColumns: (Float) -> Unit,
    onGoSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (albumOpen != null) {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCloseAlbum) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(albumOpen.substringAfterLast('/'), style = MaterialTheme.typography.titleLarge)
                    Text("${albumItems.size} items", style = MaterialTheme.typography.labelMedium)
                }
                FilledIconButton(onClick = onShuffle) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                }
            }
            if (albumItems.isEmpty()) {
                Text(
                    "This album has no media in the selected file types.",
                    modifier = Modifier.padding(40.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MediaGrid(
                    items = albumItems,
                    columns = columns,
                    gridMode = com.mousy.myrandomgallery.data.model.GridMode.SCROLL,
                    favouriteKeys = favouriteKeys,
                    selectedKeys = selectedKeys,
                    onItemClick = onItemClick,
                    onItemDoubleTap = onItemDoubleTap,
                    onItemLongPress = onItemLongPress,
                    onSwipeShuffle = {},
                    onPinchColumns = onPinchColumns,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Albums", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 14.dp))
            if (noFolders) {
                EmptyFoldersState(onChooseFolders = onGoSettings)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(albums, key = { it.path }) { album ->
                        Column(
                            modifier = Modifier.clickable { onOpenAlbum(album.path) },
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                shape = MaterialTheme.shapes.large,
                                tonalElevation = 2.dp,
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    null,
                                    modifier = Modifier.padding(32.dp),
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Text(album.displayName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                            Text(
                                album.path,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("${album.mediaCount}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
