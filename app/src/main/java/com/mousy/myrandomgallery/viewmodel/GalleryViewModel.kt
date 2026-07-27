package com.mousy.myrandomgallery.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mousy.myrandomgallery.data.media.FavouritesExporter
import com.mousy.myrandomgallery.data.media.FavouritesFolderSync
import com.mousy.myrandomgallery.data.media.MediaRepository
import com.mousy.myrandomgallery.data.model.AccentColor
import com.mousy.myrandomgallery.data.model.AppSettings
import com.mousy.myrandomgallery.data.model.AppTab
import com.mousy.myrandomgallery.data.model.FavWindow
import com.mousy.myrandomgallery.data.model.FileTypeFilter
import com.mousy.myrandomgallery.data.model.GridMode
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MediaType
import com.mousy.myrandomgallery.data.model.MultiVideoState
import com.mousy.myrandomgallery.data.model.SlideshowSpeeds
import com.mousy.myrandomgallery.data.model.SnackMessage
import com.mousy.myrandomgallery.data.model.TabFeatures
import com.mousy.myrandomgallery.data.model.ThemeMode
import com.mousy.myrandomgallery.data.preferences.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val mediaRepo = MediaRepository(application)
    private val favExporter = FavouritesExporter(application)
    private val favSync = FavouritesFolderSync(application)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _allMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _folderFavourites = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _shuffleOrder = MutableStateFlow<List<String>>(emptyList())
    private val _deletedKeys = MutableStateFlow<Set<String>>(emptySet())
    private val _discoveredFolders = MutableStateFlow<List<MediaRepository.FolderInfo>>(emptyList())

    private val _currentTab = MutableStateFlow(AppTab.GALLERY)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _viewerOpen = MutableStateFlow(false)
    private val _viewerList = MutableStateFlow<List<String>>(emptyList())
    private val _viewerIndex = MutableStateFlow(0)
    private val _viewerPlaying = MutableStateFlow(false)
    private val _viewerChrome = MutableStateFlow(true)
    private val _viewerMenuOpen = MutableStateFlow(false)
    private val _viewerReturnTab = MutableStateFlow(AppTab.GALLERY)
    /** True only when opened from the Slideshow tab (autoplay + timing controls). */
    private val _viewerSlideshowMode = MutableStateFlow(false)
    private val _speedMenuOpen = MutableStateFlow(false)
    private val _detailsOpen = MutableStateFlow(false)
    private val _customSpeedOpen = MutableStateFlow(false)
    private val _customSpeedSeconds = MutableStateFlow(8)

    private val _selectMode = MutableStateFlow(false)
    private val _selectedKeys = MutableStateFlow<Set<String>>(emptySet())
    private val _confirmDeleteKeys = MutableStateFlow<List<String>?>(null)
    private val _confirmResetSettings = MutableStateFlow(false)
    private val _hiddenFoldersDialog = MutableStateFlow(false)
    private val _favTypeMenuOpen = MutableStateFlow(false)
    private val _recentTypeMenuOpen = MutableStateFlow(false)
    private val _collapsedGroups = MutableStateFlow<Set<String>>(emptySet())
    private val _albumOpen = MutableStateFlow<String?>(null)
    private val _multiVideo = MutableStateFlow(MultiVideoState())
    private val _snack = MutableStateFlow<SnackMessage?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _pendingUndoDeletes = MutableStateFlow<List<String>>(emptyList())
    private var pendingUndoSettings: AppSettings? = null

    private var slideshowJob: Job? = null
    private var snackJob: Job? = null
    private var mvOverlayJob: Job? = null
    private var refreshJob: Job? = null
    private var persistShuffleJob: Job? = null

    /** History of gallery shuffle orders; swipe-back restores previous sets. Cap = 40. */
    private val shuffleHistory = mutableListOf<List<String>>()
    private var historyIndex = -1
    private var lastMediaScanKey: String? = null
    private var shuffleRestored = false

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<GalleryUiState> = combine(
        _settings,
        _allMedia,
        _folderFavourites,
        _shuffleOrder,
        _deletedKeys,
        _discoveredFolders,
        _currentTab,
        _viewerOpen,
        _viewerList,
        _viewerIndex,
        _viewerPlaying,
        _viewerChrome,
        _viewerMenuOpen,
        _speedMenuOpen,
        _detailsOpen,
        _customSpeedOpen,
        _customSpeedSeconds,
        _selectMode,
        _selectedKeys,
        _confirmDeleteKeys,
        _hiddenFoldersDialog,
        _favTypeMenuOpen,
        _collapsedGroups,
        _albumOpen,
        _multiVideo,
        _snack,
        _isLoading,
        _viewerSlideshowMode,
        _confirmResetSettings,
        _recentTypeMenuOpen,
    ) { values ->
        buildUiState(
            settings = values[0] as AppSettings,
            allMedia = values[1] as List<MediaItem>,
            folderFavourites = values[2] as List<MediaItem>,
            shuffleOrder = values[3] as List<String>,
            deleted = values[4] as Set<String>,
            discovered = values[5] as List<MediaRepository.FolderInfo>,
            tab = values[6] as AppTab,
            viewerOpen = values[7] as Boolean,
            viewerList = values[8] as List<String>,
            viewerIndex = values[9] as Int,
            viewerPlaying = values[10] as Boolean,
            viewerChrome = values[11] as Boolean,
            viewerMenuOpen = values[12] as Boolean,
            speedMenuOpen = values[13] as Boolean,
            detailsOpen = values[14] as Boolean,
            customSpeedOpen = values[15] as Boolean,
            customSpeedSeconds = values[16] as Int,
            selectMode = values[17] as Boolean,
            selectedKeys = values[18] as Set<String>,
            confirmDelete = values[19] as List<String>?,
            hiddenDialog = values[20] as Boolean,
            favTypeMenu = values[21] as Boolean,
            collapsed = values[22] as Set<String>,
            albumOpen = values[23] as String?,
            multiVideo = values[24] as MultiVideoState,
            snack = values[25] as SnackMessage?,
            loading = values[26] as Boolean,
            viewerSlideshowMode = values[27] as Boolean,
            confirmReset = values[28] as Boolean,
            recentTypeMenu = values[29] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState())

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { s ->
                _settings.value = s
                if (!shuffleRestored && s.shuffleHistoryEncoded.isNotBlank()) {
                    restoreShuffleHistory(s)
                    shuffleRestored = true
                } else if (!shuffleRestored) {
                    shuffleRestored = true
                }
                val key = mediaScanKey(s)
                if (key != lastMediaScanKey) {
                    lastMediaScanKey = key
                    refreshMedia()
                }
            }
        }
        // Folder discovery once at start (and after media-relevant setting changes via refreshMedia)
        viewModelScope.launch {
            _discoveredFolders.value = mediaRepo.discoverFolders(_settings.value.hiddenFolders)
        }
    }

    fun onPermissionsGranted() = refreshMedia()

    fun refreshMedia() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(180)
            _isLoading.value = true
            val s = _settings.value
            val media = mediaRepo.scanMedia(
                selectedFolders = s.selectedFolders,
                safTreeUris = s.safTreeUris,
                hiddenFolders = s.hiddenFolders,
                fileTypeFilters = effectiveFileTypes(s),
            )
            _allMedia.value = media
            _discoveredFolders.value = mediaRepo.discoverFolders(s.hiddenFolders)
            val known = media.map { it.stableKey }.toSet()
            if (_shuffleOrder.value.isEmpty()) {
                val initial = media.map { it.stableKey }.shuffled()
                _shuffleOrder.value = initial
                resetShuffleHistory(initial)
            } else {
                val pruned = _shuffleOrder.value.filter { it in known } +
                    media.map { it.stableKey }.filter { it !in _shuffleOrder.value.toSet() }
                _shuffleOrder.value = pruned
                // Keep history entries consistent with live keys
                for (i in shuffleHistory.indices) {
                    shuffleHistory[i] = shuffleHistory[i].filter { it in known }
                }
            }
            autoConfigureFileTypes(media, s)
            // Remember extension counts for Settings (all types under selection)
            val counts = mediaRepo.discoverExtensionCounts(
                selectedFolders = s.selectedFolders,
                safTreeUris = s.safTreeUris,
                hiddenFolders = s.hiddenFolders,
            )
            if (counts != s.discoveredFileTypeCounts) {
                persistSettings { it.copy(discoveredFileTypeCounts = counts) }
            }
            refreshFolderFavourites(s)
            _isLoading.value = false
        }
    }

    /** When Favourites-folder mode is on, load media from that SAF tree for the Favourites tab. */
    private suspend fun refreshFolderFavourites(s: AppSettings = _settings.value) {
        if (s.copyFavs && s.copyFavTreeUri.isNotBlank()) {
            _folderFavourites.value = mediaRepo.scanSafTree(
                treeUri = s.copyFavTreeUri,
                fileTypeFilters = effectiveFileTypes(s),
            )
        } else {
            _folderFavourites.value = emptyList()
        }
    }

    private fun usesFavouritesFolder(s: AppSettings = _settings.value): Boolean =
        s.copyFavs && s.copyFavTreeUri.isNotBlank()

    private fun mediaScanKey(s: AppSettings): String =
        listOf(
            s.selectedFolders.sorted().joinToString(","),
            s.safTreeUris.sorted().joinToString(","),
            s.copyFavs.toString(),
            s.copyFavTreeUri,
            s.fileTypes.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" },
            s.hiddenFolders.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" },
        ).joinToString("|")

    private fun resetShuffleHistory(order: List<String>) {
        shuffleHistory.clear()
        shuffleHistory += order
        historyIndex = 0
        persistShuffleHistory()
    }

    private fun restoreShuffleHistory(s: AppSettings) {
        val pages = decodeShuffleHistory(s.shuffleHistoryEncoded)
        if (pages.isEmpty()) return
        shuffleHistory.clear()
        shuffleHistory.addAll(pages)
        historyIndex = s.shuffleHistoryIndex.coerceIn(0, shuffleHistory.lastIndex)
        _shuffleOrder.value = shuffleHistory.getOrElse(historyIndex) { pages.last() }
    }

    private fun persistShuffleHistory() {
        persistShuffleJob?.cancel()
        persistShuffleJob = viewModelScope.launch {
            delay(250)
            val encoded = encodeShuffleHistory(shuffleHistory)
            val idx = historyIndex.coerceAtLeast(0)
            persistSettings {
                it.copy(shuffleHistoryEncoded = encoded, shuffleHistoryIndex = idx)
            }
        }
    }

    private fun encodeShuffleHistory(pages: List<List<String>>): String =
        pages.takeLast(40).joinToString(";") { page -> page.joinToString("|") }

    private fun decodeShuffleHistory(raw: String): List<List<String>> {
        if (raw.isBlank()) return emptyList()
        return raw.split(';')
            .map { page -> page.split('|').filter { it.isNotBlank() } }
            .filter { it.isNotEmpty() }
            .takeLast(40)
    }

    private suspend fun autoConfigureFileTypes(media: List<MediaItem>, s: AppSettings) {
        val exts = media.map { it.extension.lowercase() }.distinct()
        if (exts.isEmpty()) return
        val updated = s.fileTypes.toMutableMap()
        var changed = false
        exts.forEach { ext ->
            if (!updated.containsKey(ext)) {
                updated[ext] = ext in SlideshowSpeeds.supportedExtensions
                changed = true
            }
        }
        if (changed) persistSettings { it.copy(fileTypes = updated) }
    }

    private fun effectiveFileTypes(s: AppSettings): Map<String, Boolean> {
        if (s.fileTypes.isNotEmpty()) return s.fileTypes
        return SlideshowSpeeds.supportedExtensions.associateWith { true }
    }

    fun selectTab(tab: AppTab) {
        slideshowJob?.cancel()
        when (tab) {
            AppTab.SLIDESHOW -> {
                val list = tabSourceList(_currentTab.value, _albumOpen.value)
                if (list.isNotEmpty()) {
                    openViewer(list, 0, autoPlay = true, slideshowMode = true)
                }
            }
            AppTab.GALLERY -> {
                _viewerOpen.value = false
                _viewerPlaying.value = false
                _selectMode.value = false
                _selectedKeys.value = emptySet()
                _albumOpen.value = null
                _currentTab.value = AppTab.GALLERY
                // Wireframe: tapping Gallery always produces a fresh random set.
                shuffleGrid(direction = 1)
            }
            else -> {
                _viewerOpen.value = false
                _viewerPlaying.value = false
                _selectMode.value = false
                _selectedKeys.value = emptySet()
                if (tab != AppTab.ALBUM) _albumOpen.value = null
                _currentTab.value = tab
                if (tab == AppTab.MULTIVIDEO) showMultiVideoOverlay()
            }
        }
    }

    fun toggleGridMode() = persistSettings {
        it.copy(gridMode = if (it.gridMode == GridMode.SWIPE) GridMode.SCROLL else GridMode.SWIPE)
    }

    fun cycleColumns() = persistSettings {
        it.copy(columns = if (it.columns >= 6) 1 else it.columns + 1)
    }

    /** DEVICE-ONLY: Pinch gesture adjusts column count 1–6. */
    fun adjustColumnsFromPinch(scale: Float) {
        if (scale > 1.08f) persistSettings { it.copy(columns = (it.columns - 1).coerceAtLeast(1)) }
        else if (scale < 0.92f) persistSettings { it.copy(columns = (it.columns + 1).coerceAtMost(6)) }
    }

    fun shuffleGrid(direction: Int = 1) {
        when {
            // Swipe back → restore previous random set (history), not a fresh shuffle
            direction < 0 && historyIndex > 0 -> {
                historyIndex--
                _shuffleOrder.value = shuffleHistory[historyIndex]
                persistShuffleHistory()
            }
            else -> {
                if (historyIndex >= 0 && historyIndex < shuffleHistory.lastIndex) {
                    while (shuffleHistory.size > historyIndex + 1) {
                        shuffleHistory.removeAt(shuffleHistory.lastIndex)
                    }
                }
                val newOrder = liveOrderedKeys().shuffled()
                shuffleHistory += newOrder
                if (shuffleHistory.size > 40) {
                    shuffleHistory.removeAt(0)
                }
                historyIndex = shuffleHistory.lastIndex
                _shuffleOrder.value = newOrder
                persistShuffleHistory()
            }
        }
    }

    fun onGridSwipe(direction: Int) {
        if (_settings.value.gridMode == GridMode.SWIPE) shuffleGrid(direction)
    }

    fun toggleFavourite(key: String) {
        viewModelScope.launch {
            val s = _settings.value
            val folderItem = _folderFavourites.value.find { it.stableKey == key }
            val libraryItem = _allMedia.value.find { it.stableKey == key }

            // Unfavourite a Favourites-folder copy: remove file + matching library favIds.
            if (folderItem != null && usesFavouritesFolder(s) && libraryItem == null) {
                favSync.syncFavouriteRemoved(s.copyFavTreeUri, folderItem.displayName)
                val name = folderItem.displayName
                persistSettings { cur ->
                    cur.copy(
                        favIds = cur.favIds.filter { id ->
                            _allMedia.value.find { it.stableKey == id }?.displayName != name
                        }.toSet(),
                    )
                }
                refreshFolderFavourites()
                return@launch
            }

            val item = libraryItem ?: folderItem ?: return@launch
            val newFavs = s.favIds.toMutableSet()
            // Library items: toggle favIds; folder-mode hearts on gallery still use favIds.
            val isFav = isFavouriteItem(item)
            if (isFav) {
                newFavs.remove(key)
                _allMedia.value.filter { it.displayName == item.displayName }
                    .forEach { newFavs.remove(it.stableKey) }
                persistSettings { it.copy(favIds = newFavs) }
                if (usesFavouritesFolder(s)) {
                    favSync.syncFavouriteRemoved(s.copyFavTreeUri, item.displayName)
                    refreshFolderFavourites()
                }
            } else {
                newFavs.add(key)
                persistSettings { it.copy(favIds = newFavs) }
                if (usesFavouritesFolder(s)) {
                    favSync.syncFavouriteAdded(s.copyFavTreeUri, item)
                    refreshFolderFavourites()
                }
            }
        }
    }

    fun isFavouriteItem(item: MediaItem): Boolean {
        val s = _settings.value
        if (item.stableKey in s.favIds) return true
        if (!usesFavouritesFolder(s)) return false
        return _folderFavourites.value.any {
            it.stableKey == item.stableKey || it.displayName == item.displayName
        }
    }

    fun toggleSelect(key: String) {
        val sel = _selectedKeys.value.toMutableSet()
        if (sel.contains(key)) sel.remove(key) else sel.add(key)
        _selectedKeys.value = sel
        _selectMode.value = sel.isNotEmpty()
    }

    fun enterSelectMode(key: String) {
        _selectMode.value = true
        _selectedKeys.value = _selectedKeys.value + key
    }

    fun exitSelectMode() {
        _selectMode.value = false
        _selectedKeys.value = emptySet()
    }

    fun favouriteSelected() {
        viewModelScope.launch {
            val keys = _selectedKeys.value
            persistSettings { it.copy(favIds = it.favIds + keys) }
            val s = _settings.value
            if (usesFavouritesFolder(s)) {
                keys.forEach { key ->
                    mediaByKey(key)?.let { favSync.syncFavouriteAdded(s.copyFavTreeUri, it) }
                }
                refreshFolderFavourites()
            }
            exitSelectMode()
        }
    }

    fun deleteSelected() {
        if (_settings.value.deletesDisabled) {
            showDeleteDisabledPrompt()
            return
        }
        _confirmDeleteKeys.value = _selectedKeys.value.toList()
    }

    fun openViewer(keys: List<String>, index: Int, autoPlay: Boolean = false, slideshowMode: Boolean = autoPlay) {
        val live = keys.filter { it !in _deletedKeys.value }
        if (live.isEmpty()) return
        if (!_viewerOpen.value) {
            val ret = _currentTab.value
            _viewerReturnTab.value = if (ret == AppTab.SLIDESHOW) AppTab.GALLERY else ret
        }
        _viewerList.value = live
        _viewerIndex.value = index.coerceIn(0, live.lastIndex)
        _viewerOpen.value = true
        _viewerChrome.value = true
        _viewerMenuOpen.value = false
        _speedMenuOpen.value = false
        _detailsOpen.value = false
        _viewerSlideshowMode.value = slideshowMode
        _currentTab.value = AppTab.SLIDESHOW
        _viewerPlaying.value = slideshowMode && autoPlay
        if (_viewerPlaying.value) scheduleSlideshow() else slideshowJob?.cancel()
    }

    fun closeViewer() {
        slideshowJob?.cancel()
        _viewerOpen.value = false
        _viewerPlaying.value = false
        _viewerSlideshowMode.value = false
        _viewerMenuOpen.value = false
        _speedMenuOpen.value = false
        _detailsOpen.value = false
        _currentTab.value = _viewerReturnTab.value
    }

    /** System back / predictive back — returns true if the event was consumed. */
    fun handleSystemBack(): Boolean {
        when {
            _viewerMenuOpen.value -> {
                _viewerMenuOpen.value = false
                return true
            }
            _speedMenuOpen.value -> {
                _speedMenuOpen.value = false
                return true
            }
            _detailsOpen.value -> {
                closeDetails()
                return true
            }
            _customSpeedOpen.value -> {
                dismissCustomSpeed()
                return true
            }
            _confirmDeleteKeys.value != null -> {
                cancelDelete()
                return true
            }
            _confirmResetSettings.value -> {
                cancelResetSettings()
                return true
            }
            _hiddenFoldersDialog.value -> {
                closeHiddenFoldersDialog()
                return true
            }
            _multiVideo.value.pickerIndex != null -> {
                closeMultiVideoPicker()
                return true
            }
            _viewerOpen.value -> {
                closeViewer()
                return true
            }
            _albumOpen.value != null -> {
                closeAlbum()
                return true
            }
            _selectMode.value -> {
                exitSelectMode()
                return true
            }
            else -> return false
        }
    }

    fun toggleViewerChrome() { _viewerChrome.value = !_viewerChrome.value }

    fun viewerNavigate(delta: Int) {
        val list = _viewerList.value.filter { it !in _deletedKeys.value }
        if (list.isEmpty()) return
        var idx = _viewerIndex.value + delta
        if (idx < 0) idx = list.lastIndex
        if (idx > list.lastIndex) idx = 0
        _viewerIndex.value = idx
        _viewerList.value = list
        if (_viewerPlaying.value) scheduleSlideshow()
    }

    fun viewerSwipeUpDelete() {
        val s = _settings.value
        if (s.disableSwipeDelete) {
            showSnack("Swipe-up delete is off. Enable it in More → Playback & Safety.")
            return
        }
        if (s.deletesDisabled) {
            showDeleteDisabledPrompt()
            return
        }
        val key = currentViewerKey() ?: return
        _confirmDeleteKeys.value = listOf(key)
    }

    fun togglePlayPause() {
        _viewerPlaying.value = !_viewerPlaying.value
        if (_viewerPlaying.value) scheduleSlideshow() else slideshowJob?.cancel()
    }

    fun setSpeedIndex(index: Int) {
        if (index == SlideshowSpeeds.CUSTOM_INDEX) {
            _customSpeedOpen.value = true
            _customSpeedSeconds.value = (_settings.value.customMs / 1000).toInt().coerceAtLeast(1)
            _speedMenuOpen.value = false
            return
        }
        persistSettings { it.copy(speedIdx = index) }
        _speedMenuOpen.value = false
        if (_viewerPlaying.value) scheduleSlideshow()
    }

    fun confirmCustomSpeed(seconds: Int) {
        val v = seconds.coerceAtLeast(1)
        persistSettings { it.copy(customMs = v * 1000L, speedIdx = SlideshowSpeeds.CUSTOM_INDEX) }
        _customSpeedOpen.value = false
        if (_viewerPlaying.value) scheduleSlideshow()
    }

    fun dismissCustomSpeed() { _customSpeedOpen.value = false }

    fun toggleSpeedMenu() {
        _speedMenuOpen.value = !_speedMenuOpen.value
        _viewerMenuOpen.value = false
    }

    fun toggleViewerMenu() {
        _viewerMenuOpen.value = !_viewerMenuOpen.value
        _speedMenuOpen.value = false
    }

    fun openDetails() {
        _detailsOpen.value = true
        _viewerMenuOpen.value = false
    }

    fun closeDetails() { _detailsOpen.value = false }

    fun shareCurrentItem(): Intent? {
        val item = currentViewerItem() ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun requestDeleteCurrent() {
        if (_settings.value.deletesDisabled) {
            showDeleteDisabledPrompt()
            return
        }
        currentViewerKey()?.let { _confirmDeleteKeys.value = listOf(it) }
    }

    fun cancelDelete() { _confirmDeleteKeys.value = null }

    fun confirmDelete() {
        val keys = _confirmDeleteKeys.value ?: return
        _confirmDeleteKeys.value = null
        _deletedKeys.value = _deletedKeys.value + keys
        _selectedKeys.value = emptySet()
        _selectMode.value = false
        val live = _viewerList.value.filter { it !in _deletedKeys.value }
        if (_viewerOpen.value && live.isEmpty()) closeViewer()
        else if (_viewerOpen.value) {
            _viewerIndex.value = _viewerIndex.value.coerceAtMost(live.lastIndex.coerceAtLeast(0))
            _viewerList.value = live
        }
        _pendingUndoDeletes.value = keys
        showSnack(
            if (keys.size > 1) "${keys.size} files deleted" else "File deleted",
            "Undo",
        ) {
            _deletedKeys.value = _deletedKeys.value - keys.toSet()
            _pendingUndoDeletes.value = emptyList()
        }
    }

    private fun showDeleteDisabledPrompt() {
        showSnack("Delete is disabled. Turn it back on in More → Playback & Safety.")
    }

    fun requestResetSettings() { _confirmResetSettings.value = true }
    fun cancelResetSettings() { _confirmResetSettings.value = false }

    fun confirmResetSettings() {
        _confirmResetSettings.value = false
        viewModelScope.launch {
            val previous = _settings.value
            pendingUndoSettings = previous
            val defaults = settingsRepo.resetToDefaults(keepFavourites = true)
            _settings.value = defaults
            showSnack("Settings reset", "Undo") {
                pendingUndoSettings?.let { snap ->
                    viewModelScope.launch {
                        settingsRepo.update { snap }
                        _settings.value = snap
                        pendingUndoSettings = null
                        refreshMedia()
                    }
                }
            }
            refreshMedia()
        }
    }

    fun cycleRecentWindow() {
        setRecentWindow(FavWindow.cycle(_settings.value.recentWindow))
    }

    fun cycleFavWindow() = setFavWindow(FavWindow.cycle(_settings.value.favWindow))

    /** Favourites date window (independent of Recents). */
    fun setFavWindow(window: FavWindow) = persistSettings { s ->
        s.copy(favWindow = FavWindow.normalize(window))
    }

    /** Recents date window (independent of Favourites). */
    fun setRecentWindow(window: FavWindow) = persistSettings { s ->
        val canonical = FavWindow.normalize(window)
        s.copy(
            recentWindow = canonical,
            recentWindowDays = canonical.asRecentDays() ?: 365,
        )
    }

    /** @deprecated shared setter — prefer [setFavWindow] / [setRecentWindow]. */
    fun setListWindow(window: FavWindow) = setFavWindow(window)

    fun toggleFavType(key: String) {
        persistSettings { s ->
            s.copy(favTypes = toggleTypeFilter(s.favTypes, key))
        }
    }

    fun toggleRecentType(key: String) {
        persistSettings { s ->
            s.copy(recentTypes = toggleTypeFilter(s.recentTypes, key))
        }
    }

    private fun toggleTypeFilter(ft: FileTypeFilter, key: String): FileTypeFilter {
        val updated = when (key) {
            "photo" -> ft.copy(photo = !ft.photo)
            "video" -> ft.copy(video = !ft.video)
            "gif" -> ft.copy(gif = !ft.gif)
            "audio" -> ft.copy(audio = !ft.audio)
            else -> ft
        }
        val anyOn = updated.photo || updated.video || updated.gif || updated.audio
        return if (anyOn) updated else ft
    }

    fun toggleFavTypeMenu() { _favTypeMenuOpen.value = !_favTypeMenuOpen.value }
    fun toggleRecentTypeMenu() { _recentTypeMenuOpen.value = !_recentTypeMenuOpen.value }

    fun toggleTheme() = persistSettings {
        it.copy(themeMode = if (it.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    fun toggleAmoled() = persistSettings { it.copy(amoled = !it.amoled) }

    fun setAccent(accent: AccentColor) = persistSettings { it.copy(accent = accent) }

    fun toggleFolder(path: String) = persistSettings { s ->
        val sel = s.selectedFolders.toMutableSet()
        val normalized = MediaRepository.normalizeFolderPath(path)
        val existing = sel.find { MediaRepository.normalizeFolderPath(it).equals(normalized, ignoreCase = true) }
        if (existing != null) sel.remove(existing) else sel.add(normalized)
        s.copy(selectedFolders = sel)
    }

    fun addSafTreeUri(uri: String) = persistSettings { s ->
        s.copy(
            safTreeUris = s.safTreeUris + uri,
            selectedFolders = s.selectedFolders + "SAF:${Uri.parse(uri).lastPathSegment}",
        )
    }

    fun toggleFileType(ext: String) = persistSettings { s ->
        val map = s.fileTypes.toMutableMap()
        map[ext] = !(map[ext] ?: true)
        s.copy(fileTypes = map)
    }

    fun toggleBehaviour(key: String) = persistSettings { s ->
        when (key) {
            "dontLoop" -> s.copy(dontLoop = !s.dontLoop)
            "disableSwipeDelete" -> s.copy(disableSwipeDelete = !s.disableSwipeDelete)
            "disableDeleteOptions" -> s.copy(
                disableDeleteOptions = !s.disableDeleteOptions,
                disableEditDelete = !s.disableDeleteOptions,
            )
            "disableEditDelete" -> s.copy(
                disableEditDelete = !s.disableEditDelete,
                disableDeleteOptions = !s.disableEditDelete,
            )
            "hapticsEnabled" -> s.copy(hapticsEnabled = !s.hapticsEnabled)
            "thumbnailPadding" -> s.copy(thumbnailPadding = !s.thumbnailPadding)
            else -> s
        }
    }

    fun toggleCopyFavs() {
        persistSettings { it.copy(copyFavs = !it.copyFavs) }
        viewModelScope.launch {
            val s = _settings.value
            if (usesFavouritesFolder(s)) {
                val fromIds = _allMedia.value.filter { it.stableKey in s.favIds }
                favSync.syncAll(s.copyFavTreeUri, fromIds, previousNames = emptySet())
            }
            refreshFolderFavourites()
        }
    }

    fun setCopyFavFolder(uri: String, path: String) {
        persistSettings { it.copy(copyFavTreeUri = uri, copyFavPath = path) }
        viewModelScope.launch {
            val s = _settings.value.copy(copyFavTreeUri = uri, copyFavPath = path)
            if (s.copyFavs) {
                val fromIds = _allMedia.value.filter { it.stableKey in s.favIds }
                favSync.syncAll(uri, fromIds, previousNames = emptySet())
            }
            refreshFolderFavourites(s.copy(copyFavs = _settings.value.copyFavs))
        }
    }

    fun openHiddenFoldersDialog() { _hiddenFoldersDialog.value = true }
    fun closeHiddenFoldersDialog() { _hiddenFoldersDialog.value = false }

    fun toggleHiddenFolder(key: String) = persistSettings { s ->
        val map = s.hiddenFolders.toMutableMap()
        map[key] = !(map[key] ?: false)
        s.copy(hiddenFolders = map)
    }

    fun toggleGroupCollapsed(name: String) {
        val c = _collapsedGroups.value.toMutableSet()
        if (c.contains(name)) c.remove(name) else c.add(name)
        _collapsedGroups.value = c
    }

    fun moveTab(tab: AppTab, direction: Int) = persistSettings { s ->
        val order = s.tabOrder.toMutableList()
        val i = order.indexOf(tab)
        if (i < 0) return@persistSettings s
        val j = i + direction
        if (j !in order.indices) return@persistSettings s
        Collections.swap(order, i, j)
        s.copy(tabOrder = order)
    }

    fun toggleTabVisibility(tab: AppTab) {
        if (tab.locked) return
        persistSettings { s ->
            val hidden = s.tabHidden.toMutableSet()
            val enabling = hidden.contains(tab)
            if (enabling) hidden.remove(tab) else hidden.add(tab)
            val order = s.tabOrder.toMutableList()
            if (enabling && tab !in order) {
                val si = order.indexOf(AppTab.SETTINGS)
                order.add(if (si >= 0) si else order.size, tab)
            }
            val features = when (tab) {
                AppTab.MULTIVIDEO -> s.tabFeatures.copy(multivideo = enabling)
                AppTab.ALBUM -> s.tabFeatures.copy(album = enabling)
                else -> s.tabFeatures
            }
            if (!enabling && _currentTab.value == tab) {
                _currentTab.value = AppTab.GALLERY
            }
            s.copy(tabHidden = hidden, tabOrder = order, tabFeatures = features)
        }
    }

    fun toggleMultiVideoFeature() = toggleTabVisibility(AppTab.MULTIVIDEO)

    fun toggleAlbumFeature() = toggleTabVisibility(AppTab.ALBUM)

    fun openAlbum(path: String) {
        _albumOpen.value = path
        _currentTab.value = AppTab.ALBUM
    }

    fun closeAlbum() { _albumOpen.value = null }

    fun exportSettings(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = settingsRepo.exportJson(_settings.value)
            onResult(json)
            showSnack("Settings exported")
        }
    }

    fun importSettings(json: String) {
        viewModelScope.launch {
            settingsRepo.importJson(json)
            showSnack("Settings imported")
            refreshMedia()
        }
    }

    /** DEVICE-ONLY: Import settings JSON and/or a favourites zip from the document picker. */
    fun importSettingsOrFavourites(uri: Uri) {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val name = runCatching {
                resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    }
            }.getOrNull().orEmpty()
            val mime = resolver.getType(uri).orEmpty()
            val isZip = mime.contains("zip", ignoreCase = true) ||
                name.endsWith(".zip", ignoreCase = true)

            if (isZip) {
                val s = _settings.value
                favExporter.importFavouritesZip(
                    zipUri = uri,
                    favTreeUri = s.copyFavTreeUri.takeIf { s.copyFavs && it.isNotBlank() },
                ).onSuccess { result ->
                    result.settingsJson?.let { settingsRepo.importJson(it) }
                    if (result.displayNames.isNotEmpty()) {
                        val matched = _allMedia.value
                            .filter { it.displayName in result.displayNames.toSet() }
                            .map { it.stableKey }
                            .toSet()
                        if (matched.isNotEmpty()) {
                            persistSettings { it.copy(favIds = it.favIds + matched) }
                        }
                    }
                    refreshMedia()
                    val parts = buildList {
                        if (result.mediaCount > 0) add("${result.mediaCount} media")
                        if (result.settingsJson != null) add("settings")
                    }
                    showSnack(
                        if (parts.isEmpty()) "Nothing to import"
                        else "Imported ${parts.joinToString(" + ")}",
                    )
                }.onFailure {
                    showSnack(it.message ?: "Import failed")
                }
            } else {
                val json = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json.isNullOrBlank()) {
                    showSnack("Unable to read file")
                    return@launch
                }
                settingsRepo.importJson(json)
                showSnack("Settings imported")
                refreshMedia()
            }
        }
    }

    fun downloadFavouritesZip(onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            val favs = uiState.value.favourites
            favExporter.exportFavouritesZip(favs).onSuccess { uri ->
                onResult(uri)
                showSnack("Zipping ${favs.size} favourites…")
            }.onFailure {
                showSnack(it.message ?: "Export failed")
            }
        }
    }

    fun showSnack(text: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        snackJob?.cancel()
        _snack.value = SnackMessage(text, actionLabel, action)
        snackJob = viewModelScope.launch {
            delay(4_000)
            _snack.value = null
        }
    }

    fun dismissSnack() { _snack.value = null }

    fun runSnackAction() {
        _snack.value?.action?.invoke()
        _snack.value = null
    }

    // Multi-video — DEVICE-ONLY playback wiring in UI layer
    fun setMultiVideoCount(count: Int) {
        _multiVideo.update { it.copy(count = count) }
        showMultiVideoOverlay()
    }

    fun toggleMultiVideoLandscape() {
        _multiVideo.update { it.copy(landscape = !it.landscape) }
        showMultiVideoOverlay()
    }

    fun exitMultiVideoLandscape() {
        _multiVideo.update { it.copy(landscape = false) }
        showMultiVideoOverlay()
    }

    fun onMultiVideoCellTap(index: Int) {
        val mv = _multiVideo.value
        val cell = mv.cells.getOrNull(index) ?: return
        if (cell.uri == null) {
            openMultiVideoPicker(index)
            return
        }
        if (mv.landscape) {
            // Immersive: tap toggles chrome overlay
            _multiVideo.update { it.copy(chromeVisible = !it.chromeVisible, overlayVisible = !it.chromeVisible) }
            return
        }
        if (!mv.overlayVisible) {
            showMultiVideoOverlay()
        } else {
            toggleMultiVideoCellPlay(index)
        }
    }

    fun updateMultiVideoProgress(index: Int, progress: Float) {
        _multiVideo.update { mv ->
            if (index !in mv.cells.indices) return@update mv
            val cells = mv.cells.toMutableList()
            val c = cells[index]
            if (c.progress == progress) return@update mv
            cells[index] = c.copy(progress = progress.coerceIn(0f, 1f))
            mv.copy(cells = cells)
        }
    }

    fun multiVideoPlayAll() {
        _multiVideo.update { mv ->
            mv.copy(cells = mv.cells.mapIndexed { i, c ->
                if (i < mv.count) c.copy(playing = true) else c
            })
        }
        showMultiVideoOverlay()
    }

    fun multiVideoPauseAll() {
        _multiVideo.update { mv ->
            mv.copy(cells = mv.cells.mapIndexed { i, c ->
                if (i < mv.count) c.copy(playing = false) else c
            })
        }
    }

    fun multiVideoMuteAll() {
        _multiVideo.update { mv ->
            val m = !mv.muteAll
            mv.copy(muteAll = m, cells = mv.cells.mapIndexed { i, c ->
                if (i < mv.count) c.copy(muted = m) else c
            })
        }
        showMultiVideoOverlay()
    }

    fun toggleMultiVideoCellPlay(index: Int) {
        _multiVideo.update { mv ->
            val cells = mv.cells.toMutableList()
            val c = cells[index]
            cells[index] = c.copy(playing = !c.playing)
            mv.copy(cells = cells)
        }
        showMultiVideoOverlay()
    }

    fun toggleMultiVideoCellMute(index: Int) {
        _multiVideo.update { mv ->
            val cells = mv.cells.toMutableList()
            val c = cells[index]
            cells[index] = c.copy(muted = !c.muted)
            mv.copy(cells = cells)
        }
    }

    fun openMultiVideoPicker(index: Int) {
        _multiVideo.update { it.copy(pickerIndex = index) }
    }

    fun closeMultiVideoPicker() { _multiVideo.update { it.copy(pickerIndex = null) } }

    fun assignMultiVideo(index: Int, item: MediaItem?) {
        _multiVideo.update { mv ->
            val cells = mv.cells.toMutableList()
            cells[index] = cells[index].copy(
                mediaId = item?.id,
                uri = item?.uri?.toString(),
                displayName = item?.displayName,
                isAudio = item?.mediaType == MediaType.AUDIO,
                playing = item != null,
                progress = 0f,
            )
            mv.copy(cells = cells, pickerIndex = null)
        }
        showMultiVideoOverlay()
    }

    fun assignMultiVideoUri(index: Int, uri: String, displayName: String?, isAudio: Boolean) {
        _multiVideo.update { mv ->
            val cells = mv.cells.toMutableList()
            cells[index] = cells[index].copy(
                mediaId = null,
                uri = uri,
                displayName = displayName,
                isAudio = isAudio,
                playing = true,
                progress = 0f,
            )
            mv.copy(cells = cells, pickerIndex = null)
        }
        showMultiVideoOverlay()
    }

    fun showMultiVideoOverlay() {
        mvOverlayJob?.cancel()
        _multiVideo.update { it.copy(overlayVisible = true) }
        mvOverlayJob = viewModelScope.launch {
            delay(3_000)
            if (_currentTab.value == AppTab.MULTIVIDEO) {
                _multiVideo.update { it.copy(overlayVisible = false) }
            }
        }
    }

    private fun scheduleSlideshow() {
        slideshowJob?.cancel()
        val s = _settings.value
        if (!_viewerPlaying.value || !_viewerOpen.value) return
        if (s.speedIdx == SlideshowSpeeds.OFF_INDEX) return
        val item = currentViewerItem()
        val delayMs = when {
            item?.mediaType == MediaType.VIDEO && item.durationMs > 0 -> item.durationMs
            s.speedIdx == SlideshowSpeeds.CUSTOM_INDEX -> s.customMs
            else -> SlideshowSpeeds.speeds.getOrNull(s.speedIdx)?.ms ?: 5_000L
        }
        if (delayMs <= 0L) return
        slideshowJob = viewModelScope.launch {
            delay(delayMs)
            val list = _viewerList.value.filter { it !in _deletedKeys.value }
            if (list.isEmpty()) {
                _viewerPlaying.value = false
                return@launch
            }
            var next = _viewerIndex.value + 1
            if (next >= list.size) {
                if (s.dontLoop) {
                    _viewerPlaying.value = false
                    return@launch
                }
                next = 0
            }
            _viewerIndex.value = next
            scheduleSlideshow()
        }
    }

    private fun persistSettings(block: (AppSettings) -> AppSettings) {
        val updated = block(_settings.value)
        _settings.value = updated
        viewModelScope.launch {
            settingsRepo.update { updated }
        }
    }

    private fun liveOrderedKeys(): List<String> {
        val deleted = _deletedKeys.value
        val order = _shuffleOrder.value.filter { it !in deleted }
        val known = order.toSet()
        val extras = _allMedia.value.map { it.stableKey }.filter { it !in deleted && it !in known }
        return order + extras
    }

    private fun mediaByKey(key: String): MediaItem? =
        _allMedia.value.find { it.stableKey == key }
            ?: _folderFavourites.value.find { it.stableKey == key }

    private fun passType(item: MediaItem, s: AppSettings): Boolean {
        val ft = effectiveFileTypes(s)
        if (ft.isNotEmpty() && ft.containsKey(item.extension) && ft[item.extension] == false) return false
        return when (item.mediaType) {
            MediaType.PHOTO, MediaType.VIDEO, MediaType.GIF, MediaType.AUDIO -> true
            MediaType.OTHER -> item.extension in SlideshowSpeeds.supportedExtensions
        }
    }

    private fun passFavType(item: MediaItem, filter: FileTypeFilter): Boolean = when (item.mediaType) {
        MediaType.PHOTO -> filter.photo
        MediaType.VIDEO -> filter.video
        MediaType.GIF -> filter.gif
        MediaType.AUDIO -> filter.audio
        MediaType.OTHER -> false
    }

    private fun tabSourceList(tab: AppTab, albumPath: String?): List<String> {
        val state = uiState.value
        return when (tab) {
            AppTab.FAV -> state.favourites.map { it.stableKey }
            AppTab.RECENT -> state.recent.map { it.stableKey }
            AppTab.ALBUM -> {
                if (albumPath != null && state.albumDetail.isNotEmpty()) {
                    state.albumDetail.map { it.stableKey }
                } else {
                    state.gallery.map { it.stableKey }
                }
            }
            // Settings / Multi-Video / Gallery / Slideshow fallback → gallery order
            else -> state.gallery.map { it.stableKey }
        }
    }

    private fun currentViewerKey(): String? =
        _viewerList.value.filter { it !in _deletedKeys.value }.getOrNull(_viewerIndex.value)

    private fun currentViewerItem(): MediaItem? =
        currentViewerKey()?.let { mediaByKey(it) }

    private fun buildUiState(
        settings: AppSettings,
        allMedia: List<MediaItem>,
        folderFavourites: List<MediaItem>,
        shuffleOrder: List<String>,
        deleted: Set<String>,
        discovered: List<MediaRepository.FolderInfo>,
        tab: AppTab,
        viewerOpen: Boolean,
        viewerList: List<String>,
        viewerIndex: Int,
        viewerPlaying: Boolean,
        viewerChrome: Boolean,
        viewerMenuOpen: Boolean,
        speedMenuOpen: Boolean,
        detailsOpen: Boolean,
        customSpeedOpen: Boolean,
        customSpeedSeconds: Int,
        selectMode: Boolean,
        selectedKeys: Set<String>,
        confirmDelete: List<String>?,
        hiddenDialog: Boolean,
        favTypeMenu: Boolean,
        collapsed: Set<String>,
        albumOpen: String?,
        multiVideo: MultiVideoState,
        snack: SnackMessage?,
        loading: Boolean,
        viewerSlideshowMode: Boolean,
        confirmReset: Boolean,
        recentTypeMenu: Boolean,
    ): GalleryUiState {
        val mediaMap = allMedia.associateBy { it.stableKey }
        val folderMap = folderFavourites.associateBy { it.stableKey }
        val lookup = mediaMap + folderMap
        val liveKeys = shuffleOrder.filter { it !in deleted && mediaMap.containsKey(it) } +
            allMedia.map { it.stableKey }.filter { it !in deleted && it !in shuffleOrder.toSet() }

        val gallery = liveKeys.mapNotNull { mediaMap[it] }.filter { passType(it, settings) }
        val favWindow = FavWindow.normalize(settings.favWindow)
        val recentWindow = FavWindow.normalize(settings.recentWindow)
        val favourites = try {
            if (settings.copyFavs && settings.copyFavTreeUri.isNotBlank()) {
                folderFavourites.filter { item ->
                    item.stableKey !in deleted &&
                        passFavType(item, settings.favTypes) &&
                        favWindow.matches(item.ageDays())
                }
            } else {
                liveKeys.mapNotNull { mediaMap[it] }.filter { item ->
                    settings.favIds.contains(item.stableKey) &&
                        passFavType(item, settings.favTypes) &&
                        favWindow.matches(item.ageDays())
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("GalleryVM", "Favourites filter failed window=$favWindow", t)
            emptyList()
        }
        val recent = try {
            liveKeys.mapNotNull { mediaMap[it] }
                .filter { item ->
                    passType(item, settings) &&
                        passFavType(item, settings.recentTypes) &&
                        recentWindow.matches(item.ageDays())
                }
                .sortedBy { it.ageDays() }
        } catch (t: Throwable) {
            android.util.Log.e("GalleryVM", "Recent filter failed window=$recentWindow", t)
            emptyList()
        }
        val videos = allMedia.filter {
            (it.mediaType == MediaType.VIDEO || it.mediaType == MediaType.AUDIO) && passType(it, settings)
        }
        val selectedNormalized = MediaRepository.mediaStoreFolderKeys(settings.selectedFolders)
        val albums = discovered.filter {
            MediaRepository.folderMatchesSelection(it.path, selectedNormalized)
        }
        val albumDetail = if (albumOpen != null) {
            val albumNorm = setOf(MediaRepository.normalizeFolderPath(albumOpen))
            gallery.filter { MediaRepository.folderMatchesSelection(it.folderPath, albumNorm) }
        } else emptyList()

        val noFolders = MediaRepository.mediaStoreFolderKeys(settings.selectedFolders).isEmpty() &&
            settings.safTreeUris.isEmpty()

        val viewerLive = viewerList.filter { it !in deleted }
        val viewerItem = viewerLive.getOrNull(viewerIndex)?.let { lookup[it] }

        val visibleTabs = settings.tabOrder.filter { tab -> tab !in settings.tabHidden }

        return GalleryUiState(
            settings = settings,
            currentTab = tab,
            visibleTabs = visibleTabs,
            gallery = gallery,
            favourites = favourites,
            recent = recent,
            videos = videos,
            albums = albums,
            discoveredFolders = discovered,
            albumDetail = albumDetail,
            albumOpen = albumOpen,
            noFolders = noFolders,
            loading = loading,
            viewerOpen = viewerOpen,
            viewerItem = viewerItem,
            viewerIndex = viewerIndex,
            viewerCount = viewerLive.size,
            viewerPlaying = viewerPlaying,
            viewerChrome = viewerChrome,
            viewerMenuOpen = viewerMenuOpen,
            viewerSlideshowMode = viewerSlideshowMode,
            speedMenuOpen = speedMenuOpen,
            detailsOpen = detailsOpen,
            customSpeedOpen = customSpeedOpen,
            customSpeedSeconds = customSpeedSeconds,
            selectMode = selectMode,
            selectedKeys = selectedKeys,
            confirmDeleteKeys = confirmDelete,
            confirmResetSettings = confirmReset,
            hiddenFoldersDialog = hiddenDialog,
            favTypeMenuOpen = favTypeMenu,
            recentTypeMenuOpen = recentTypeMenu,
            collapsedGroups = collapsed,
            multiVideo = multiVideo,
            snack = snack,
            mediaByKey = lookup,
        )
    }

    override fun onCleared() {
        slideshowJob?.cancel()
        snackJob?.cancel()
        mvOverlayJob?.cancel()
        refreshJob?.cancel()
        super.onCleared()
    }
}

data class GalleryUiState(
    val settings: AppSettings = AppSettings(),
    val currentTab: AppTab = AppTab.GALLERY,
    val visibleTabs: List<AppTab> = AppTab.defaultOrder.filter { it !in AppTab.defaultHidden },
    val gallery: List<MediaItem> = emptyList(),
    val favourites: List<MediaItem> = emptyList(),
    val recent: List<MediaItem> = emptyList(),
    val videos: List<MediaItem> = emptyList(),
    val albums: List<MediaRepository.FolderInfo> = emptyList(),
    val discoveredFolders: List<MediaRepository.FolderInfo> = emptyList(),
    val albumDetail: List<MediaItem> = emptyList(),
    val albumOpen: String? = null,
    val noFolders: Boolean = true,
    val loading: Boolean = false,
    val viewerOpen: Boolean = false,
    val viewerItem: MediaItem? = null,
    val viewerIndex: Int = 0,
    val viewerCount: Int = 0,
    val viewerPlaying: Boolean = false,
    val viewerChrome: Boolean = true,
    val viewerMenuOpen: Boolean = false,
    val viewerSlideshowMode: Boolean = false,
    val speedMenuOpen: Boolean = false,
    val detailsOpen: Boolean = false,
    val customSpeedOpen: Boolean = false,
    val customSpeedSeconds: Int = 8,
    val selectMode: Boolean = false,
    val selectedKeys: Set<String> = emptySet(),
    val confirmDeleteKeys: List<String>? = null,
    val confirmResetSettings: Boolean = false,
    val hiddenFoldersDialog: Boolean = false,
    val favTypeMenuOpen: Boolean = false,
    val recentTypeMenuOpen: Boolean = false,
    val collapsedGroups: Set<String> = emptySet(),
    val multiVideo: MultiVideoState = MultiVideoState(),
    val snack: SnackMessage? = null,
    val mediaByKey: Map<String, MediaItem> = emptyMap(),
)
