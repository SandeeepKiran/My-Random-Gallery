package com.mousy.myrandomgallery.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mousy.myrandomgallery.data.model.AppTab
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MediaType
import com.mousy.myrandomgallery.ui.components.CustomSpeedDialog
import com.mousy.myrandomgallery.ui.components.DeleteConfirmDialog
import com.mousy.myrandomgallery.ui.components.DetailsDialog
import com.mousy.myrandomgallery.ui.components.FullscreenViewer
import com.mousy.myrandomgallery.ui.components.GallerySnackbarHost
import com.mousy.myrandomgallery.ui.components.HiddenFoldersDialog
import com.mousy.myrandomgallery.ui.components.ResetSettingsConfirmDialog
import com.mousy.myrandomgallery.ui.components.SelectionBar
import com.mousy.myrandomgallery.ui.components.VideoPickerDialog
import com.mousy.myrandomgallery.ui.navigation.MainScaffold
import com.mousy.myrandomgallery.ui.screens.AlbumsScreen
import com.mousy.myrandomgallery.ui.screens.FavouritesScreen
import com.mousy.myrandomgallery.ui.screens.GalleryScreen
import com.mousy.myrandomgallery.ui.screens.MultiVideoScreen
import com.mousy.myrandomgallery.ui.screens.RecentScreen
import com.mousy.myrandomgallery.ui.screens.SettingsScreen
import com.mousy.myrandomgallery.util.GalleryHaptics
import com.mousy.myrandomgallery.util.LogCapture
import com.mousy.myrandomgallery.viewmodel.GalleryViewModel

