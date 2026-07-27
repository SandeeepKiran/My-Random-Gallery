package com.mousy.myrandomgallery.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline-friendly diagnostics: dump recent logcat lines to a cache file and share via Intent.
 */
object LogCapture {
    private const val MAX_LINES = 2_500
    private const val FILE_NAME = "my_random_gallery_log.txt"

    fun captureToCache(context: Context): Result<File> = runCatching {
        val out = File(context.cacheDir, FILE_NAME)
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val header = buildString {
            appendLine("My Random Gallery diagnostics")
            appendLine("Captured: $stamp")
            appendLine("Package: ${context.packageName}")
            appendLine("--- logcat (tail) ---")
        }
        val body = dumpLogcat()
        out.writeText(header + body)
        out
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "My Random Gallery log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun dumpLogcat(): String {
        val process = Runtime.getRuntime().exec(
            arrayOf("logcat", "-d", "-t", MAX_LINES.toString(), "*:W"),
        )
        return try {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText()
            }.ifBlank { "(no logcat output — grant permission or run after a crash)" }
        } finally {
            process.destroy()
        }
    }
}
