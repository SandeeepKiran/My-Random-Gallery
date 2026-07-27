package com.mousy.myrandomgallery.data.model

import android.net.Uri

enum class MediaType {
    PHOTO, VIDEO, GIF, AUDIO, OTHER
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
        val ref = when {
            dateAddedMs > 0L -> dateAddedMs
            dateTakenMs > 0L -> dateTakenMs
            else -> return Int.MAX_VALUE / 4 // unknown date → fail closed for day filters
        }
        val diffMs = (nowMs - ref).coerceAtLeast(0L)
        val days = diffMs / 86_400_000L
        // Guard against toInt() overflow on pathological timestamps
        return days.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }
}
