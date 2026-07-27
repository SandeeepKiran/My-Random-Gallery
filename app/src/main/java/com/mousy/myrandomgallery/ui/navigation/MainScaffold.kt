package com.mousy.myrandomgallery.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mousy.myrandomgallery.data.model.AppTab

/**
 * Bottom nav is drawn as an overlay so immersive chrome hide/show does not
 * re-pad / re-center the fullscreen viewer content.
 * Edge-to-edge: content draws under system bars; status bar inset applied when
 * not in the immersive viewer; nav bar inset stays on the overlay bar.
 */
@Composable
fun MainScaffold(
    currentTab: AppTab,
    visibleTabs: List<AppTab>,
    viewerOpen: Boolean,
    viewerSlideshowMode: Boolean = false,
    selectMode: Boolean,
    snackbarHost: @Composable () -> Unit,
    selectionBar: @Composable () -> Unit,
    bottomBarVisible: Boolean = true,
    onTabSelected: (AppTab) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        snackbarHost = snackbarHost,
        // Content is full-bleed; bottom bar overlays so media never jumps.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Keep a stable bottom inset for non-viewer tabs so grids aren't under the bar.
            val contentBottomPad = if (viewerOpen || !bottomBarVisible) 0.dp else 56.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentBottomPad)
                    .then(if (viewerOpen) Modifier else Modifier.statusBarsPadding()),
            ) {
                content()
            }

            if (selectMode) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                ) {
                    selectionBar()
                }
            }

            AnimatedVisibility(
                visible = bottomBarVisible,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it },
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                    slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            ) {
                NavigationBar(
                    modifier = Modifier.height(56.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets,
                ) {
                    visibleTabs.forEach { tab ->
                        // View mode must not highlight Slideshow; only true slideshow mode does.
                        val selected = when {
                            viewerOpen && viewerSlideshowMode -> tab == AppTab.SLIDESHOW
                            viewerOpen -> tab == currentTab
                            else -> currentTab == tab
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onTabSelected(tab) },
                            icon = {
                                Box(
                                    modifier = Modifier.size(width = 56.dp, height = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {}
                                    }
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(22.dp),
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
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        }
    }
}
