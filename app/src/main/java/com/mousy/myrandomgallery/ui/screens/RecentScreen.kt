package com.mousy.myrandomgallery.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.ui.components.EmptyFoldersState
import com.mousy.myrandomgallery.ui.components.MediaGrid

@Composable
fun RecentScreen(
    items: List<MediaItem>,
    columns: Int,
    recentWindowDays: Int,
    noFolders: Boolean,
    favouriteKeys: Set<String>,
    selectedKeys: Set<String>,
    onCycleWindow: () -> Unit,
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
            Text("Recent", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onCycleWindow) {
                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                Text("$recentWindowDays Days", modifier = Modifier.padding(start = 4.dp))
            }
        }

        when {
            noFolders -> EmptyFoldersState(onChooseFolders = onGoSettings)
            items.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp, start = 40.dp, end = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 2.dp) {
                    Icon(
                        Icons.Default.HistoryToggleOff,
                        null,
                        modifier = Modifier
                            .padding(24.dp)
                            .size(52.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    "No recently added files found in the selected paths",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
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
