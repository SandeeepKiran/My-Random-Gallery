package com.mousy.myrandomgallery

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath

/**
 * Configures Coil memory + disk caches and default crossfade for the whole app.
 */
class GalleryApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val appContext = this
        return ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, percent = 0.28)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("coil_image_cache").toOkioPath())
                    .maxSizeBytes(512L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
