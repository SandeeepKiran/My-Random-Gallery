package com.mousy.myrandomgallery.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mousy.myrandomgallery.data.model.AccentColor
import com.mousy.myrandomgallery.data.model.AppSettings
import com.mousy.myrandomgallery.data.model.AppTab
import com.mousy.myrandomgallery.data.model.FavWindow
import com.mousy.myrandomgallery.data.model.FileTypeFilter
import com.mousy.myrandomgallery.data.model.GridMode
import com.mousy.myrandomgallery.data.model.TabFeatures
import com.mousy.myrandomgallery.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "my_random_gallery_settings")

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = if (prefs[Keys.THEME_DARK] != false) ThemeMode.DARK else ThemeMode.LIGHT,
            amoled = prefs[Keys.AMOLED] ?: false,
            accent = AccentColor.fromKey(prefs[Keys.ACCENT] ?: AccentColor.ROSE.key),
            columns = prefs[Keys.COLUMNS] ?: 3,
            gridMode = if (prefs[Keys.GRID_SCROLL] == true) GridMode.SCROLL else GridMode.SWIPE,
            selectedFolders = prefs[Keys.SELECTED_FOLDERS] ?: emptySet(),
            safTreeUris = prefs[Keys.SAF_TREE_URIS] ?: emptySet(),
            fileTypes = decodeFileTypes(prefs[Keys.FILE_TYPES]),
            favIds = prefs[Keys.FAV_IDS] ?: emptySet(),
            dontLoop = prefs[Keys.DONT_LOOP] ?: false,
            disableSwipeDelete = prefs[Keys.DISABLE_SWIPE_DELETE] ?: true,
            disableEditDelete = prefs[Keys.DISABLE_EDIT_DELETE] ?: false,
            copyFavs = prefs[Keys.COPY_FAVS] ?: false,
            copyFavPath = prefs[Keys.COPY_FAV_PATH] ?: "",
            copyFavTreeUri = prefs[Keys.COPY_FAV_TREE_URI] ?: "",
            hiddenFolders = decodeHiddenFolders(prefs[Keys.HIDDEN_FOLDERS]),
            tabFeatures = TabFeatures(
                multivideo = prefs[Keys.TAB_FEATURE_MV] ?: false,
                album = prefs[Keys.TAB_FEATURE_ALBUM] ?: false,
            ),
            tabOrder = decodeTabOrder(prefs[Keys.TAB_ORDER]),
            tabHidden = decodeTabHidden(prefs[Keys.TAB_HIDDEN]),
            speedIdx = prefs[Keys.SPEED_IDX] ?: 2,
            customMs = prefs[Keys.CUSTOM_MS] ?: 8_000L,
            recentWindowDays = prefs[Keys.RECENT_WINDOW] ?: 30,
            favWindow = decodeFavWindow(prefs[Keys.FAV_WINDOW]),
            favTypes = FileTypeFilter(
                photo = prefs[Keys.FAV_TYPE_PHOTO] ?: true,
                video = prefs[Keys.FAV_TYPE_VIDEO] ?: true,
                gif = prefs[Keys.FAV_TYPE_GIF] ?: true,
            ),
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = settingsFromPrefs(prefs)
            val updated = transform(current)
            prefs[Keys.THEME_DARK] = updated.themeMode == ThemeMode.DARK
            prefs[Keys.AMOLED] = updated.amoled
            prefs[Keys.ACCENT] = updated.accent.key
            prefs[Keys.COLUMNS] = updated.columns
            prefs[Keys.GRID_SCROLL] = updated.gridMode == GridMode.SCROLL
            prefs[Keys.SELECTED_FOLDERS] = updated.selectedFolders
            prefs[Keys.SAF_TREE_URIS] = updated.safTreeUris
            prefs[Keys.FILE_TYPES] = encodeFileTypes(updated.fileTypes)
            prefs[Keys.FAV_IDS] = updated.favIds
            prefs[Keys.DONT_LOOP] = updated.dontLoop
            prefs[Keys.DISABLE_SWIPE_DELETE] = updated.disableSwipeDelete
            prefs[Keys.DISABLE_EDIT_DELETE] = updated.disableEditDelete
            prefs[Keys.COPY_FAVS] = updated.copyFavs
            prefs[Keys.COPY_FAV_PATH] = updated.copyFavPath
            prefs[Keys.COPY_FAV_TREE_URI] = updated.copyFavTreeUri
            prefs[Keys.HIDDEN_FOLDERS] = encodeHiddenFolders(updated.hiddenFolders)
            prefs[Keys.TAB_FEATURE_MV] = updated.tabFeatures.multivideo
            prefs[Keys.TAB_FEATURE_ALBUM] = updated.tabFeatures.album
            prefs[Keys.TAB_ORDER] = updated.tabOrder.joinToString(",") { it.key }
            prefs[Keys.TAB_HIDDEN] = updated.tabHidden.joinToString(",") { it.key }
            prefs[Keys.SPEED_IDX] = updated.speedIdx
            prefs[Keys.CUSTOM_MS] = updated.customMs
            prefs[Keys.RECENT_WINDOW] = updated.recentWindowDays
            prefs[Keys.FAV_WINDOW] = encodeFavWindow(updated.favWindow)
            prefs[Keys.FAV_TYPE_PHOTO] = updated.favTypes.photo
            prefs[Keys.FAV_TYPE_VIDEO] = updated.favTypes.video
            prefs[Keys.FAV_TYPE_GIF] = updated.favTypes.gif
        }
    }

    suspend fun exportJson(settings: AppSettings): String = SettingsJson.encode(settings)

    suspend fun importJson(json: String): AppSettings {
        val imported = SettingsJson.decode(json)
        update { imported }
        return imported
    }

    private fun settingsFromPrefs(prefs: Preferences): AppSettings = AppSettings(
        themeMode = if (prefs[Keys.THEME_DARK] != false) ThemeMode.DARK else ThemeMode.LIGHT,
        amoled = prefs[Keys.AMOLED] ?: false,
        accent = AccentColor.fromKey(prefs[Keys.ACCENT] ?: AccentColor.ROSE.key),
        columns = prefs[Keys.COLUMNS] ?: 3,
        gridMode = if (prefs[Keys.GRID_SCROLL] == true) GridMode.SCROLL else GridMode.SWIPE,
        selectedFolders = prefs[Keys.SELECTED_FOLDERS] ?: emptySet(),
        safTreeUris = prefs[Keys.SAF_TREE_URIS] ?: emptySet(),
        fileTypes = decodeFileTypes(prefs[Keys.FILE_TYPES]),
        favIds = prefs[Keys.FAV_IDS] ?: emptySet(),
        dontLoop = prefs[Keys.DONT_LOOP] ?: false,
        disableSwipeDelete = prefs[Keys.DISABLE_SWIPE_DELETE] ?: true,
        disableEditDelete = prefs[Keys.DISABLE_EDIT_DELETE] ?: false,
        copyFavs = prefs[Keys.COPY_FAVS] ?: false,
        copyFavPath = prefs[Keys.COPY_FAV_PATH] ?: "",
        copyFavTreeUri = prefs[Keys.COPY_FAV_TREE_URI] ?: "",
        hiddenFolders = decodeHiddenFolders(prefs[Keys.HIDDEN_FOLDERS]),
        tabFeatures = TabFeatures(
            multivideo = prefs[Keys.TAB_FEATURE_MV] ?: false,
            album = prefs[Keys.TAB_FEATURE_ALBUM] ?: false,
        ),
        tabOrder = decodeTabOrder(prefs[Keys.TAB_ORDER]),
        tabHidden = decodeTabHidden(prefs[Keys.TAB_HIDDEN]),
        speedIdx = prefs[Keys.SPEED_IDX] ?: 2,
        customMs = prefs[Keys.CUSTOM_MS] ?: 8_000L,
        recentWindowDays = prefs[Keys.RECENT_WINDOW] ?: 30,
        favWindow = decodeFavWindow(prefs[Keys.FAV_WINDOW]),
        favTypes = FileTypeFilter(
            photo = prefs[Keys.FAV_TYPE_PHOTO] ?: true,
            video = prefs[Keys.FAV_TYPE_VIDEO] ?: true,
            gif = prefs[Keys.FAV_TYPE_GIF] ?: true,
        ),
    )

    private object Keys {
        val THEME_DARK = booleanPreferencesKey("theme_dark")
        val AMOLED = booleanPreferencesKey("amoled")
        val ACCENT = stringPreferencesKey("accent")
        val COLUMNS = intPreferencesKey("columns")
        val GRID_SCROLL = booleanPreferencesKey("grid_scroll")
        val SELECTED_FOLDERS = stringSetPreferencesKey("selected_folders")
        val SAF_TREE_URIS = stringSetPreferencesKey("saf_tree_uris")
        val FILE_TYPES = stringPreferencesKey("file_types")
        val FAV_IDS = stringSetPreferencesKey("fav_ids")
        val DONT_LOOP = booleanPreferencesKey("dont_loop")
        val DISABLE_SWIPE_DELETE = booleanPreferencesKey("disable_swipe_delete")
        val DISABLE_EDIT_DELETE = booleanPreferencesKey("disable_edit_delete")
        val COPY_FAVS = booleanPreferencesKey("copy_favs")
        val COPY_FAV_PATH = stringPreferencesKey("copy_fav_path")
        val COPY_FAV_TREE_URI = stringPreferencesKey("copy_fav_tree_uri")
        val HIDDEN_FOLDERS = stringPreferencesKey("hidden_folders")
        val TAB_FEATURE_MV = booleanPreferencesKey("tab_feature_mv")
        val TAB_FEATURE_ALBUM = booleanPreferencesKey("tab_feature_album")
        val TAB_ORDER = stringPreferencesKey("tab_order")
        val TAB_HIDDEN = stringPreferencesKey("tab_hidden")
        val SPEED_IDX = intPreferencesKey("speed_idx")
        val CUSTOM_MS = longPreferencesKey("custom_ms")
        val RECENT_WINDOW = intPreferencesKey("recent_window")
        val FAV_WINDOW = stringPreferencesKey("fav_window")
        val FAV_TYPE_PHOTO = booleanPreferencesKey("fav_type_photo")
        val FAV_TYPE_VIDEO = booleanPreferencesKey("fav_type_video")
        val FAV_TYPE_GIF = booleanPreferencesKey("fav_type_gif")
    }

    private fun decodeTabOrder(raw: String?): List<AppTab> {
        if (raw.isNullOrBlank()) return AppTab.defaultOrder
        return raw.split(",").mapNotNull { AppTab.fromKey(it.trim()) }
            .ifEmpty { AppTab.defaultOrder }
    }

    private fun decodeTabHidden(raw: String?): Set<AppTab> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").mapNotNull { AppTab.fromKey(it.trim()) }.toSet()
    }

    private fun decodeFavWindow(raw: String?): FavWindow = when {
        raw == null || raw == "all" -> FavWindow.ALL
        raw.startsWith("days:") -> FavWindow.Days(raw.removePrefix("days:").toIntOrNull() ?: 30)
        else -> FavWindow.ALL
    }

    private fun encodeFavWindow(window: FavWindow): String = when (window) {
        FavWindow.ALL -> "all"
        is FavWindow.Days -> "days:${window.days}"
    }

    private fun encodeFileTypes(map: Map<String, Boolean>): String =
        map.entries.joinToString(";") { "${it.key}=${it.value}" }

    private fun decodeFileTypes(raw: String?): Map<String, Boolean> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            part.substring(0, idx) to (part.substring(idx + 1).toBooleanStrictOrNull() ?: true)
        }.toMap()
    }

    private fun encodeHiddenFolders(map: Map<String, Boolean>): String =
        map.entries.joinToString(";") { "${it.key}=${it.value}" }

    private fun decodeHiddenFolders(raw: String?): Map<String, Boolean> {
        if (raw.isNullOrBlank()) return AppSettings.defaultHiddenFolders()
        return raw.split(";").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            part.substring(0, idx) to (part.substring(idx + 1).toBooleanStrictOrNull() ?: false)
        }.toMap().ifEmpty { AppSettings.defaultHiddenFolders() }
    }
}

