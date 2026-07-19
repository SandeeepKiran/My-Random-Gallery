package com.mousy.myrandomgallery.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mousy.myrandomgallery.data.model.AppTab

@Composable
fun MainScaffold(
    currentTab: AppTab,
    visibleTabs: List<AppTab>,
    viewerOpen: Boolean,
    selectMode: Boolean,
    selectionBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    bottomBarVisible: Boolean = true,
    onTabSelected: (AppTab) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        snackbarHost = snackbarHost,
        bottomBar = {
            // Bottom nav stays visible in the slideshow viewer (wireframe: no old 1/N counter).
            // Icons only — no text labels under tabs.
            if (bottomBarVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    visibleTabs.forEach { tab ->
                        val selected = if (viewerOpen) tab == AppTab.SLIDESHOW else currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onTabSelected(tab) },
                            icon = {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                ) {
                                    androidx.compose.material3.Icon(
                                        tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            },
                            label = {},
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content()
            if (selectMode) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    selectionBar()
                }
            }
        }
    }
}
