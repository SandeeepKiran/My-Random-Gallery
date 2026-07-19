package com.mousy.myrandomgallery.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.mousy.myrandomgallery.data.model.MediaItem
import com.mousy.myrandomgallery.data.model.MediaType
import com.mousy.myrandomgallery.data.model.SlideshowSpeeds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * DEVICE-ONLY: Scans real on-device media via MediaStore and SAF tree URIs.
 * Requires storage permissions and user-selected folder access.
 */
class MediaRepository(private val context: Context) {

    data class FolderInfo(
        val path: String,
        val displayName: String,
        val group: String,
        val mediaCount: Int,
    )

    suspend fun scanMedia(
        selectedFolders: Set<String>,
        safTreeUris: Set<String>,
        hiddenFolders: Map<String, Boolean>,
        fileTypeFilters: Map<String, Boolean>,
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val storeFolders = mediaStoreFolderKeys(selectedFolders)
        val items = mutableListOf<MediaItem>()
        // Only query MediaStore when at least one real folder is selected.
        if (storeFolders.isNotEmpty()) {
            items += queryMediaStore(storeFolders, hiddenFolders, fileTypeFilters, discoverOnly = false)
        }
        if (safTreeUris.isNotEmpty()) {
            items += scanSafTrees(safTreeUris, fileTypeFilters)
        }
        items.distinctBy { it.stableKey }
    }

    /** DEVICE-ONLY: Scan a single SAF tree (e.g. favourites copy folder). */
    suspend fun scanSafTree(
        treeUri: String,
        fileTypeFilters: Map<String, Boolean> = emptyMap(),
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        if (treeUri.isBlank()) emptyList()
        else scanSafTrees(setOf(treeUri), fileTypeFilters)
    }

    suspend fun discoverFolders(
        hiddenFolders: Map<String, Boolean>,
    ): List<FolderInfo> = withContext(Dispatchers.IO) {
        val all = queryMediaStore(emptySet(), hiddenFolders, emptyMap(), discoverOnly = true)
        all.groupBy { it.folderPath }
            .map { (path, items) ->
                FolderInfo(
                    path = path,
                    displayName = path.substringAfterLast('/').ifBlank { path },
                    group = folderGroup(path),
                    mediaCount = items.size,
                )
            }
            .sortedBy { it.path }
    }

    suspend fun availableExtensions(selectedFolders: Set<String>): Set<String> =
        withContext(Dispatchers.IO) {
            scanMedia(selectedFolders, emptySet(), emptyMap(), emptyMap())
                .map { it.extension.lowercase(Locale.US) }
                .toSet()
        }