private object SettingsJson {
    fun encode(s: AppSettings): String = buildString {
        append("{")
        append("\"theme\":\"${s.themeMode.name.lowercase()}\",")
        append("\"amoled\":${s.amoled},")
        append("\"accent\":\"${s.accent.key}\",")
        append("\"columns\":${s.columns},")
        append("\"gridMode\":\"${s.gridMode.name.lowercase()}\",")
        append("\"selectedFolders\":${s.selectedFolders.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},")
        append("\"speedIdx\":${s.speedIdx},")
        append("\"customMs\":${s.customMs},")
        append("\"recentWindow\":${s.recentWindowDays},")
        append("\"favWindow\":\"${when (val w = s.favWindow) { FavWindow.ALL -> "all"; is FavWindow.Days -> "days:${w.days}" }}\",")
        append("\"favIds\":${s.favIds.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}")
        append("}")
    }

    fun decode(json: String): AppSettings {
        // Minimal JSON parse for export/import backup
        fun extractString(key: String): String? {
            val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            return pattern.find(json)?.groupValues?.getOrNull(1)
        }
        fun extractBool(key: String): Boolean? =
            "\"$key\"\\s*:\\s*(true|false)".toRegex().find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        fun extractInt(key: String): Int? =
            "\"$key\"\\s*:\\s*(\\d+)".toRegex().find(json)?.groupValues?.get(1)?.toIntOrNull()

        val favWindowRaw = extractString("favWindow")
        val favWindow = when {
            favWindowRaw == null || favWindowRaw == "all" -> FavWindow.ALL
            favWindowRaw.startsWith("days:") -> FavWindow.Days(favWindowRaw.removePrefix("days:").toIntOrNull() ?: 30)
            else -> FavWindow.ALL
        }

        val favIds = "\"favIds\"\\s*:\\s*\\[([^\\]]*)\\]".toRegex().find(json)?.groupValues?.get(1)
            ?.split(",")
            ?.map { it.trim().trim('"') }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: emptySet()

        return AppSettings(
            themeMode = if (extractString("theme") == "light") ThemeMode.LIGHT else ThemeMode.DARK,
            amoled = extractBool("amoled") ?: false,
            accent = AccentColor.fromKey(extractString("accent") ?: "rose"),
            columns = extractInt("columns") ?: 3,
            gridMode = if (extractString("gridMode") == "scroll") GridMode.SCROLL else GridMode.SWIPE,
            favIds = favIds,
            speedIdx = extractInt("speedIdx") ?: 2,
            customMs = extractInt("customMs")?.toLong() ?: 8_000L,
            recentWindowDays = extractInt("recentWindow") ?: 30,
            favWindow = favWindow,
        )
    }
}
