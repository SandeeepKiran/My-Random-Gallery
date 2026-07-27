package com.mousy.myrandomgallery

import android.app.Application
import android.content.pm.ApplicationInfo
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import coil3.video.VideoFrameDecoder
import okio.Path.Companion.toOkioPath

/**
 * Configures Coil memory + disk caches, video frame decoding, and default crossfade.
 */
class GalleryApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val appContext = this
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return ImageLoader.Builder(context)
            // Debug builds log every hit/miss, which is how thumbnail-cache regressions get
            // diagnosed instead of guessed at.
            .apply { if (debuggable) logger(DebugLogger()) }
            .crossfade(true)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                // Thumbnails are the whole product, so this app spends more of its heap on them
                // than Coil's default. At a ~440KB decoded thumbnail this holds a few hundred,
                // which is what keeps swipe-back instant instead of re-decoding.
                MemoryCache.Builder()
                    .maxSizePercent(context, percent = 0.45)
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
