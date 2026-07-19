package com.mousy.myrandomgallery.data.model

import android.net.Uri

enum class MediaType {
    PHOTO, VIDEO, GIF, OTHER
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val extension: String,
    val mediaType: MediaType,
    val folderPath: String,
    val dateTakenMs: Long,
    val dateAddedMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long = 0L,
) {
    val stableKey: String get() = "${id}_${uri}"

    fun ageDays(nowMs: Long = System.currentTimeMillis()): Int {
        val ref = if (dateAddedMs > 0) dateAddedMs else dateTakenMs
        return ((nowMs - ref) / 86_400_000L).toInt().coerceAtLeast(0)
    }
}
