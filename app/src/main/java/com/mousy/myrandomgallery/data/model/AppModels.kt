package com.mousy.myrandomgallery.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val label: String, val icon: ImageVector, val locked: Boolean = false) {
    FAV("Favourites", Icons.Default.Favorite),
    RECENT("Recent", Icons.Default.Schedule),
    GALLERY("Gallery", Icons.Default.Shuffle, locked = true),
    SLIDESHOW("Slideshow", Icons.Default.Slideshow),
    MULTIVIDEO("Videos", Icons.Default.VideoLibrary),
    ALBUM("Albums", Icons.Default.PhotoAlbum),
    SETTINGS("More", Icons.Default.MoreHoriz, locked = true);

    val key: String get() = name.lowercase()

    companion object {
        fun fromKey(key: String): AppTab? = entries.find { it.key == key }

        /** Full tab strip including optional Multi-Video / Album (hidden by default). */
        val defaultOrder: List<AppTab> = listOf(
            FAV, RECENT, GALLERY, SLIDESHOW, MULTIVIDEO, ALBUM, SETTINGS,
        )

        /** Optional tabs start unchecked in Settings tab-order UI. */
        val defaultHidden: Set<AppTab> = setOf(MULTIVIDEO, ALBUM)
    }
}

enum class GridMode {
    SWIPE, SCROLL;

    val icon get() = when (this) {
        SWIPE -> Icons.Default.Shuffle
        SCROLL -> Icons.AutoMirrored.Filled.ViewList
    }
}

enum class ThemeMode { LIGHT, DARK }

enum class AccentColor(val key: String, val label: String, val pastelHex: Long) {
    ROSE("rose", "Rose", 0xFFE9A8BD),
    LAVENDER("lavender", "Lavender", 0xFFC9B7F5),
    MINT("mint", "Mint", 0xFF8FF0C4),
    PEACH("peach", "Peach", 0xFFFFCBA8),
    SKY("sky", "Sky", 0xFFA9D9FF),
    SAND("sand", "Sand", 0xFFECD79A);

    companion object {
        fun fromKey(key: String): AccentColor = entries.find { it.key == key } ?: ROSE
    }
}

data class FileTypeFilter(
    val photo: Boolean = true,
    val video: Boolean = true,
    val gif: Boolean = true,
    val audio: Boolean = true,
)

data class TabFeatures(
    val multivideo: Boolean = false,
    val album: Boolean = false,
)

data class MultiVideoCell(
    val index: Int,
    val mediaId: Long? = null,
    val uri: String? = null,
    val displayName: String? = null,
    val isAudio: Boolean = false,
    val playing: Boolean = false,
    val muted: Boolean = false,
    val progress: Float = 0f,
)

data class MultiVideoState(
    val count: Int = 2,
    val muteAll: Boolean = false,
    val landscape: Boolean = false,
    val overlayVisible: Boolean = true,
    val chromeVisible: Boolean = true,
    val pickerIndex: Int? = null,
    val cells: List<MultiVideoCell> = List(4) { MultiVideoCell(index = it) },
)

data class SnackMessage(
    val text: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val amoled: Boolean = false,
    val accent: AccentColor = AccentColor.ROSE,
    val columns: Int = 3,
    val gridMode: GridMode = GridMode.SWIPE,
    val selectedFolders: Set<String> = emptySet(),
    val safTreeUris: Set<String> = emptySet(),
    val fileTypes: Map<String, Boolean> = emptyMap(),
    /** Extension → count of files seen in last scan (for Settings UI). */
    val discoveredFileTypeCounts: Map<String, Int> = emptyMap(),
    val favIds: Set<String> = emptySet(),
    val dontLoop: Boolean = false,
    val disableSwipeDelete: Boolean = true,
    /** Hides delete UI and blocks all delete actions app-wide. */
    val disableDeleteOptions: Boolean = false,
    /** Legacy alias kept for import/export compatibility. */
    val disableEditDelete: Boolean = false,
    val hapticsEnabled: Boolean = true,
    /** Padding + rounded corners on gallery thumbnails. */
    val thumbnailPadding: Boolean = true,
    val copyFavs: Boolean = false,
    val copyFavPath: String = "",
    val copyFavTreeUri: String = "",
    val hiddenFolders: Map<String, Boolean> = defaultHiddenFolders(),
    val tabFeatures: TabFeatures = TabFeatures(),
    val tabOrder: List<AppTab> = AppTab.defaultOrder,
    val tabHidden: Set<AppTab> = AppTab.defaultHidden,
    val speedIdx: Int = 2,
    val customMs: Long = 8_000L,
    /** Legacy day count; kept for migration. Prefer [recentWindow]. */
    val recentWindowDays: Int = 30,
    /** Favourites date window (independent of Recents). */
    val favWindow: FavWindow = FavWindow.ALL,
    /** Favourites media-type filter (independent of Recents). */
    val favTypes: FileTypeFilter = FileTypeFilter(),
    /** Recents date window (independent of Favourites). */
    val recentWindow: FavWindow = FavWindow.Days(30),
    /** Recents media-type filter (independent of Favourites). */
    val recentTypes: FileTypeFilter = FileTypeFilter(),
    /** Persisted gallery shuffle pages (pipe-separated keys; pages joined by `;`). */
    val shuffleHistoryEncoded: String = "",
    val shuffleHistoryIndex: Int = 0,
) {
    /** Effective “delete disabled” — either dedicated or legacy toggle. */
    val deletesDisabled: Boolean get() = disableDeleteOptions || disableEditDelete

    companion object {
        fun defaultHiddenFolders(): Map<String, Boolean> = mapOf(
            ".thumbnails" to false,
            ".hidden_pics" to false,
            "Android/media" to false,
            ".Trash" to false,
        )

        fun defaults(): AppSettings = AppSettings()
    }
}