    private fun queryMediaStore(
        selectedFolders: Set<String>,
        hiddenFolders: Map<String, Boolean>,
        fileTypeFilters: Map<String, Boolean>,
        discoverOnly: Boolean = false,
    ): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        result += queryCollection(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            selectedFolders,
            hiddenFolders,
            fileTypeFilters,
            discoverOnly,
        )
        result += queryCollection(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            selectedFolders,
            hiddenFolders,
            fileTypeFilters,
            discoverOnly,
        )
        return result
    }

    private fun queryCollection(
        collection: Uri,
        selectedFolders: Set<String>,
        hiddenFolders: Map<String, Boolean>,
        fileTypeFilters: Map<String, Boolean>,
        discoverOnly: Boolean,
    ): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
        )

        val normalizedSelected = selectedFolders.map { normalizeFolderPath(it) }.toSet()
        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val relCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(nameCol) ?: continue
                val mime = cursor.getString(mimeCol) ?: "application/octet-stream"
                val relativePath = cursor.getString(relCol)?.let { normalizeFolderPath(it) } ?: run {
                    val dataPath = cursor.getString(dataCol) ?: return@run ""
                    normalizeFolderPath(inferRelativePath(dataPath))
                }
                if (relativePath.isBlank()) continue
                if (isHiddenPath(relativePath, hiddenFolders)) continue

                if (!discoverOnly) {
                    if (normalizedSelected.isEmpty()) continue
                    if (!folderMatchesSelection(relativePath, normalizedSelected)) continue
                }

                val ext = displayName.substringAfterLast('.', "").lowercase(Locale.US)
                if (fileTypeFilters.isNotEmpty() && fileTypeFilters.containsKey(ext) && fileTypeFilters[ext] == false) {
                    continue
                }
                if (fileTypeFilters.isEmpty() && ext.isNotEmpty() &&
                    ext !in SlideshowSpeeds.supportedExtensions &&
                    ext !in setOf("png", "jpg", "jpeg", "webp", "gif", "mp4")
                ) {
                    continue
                }

                val uri = ContentUris.withAppendedId(collection, id)
                val mediaType = classifyMedia(mime, ext)
                items += MediaItem(
                    id = id,
                    uri = uri,
                    displayName = displayName,
                    mimeType = mime,
                    extension = ext,
                    mediaType = mediaType,
                    folderPath = relativePath,
                    dateTakenMs = cursor.getLong(takenCol).let { if (it > 0) it else 0L },
                    dateAddedMs = cursor.getLong(addedCol) * 1_000L,
                    sizeBytes = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                )
            }
        }
        return items
    }

    /** DEVICE-ONLY: Walk SAF document trees the user granted via OpenDocumentTree. */
    private fun scanSafTrees(
        treeUris: Set<String>,
        fileTypeFilters: Map<String, Boolean>,
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        var syntheticId = -1L
        for (treeUriStr in treeUris) {
            val treeUri = Uri.parse(treeUriStr)
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: continue
            walkDocumentTree(root, root.name ?: "SAF", treeUriStr, fileTypeFilters) { doc, folderPath ->
                val name = doc.name ?: return@walkDocumentTree
                val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                if (fileTypeFilters.isNotEmpty() && fileTypeFilters[ext] == false) return@walkDocumentTree
                val mime = doc.type ?: guessMime(ext)
                syntheticId--
                items += MediaItem(
                    id = syntheticId,
                    uri = doc.uri,
                    displayName = name,
                    mimeType = mime,
                    extension = ext,
                    mediaType = classifyMedia(mime, ext),
                    folderPath = folderPath,
                    dateTakenMs = doc.lastModified(),
                    dateAddedMs = doc.lastModified(),
                    sizeBytes = doc.length(),
                    width = 0,
                    height = 0,
                    durationMs = 0L,
                )
            }
        }
        return items
    }

    private fun walkDocumentTree(
        node: DocumentFile,
        folderPath: String,
        treeUri: String,
        fileTypeFilters: Map<String, Boolean>,
        onFile: (DocumentFile, String) -> Unit,
    ) {
        for (child in node.listFiles()) {
            if (child.isDirectory) {
                walkDocumentTree(child, "$folderPath/${child.name}", treeUri, fileTypeFilters, onFile)
            } else if (child.isFile) {
                onFile(child, folderPath)
            }
        }
    }

    private fun isHiddenPath(path: String, hiddenFolders: Map<String, Boolean>): Boolean {
        val lower = path.lowercase(Locale.US)
        return hiddenFolders.any { (segment, include) ->
            !include && lower.contains(segment.lowercase(Locale.US))
        }
    }

    private fun inferRelativePath(dataPath: String): String {
        val markers = listOf("/Pictures/", "/DCIM/", "/Movies/", "/Download/", "/Downloads/")
        for (marker in markers) {
            val idx = dataPath.indexOf(marker, ignoreCase = true)
            if (idx >= 0) {
                val sub = dataPath.substring(idx + 1)
                return sub.substringBeforeLast('/')
            }
        }
        return dataPath.substringBeforeLast('/')
    }

    private fun classifyMedia(mime: String, ext: String): MediaType = when {
        mime.startsWith("video/") || ext == "mp4" -> MediaType.VIDEO
        ext == "gif" || mime.equals("image/gif", ignoreCase = true) -> MediaType.GIF
        mime.startsWith("image/") -> MediaType.PHOTO
        else -> MediaType.OTHER
    }

    private fun guessMime(ext: String): String = when (ext) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        else -> "application/octet-stream"
    }

    companion object {
        private val knownRoots = setOf(
            "pictures", "dcim", "movies", "download", "downloads", "myfiles", "documents",
        )

        /** Keys used for MediaStore folder checkboxes (exclude SAF synthetic keys). */
        fun mediaStoreFolderKeys(selectedFolders: Set<String>): Set<String> =
            selectedFolders
                .filter { !it.startsWith("SAF:", ignoreCase = true) }
                .map { normalizeFolderPath(it) }
                .filter { it.isNotBlank() }
                .toSet()

        fun normalizeFolderPath(path: String): String =
            path.trim().trim('/').replace('\\', '/').replace(Regex("/+"), "/")

        fun folderMatchesSelection(folderPath: String, selectedNormalized: Set<String>): Boolean {
            val path = normalizeFolderPath(folderPath).lowercase(Locale.US)
            if (path.isBlank() || selectedNormalized.isEmpty()) return false
            return selectedNormalized.any { sel ->
                val s = sel.lowercase(Locale.US)
                path == s || path.startsWith("$s/")
            }
        }

        fun folderGroup(path: String): String {
            val root = normalizeFolderPath(path).substringBefore('/', path)
            return if (root.lowercase(Locale.US) in knownRoots) root else "Other (found)"
        }
    }
}
