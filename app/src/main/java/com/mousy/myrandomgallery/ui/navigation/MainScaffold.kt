package com.mousy.myrandomgallery.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mousy.myrandomgallery.data.model.AppTab

/**
 * Adaptive navigation suite on phones still reads as a bottom bar.
 *
 * Immersive viewer: suite layout type is [NavigationSuiteType.None] so media stays
 * full-bleed (no re-pad / re-center). When chrome is visible, a compact NavigationBar
 * overlays using real [WindowInsets.navigationBars] — intentional overlay, not Scaffold padding.
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
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val calculatedType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    // Viewer always uses None so immersive media never jumps with suite show/hide.
    val layoutType = when {
        viewerOpen -> NavigationSuiteType.None
        !bottomBarVisible -> NavigationSuiteType.None
        else -> calculatedType
    }

    Scaffold(
        snackbarHost = snackbarHost,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavigationSuiteScaffold(
                layoutType = layoutType,
                navigationSuiteItems = {
                    if (!viewerOpen && bottomBarVisible) {
                        visibleTabs.forEach { tab ->
                            val selected = currentTab == tab
                            item(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = {
                                    Icon(tab.icon, contentDescription = tab.label)
                                },
                                label = {
                                    Text(
                                        if (layoutType == NavigationSuiteType.NavigationBar) {
                                            tab.shortLabel
                                        } else {
                                            tab.label
                                        },
                                    )
                                },
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (viewerOpen) Modifier else Modifier.statusBarsPadding()),
                ) {
                    content()
                }
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

            // Immersive overlay bar: same selection rules as before, real nav-bar insets.
            AnimatedVisibility(
                visible = viewerOpen && bottomBarVisible,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it },
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                    slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    visibleTabs.forEach { tab ->
                        val selected = when {
                            viewerSlideshowMode -> tab == AppTab.SLIDESHOW
                            else -> tab == currentTab
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onTabSelected(tab) },
                            icon = {
                                Icon(tab.icon, contentDescription = tab.label)
                            },
                            label = { Text(tab.shortLabel) },
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        }
    }
}