/**
 * Date window for Favourites / Recents filters.
 *
 * Important: always resolve through [normalize] / companion [options] so Days(365)
 * is never an orphan instance that breaks index lookups.
 */
sealed class FavWindow {
    data object ALL : FavWindow()
    data class Days(val days: Int) : FavWindow()

    fun label(): String = when (this) {
        ALL -> "All time"
        is Days -> when (days) {
            365 -> "1 year"
            else -> "$days days"
        }
    }

    /** Compact label for filter buttons (avoids overflow on narrow screens). */
    fun shortLabel(): String = when (this) {
        ALL -> "All time"
        is Days -> when (days) {
            365 -> "1y"
            else -> "${days}d"
        }
    }

    fun matches(ageDays: Int): Boolean = when (this) {
        ALL -> true
        is Days -> {
            val limit = days.coerceIn(1, 3650)
            val age = ageDays.coerceAtLeast(0)
            age <= limit
        }
    }

    fun sameAs(other: FavWindow): Boolean = when (this) {
        ALL -> other is ALL
        is Days -> other is Days && other.days == days
    }

    /** Day count for legacy Recents sync; null means "all time". */
    fun asRecentDays(): Int? = when (this) {
        ALL -> null
        is Days -> days.coerceIn(1, 3650)
    }

    /** Stable encode key used in DataStore. */
    fun encode(): String = when (this) {
        ALL -> "all"
        is Days -> "days:${days.coerceIn(1, 3650)}"
    }

    companion object {
        val options: List<FavWindow> = listOf(
            ALL, Days(7), Days(14), Days(30), Days(60), Days(90), Days(365),
        )

        /** Map any window onto the canonical companion instance (fixes Days(365) identity bugs). */
        fun normalize(window: FavWindow): FavWindow = when (window) {
            ALL -> ALL
            is Days -> options.filterIsInstance<Days>().find { it.days == window.days }
                ?: Days(window.days.coerceIn(1, 3650))
        }

        fun fromRecentDays(days: Int): FavWindow {
            val d = days.coerceIn(1, 3650)
            return options.filterIsInstance<Days>().find { it.days == d } ?: Days(d)
        }

        fun decode(raw: String?): FavWindow {
            if (raw.isNullOrBlank()) return ALL
            return when {
                raw.equals("all", ignoreCase = true) -> ALL
                raw.startsWith("days:", ignoreCase = true) -> {
                    val days = raw.substringAfter(':').toIntOrNull()?.coerceIn(1, 3650) ?: 30
                    fromRecentDays(days)
                }
                // Numeric legacy
                raw.toIntOrNull() != null -> fromRecentDays(raw.toInt())
                else -> ALL
            }
        }

        fun cycle(current: FavWindow): FavWindow {
            val canonical = normalize(current)
            val idx = options.indexOfFirst { it.sameAs(canonical) }.takeIf { it >= 0 } ?: 0
            return options.getOrElse((idx + 1) % options.size) { ALL }
        }
    }
}

object SlideshowSpeeds {
    data class Speed(val label: String, val ms: Long)

    val speeds: List<Speed> = listOf(
        Speed("1s", 1_000),
        Speed("2s", 2_000),
        Speed("5s", 5_000),
        Speed("10s", 10_000),
        Speed("15s", 15_000),
        Speed("30s", 30_000),
        Speed("1min", 60_000),
        Speed("5min", 300_000),
        Speed("Custom", 8_000),
        Speed("Off", 0),
    )

    const val CUSTOM_INDEX = 8
    const val OFF_INDEX = 9

    val recentWindows: List<Int> = listOf(7, 14, 30, 60, 90, 365)

    val supportedExtensions: Set<String> = setOf(
        "png", "jpg", "jpeg", "webp", "gif", "mp4", "mp3", "m4a", "aac", "wav", "ogg", "flac",
    )

    val audioExtensions: Set<String> = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
}