@Composable
fun GalleryApp(
    viewModel: GalleryViewModel = viewModel(),
    onRequestOrientation: (Int) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val view = LocalView.current
    val appVersion = remember {
        runCatching {
            val pm = context.packageManager
            val pkg = context.packageName
            val info = pm.getPackageInfo(pkg, 0)
            val name = info.versionName ?: "1.0.0"
            val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull()
                ?: runCatching {
                    @Suppress("DEPRECATION")
                    context.applicationInfo.nativeLibraryDir?.substringAfterLast('/')
                }.getOrNull()
                ?: ""
            if (abi.isNotBlank()) "v$name ($abi)" else "v$name"
        }.getOrDefault("v1.0.0")
    }

    val backEnabled = state.viewerOpen ||
        state.albumOpen != null ||
        state.selectMode ||
        state.detailsOpen ||
        state.customSpeedOpen ||
        state.confirmDeleteKeys != null ||
        state.confirmResetSettings ||
        state.hiddenFoldersDialog ||
        state.multiVideo.pickerIndex != null ||
        state.viewerMenuOpen ||
        state.speedMenuOpen

    BackHandler(enabled = backEnabled) {
        viewModel.handleSystemBack()
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        viewModel.addSafTreeUri(uri.toString())
    }

    val favFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        viewModel.setCopyFavFolder(uri.toString(), uri.lastPathSegment ?: uri.toString())
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.importSettingsOrFavourites(uri)
    }

    var pendingPickerIndex = remember { intArrayOf(-1) }

    val multiPickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        val idx = pendingPickerIndex[0]
        if (uri == null || idx < 0) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri).orEmpty()
        val name = uri.lastPathSegment
        viewModel.assignMultiVideoUri(idx, uri.toString(), name, mime.startsWith("audio/"))
        pendingPickerIndex[0] = -1
    }

    val multiPickFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val idx = pendingPickerIndex[0]
        if (uri == null || idx < 0) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val mime = context.contentResolver.getType(uri).orEmpty()
        val name = uri.lastPathSegment
        viewModel.assignMultiVideoUri(idx, uri.toString(), name, mime.startsWith("audio/"))
        pendingPickerIndex[0] = -1
    }

    // Multi-Video landscape lock wins; otherwise allow sensor rotation while viewing a video.
    LaunchedEffect(
        state.multiVideo.landscape,
        state.currentTab,
        state.viewerOpen,
        state.viewerItem?.mediaType,
    ) {
        val orientation = when {
            state.currentTab == AppTab.MULTIVIDEO && state.multiVideo.landscape ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            state.viewerOpen && state.viewerItem?.mediaType == MediaType.VIDEO ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onRequestOrientation(orientation)
    }

    fun handleItemClick(item: MediaItem, list: List<MediaItem>) {
        if (state.selectMode) {
            viewModel.toggleSelect(item.stableKey)
            return
        }
        viewModel.openViewer(list.map { it.stableKey }, list.indexOf(item), slideshowMode = false)
    }

    val hideBottomBar = (state.viewerOpen && !state.viewerChrome) ||
        (state.currentTab == AppTab.MULTIVIDEO && state.multiVideo.landscape)

    MainScaffold(
        currentTab = state.currentTab,
        visibleTabs = state.visibleTabs,
        viewerOpen = state.viewerOpen,
        viewerSlideshowMode = state.viewerSlideshowMode,
        selectMode = state.selectMode,
        bottomBarVisible = !hideBottomBar,
        selectionBar = {
            SelectionBar(
                count = state.selectedKeys.size,
                deleteEnabled = !state.settings.deletesDisabled,
                onExit = viewModel::exitSelectMode,
                onFavourite = {
                    GalleryHaptics.confirm(view, state.settings.hapticsEnabled)
                    viewModel.favouriteSelected()
                },
                onDelete = {
                    if (state.settings.deletesDisabled) {
                        viewModel.deleteSelected()
                    } else {
                        viewModel.deleteSelected()
                    }
                },
            )
        },
        snackbarHost = {
            GallerySnackbarHost(
                snackbarHostState = snackbarHostState,
                message = state.snack,
                onDismiss = viewModel::dismissSnack,
                onAction = viewModel::runSnackAction,
            )
        },
        onTabSelected = viewModel::selectTab,
    ) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.currentTab,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(
                            initialScale = 0.98f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        )) togetherWith
                        (fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                            scaleOut(
                                targetScale = 0.98f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            ))
                },
                label = "mainTab",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                when (tab) {
                AppTab.GALLERY -> GalleryScreen(
                    items = state.gallery,
                    columns = state.settings.columns,
                    gridMode = state.settings.gridMode,
                    noFolders = state.noFolders,
                    favouriteKeys = state.settings.favIds,
                    selectedKeys = state.selectedKeys,
                    thumbnailPadding = state.settings.thumbnailPadding,
                    hapticsEnabled = state.settings.hapticsEnabled,
                    onToggleGridMode = viewModel::toggleGridMode,
                    onCycleColumns = viewModel::cycleColumns,
                    onShuffle = { viewModel.shuffleGrid() },
                    onItemClick = { handleItemClick(it, state.gallery) },
                    onItemDoubleTap = {
                        GalleryHaptics.confirm(view, state.settings.hapticsEnabled)
                        viewModel.toggleFavourite(it.stableKey)
                    },
                    onItemLongPress = { viewModel.enterSelectMode(it.stableKey) },
                    onSwipeShuffle = viewModel::onGridSwipe,
                    onPinchColumns = viewModel::adjustColumnsFromPinch,
                    onGoSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                )
                AppTab.FAV -> FavouritesScreen(
                    items = state.favourites,
                    columns = state.settings.columns,
                    noFolders = state.noFolders,
                    favouriteKeys = state.settings.favIds + state.favourites.map { it.stableKey }.toSet(),
                    selectedKeys = state.selectedKeys,
                    favTypes = state.settings.favTypes,
                    favWindow = state.settings.favWindow,
                    favTypeMenuOpen = state.favTypeMenuOpen,
                    thumbnailPadding = state.settings.thumbnailPadding,
                    onToggleFavTypeMenu = viewModel::toggleFavTypeMenu,
                    onToggleFavType = viewModel::toggleFavType,
                    onSelectFavWindow = viewModel::setFavWindow,
                    onItemClick = { handleItemClick(it, state.favourites) },
                    onItemDoubleTap = {
                        GalleryHaptics.confirm(view, state.settings.hapticsEnabled)
                        viewModel.toggleFavourite(it.stableKey)
                    },
                    onItemLongPress = { viewModel.enterSelectMode(it.stableKey) },
                    onPinchColumns = viewModel::adjustColumnsFromPinch,
                    onGoSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                )
                AppTab.RECENT -> RecentScreen(
                    items = state.recent,
                    columns = state.settings.columns,
                    noFolders = state.noFolders,
                    favouriteKeys = state.settings.favIds,
                    selectedKeys = state.selectedKeys,
                    listTypes = state.settings.recentTypes,
                    listWindow = state.settings.recentWindow,
                    typeMenuOpen = state.recentTypeMenuOpen,
                    thumbnailPadding = state.settings.thumbnailPadding,
                    onToggleTypeMenu = viewModel::toggleRecentTypeMenu,
                    onToggleType = viewModel::toggleRecentType,
                    onSelectWindow = viewModel::setRecentWindow,
                    onItemClick = { handleItemClick(it, state.recent) },
                    onItemDoubleTap = {
                        GalleryHaptics.confirm(view, state.settings.hapticsEnabled)
                        viewModel.toggleFavourite(it.stableKey)
                    },
                    onItemLongPress = { viewModel.enterSelectMode(it.stableKey) },
                    onPinchColumns = viewModel::adjustColumnsFromPinch,
                    onGoSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                )
                AppTab.MULTIVIDEO -> MultiVideoScreen(
                    state = state.multiVideo,
                    videos = state.videos,
                    noFolders = state.noFolders,
                    onToggleLandscape = viewModel::toggleMultiVideoLandscape,
                    onExitLandscape = viewModel::exitMultiVideoLandscape,
                    onSetCount = viewModel::setMultiVideoCount,
                    onPlayAll = viewModel::multiVideoPlayAll,
                    onPauseAll = viewModel::multiVideoPauseAll,
                    onMuteAll = viewModel::multiVideoMuteAll,
                    onCellTap = viewModel::onMultiVideoCellTap,
                    onTogglePlay = viewModel::toggleMultiVideoCellPlay,
                    onToggleMute = viewModel::toggleMultiVideoCellMute,
                    onChooseVideo = viewModel::openMultiVideoPicker,
                    onProgress = viewModel::updateMultiVideoProgress,
                    onGoSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                )
                AppTab.ALBUM -> AlbumsScreen(
                    albums = state.albums,
                    albumOpen = state.albumOpen,
                    albumItems = state.albumDetail,
                    columns = state.settings.columns,
                    noFolders = state.noFolders,
                    favouriteKeys = state.settings.favIds,
                    selectedKeys = state.selectedKeys,
                    thumbnailPadding = state.settings.thumbnailPadding,
                    onOpenAlbum = viewModel::openAlbum,
                    onCloseAlbum = viewModel::closeAlbum,
                    onShuffle = { viewModel.shuffleGrid() },
                    onItemClick = { handleItemClick(it, state.albumDetail) },
                    onItemDoubleTap = { viewModel.toggleFavourite(it.stableKey) },
                    onItemLongPress = { viewModel.enterSelectMode(it.stableKey) },
                    onPinchColumns = viewModel::adjustColumnsFromPinch,
                    onGoSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                )
                AppTab.SETTINGS -> SettingsScreen(
                    settings = state.settings,
                    discoveredFolders = state.discoveredFolders,
                    collapsedGroups = state.collapsedGroups,
                    appVersion = appVersion,
                    onToggleDark = viewModel::toggleTheme,
                    onToggleAmoled = viewModel::toggleAmoled,
                    onSetAccent = viewModel::setAccent,
                    onMoveTab = viewModel::moveTab,
                    onToggleTabVisibility = viewModel::toggleTabVisibility,
                    onToggleFolder = viewModel::toggleFolder,
                    onToggleGroup = viewModel::toggleGroupCollapsed,
                    onToggleFileType = viewModel::toggleFileType,
                    onToggleBehaviour = viewModel::toggleBehaviour,
                    onToggleCopyFavs = viewModel::toggleCopyFavs,
                    onChooseFavFolder = { favFolderLauncher.launch(null) },
                    onOpenHiddenFolders = viewModel::openHiddenFoldersDialog,
                    onExportSettings = {
                        viewModel.exportSettings { json ->
                            viewModel.showSnack("Settings exported (${json.length} bytes)")
                        }
                    },
                    onDownloadFavs = {
                        viewModel.downloadFavouritesZip { uri ->
                            uri?.let {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Share favourites zip"))
                            }
                        }
                    },
                    onImportSettings = {
                        importLauncher.launch(
                            arrayOf(
                                "application/json",
                                "text/*",
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                            ),
                        )
                    },
                    onAddSafFolder = { safFolderLauncher.launch(null) },
                    onResetSettings = viewModel::requestResetSettings,
                    onShareLogs = {
                        LogCapture.captureToCache(context).onSuccess { file ->
                            context.startActivity(
                                Intent.createChooser(LogCapture.shareIntent(context, file), "Share log"),
                            )
                        }.onFailure {
                            viewModel.showSnack(it.message ?: "Could not capture log")
                        }
                    },
                    onOpenGithub = {
                        val url = context.getString(com.mousy.myrandomgallery.R.string.github_url)
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                            )
                        }
                    },
                    onOpenRate = {
                        val market = context.getString(com.mousy.myrandomgallery.R.string.play_store_url)
                        val web = context.getString(com.mousy.myrandomgallery.R.string.play_store_web_url)
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(market)))
                        }.onFailure {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
                            }
                        }
                    },
                )
                AppTab.SLIDESHOW -> Unit
                }
            }

            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        AnimatedVisibility(
            visible = state.viewerOpen,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                scaleOut(
                    targetScale = 0.94f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                ),
        ) {
            FullscreenViewer(
                item = state.viewerItem,
                index = state.viewerIndex,
                count = state.viewerCount,
                isPlaying = state.viewerPlaying,
                chromeVisible = state.viewerChrome,
                menuOpen = state.viewerMenuOpen,
                speedMenuOpen = state.speedMenuOpen,
                speedIndex = state.settings.speedIdx,
                customMs = state.settings.customMs,
                isFavourite = state.viewerItem?.let { viewModel.isFavouriteItem(it) } == true,
                disableSwipeDelete = state.settings.disableSwipeDelete || state.settings.deletesDisabled,
                deleteEnabled = !state.settings.deletesDisabled,
                slideshowMode = state.viewerSlideshowMode,
                muted = state.viewerMuted,
                loopVideos = !state.settings.dontLoop,
                onClose = viewModel::closeViewer,
                onToggleChrome = viewModel::toggleViewerChrome,
                onNavigate = viewModel::viewerNavigate,
                onSwipeUpDelete = viewModel::viewerSwipeUpDelete,
                onTogglePlay = viewModel::togglePlayPause,
                onToggleMenu = viewModel::toggleViewerMenu,
                onToggleSpeedMenu = viewModel::toggleSpeedMenu,
                onSpeedSelected = viewModel::setSpeedIndex,
                onToggleFavourite = {
                    GalleryHaptics.confirm(view, state.settings.hapticsEnabled)
                    state.viewerItem?.stableKey?.let(viewModel::toggleFavourite)
                },
                onShare = {
                    viewModel.shareCurrentItem()?.let { intent ->
                        context.startActivity(Intent.createChooser(intent, "Share"))
                    }
                },
                onDelete = viewModel::requestDeleteCurrent,
                onDetails = viewModel::openDetails,
                onToggleMute = viewModel::toggleViewerMute,
                onVideoEnded = viewModel::onViewerVideoEnded,
                onUserInteracted = viewModel::noteViewerInteraction,
                chromeAutoHideNonce = state.viewerChromeNonce,
                modifier = Modifier.fillMaxSize(),
            )
        }

        state.confirmDeleteKeys?.let { keys ->
            DeleteConfirmDialog(
                count = keys.size,
                onConfirm = {
                    GalleryHaptics.confirm(view, state.settings.hapticsEnabled)
                    viewModel.confirmDelete()
                },
                onDismiss = viewModel::cancelDelete,
            )
        }

        if (state.confirmResetSettings) {
            ResetSettingsConfirmDialog(
                onConfirm = viewModel::confirmResetSettings,
                onDismiss = viewModel::cancelResetSettings,
            )
        }

        if (state.detailsOpen) {
            DetailsDialog(item = state.viewerItem, onDismiss = viewModel::closeDetails)
        }

        if (state.customSpeedOpen) {
            CustomSpeedDialog(
                initialSeconds = state.customSpeedSeconds,
                onConfirm = viewModel::confirmCustomSpeed,
                onDismiss = viewModel::dismissCustomSpeed,
            )
        }

        if (state.hiddenFoldersDialog) {
            HiddenFoldersDialog(
                folders = state.settings.hiddenFolders,
                onToggle = viewModel::toggleHiddenFolder,
                onDismiss = viewModel::closeHiddenFoldersDialog,
            )
        }

        state.multiVideo.pickerIndex?.let { pickerIndex ->
            VideoPickerDialog(
                videos = state.videos,
                onSelect = { viewModel.assignMultiVideo(pickerIndex, it) },
                onPickGallery = {
                    pendingPickerIndex[0] = pickerIndex
                    multiPickGallery.launch("*/*")
                },
                onPickFiles = {
                    pendingPickerIndex[0] = pickerIndex
                    multiPickFiles.launch(arrayOf("video/*", "audio/*", "*/*"))
                },
                onDismiss = viewModel::closeMultiVideoPicker,
            )
        }
    }
}
