package com.mousy.myrandomgallery.data.model

import kotlin.math.roundToInt
import kotlin.random.Random

object SamplingDefaults {
    /** Assumed items-per-session before we've learned anything about this user. */
    const val INITIAL_AVG_VIEWED = 800f
    /** Never prepare fewer than this, even for a user who only ever glances at the app. */
    const val MIN_SAMPLE = 300
    /** Prepare this much headroom above the user's typical session. */
    const val SAMPLE_HEADROOM = 1.5f
    /** Weight of the newest session when folding it into the moving average. */
    const val EMA_ALPHA = 0.3f
    /** Extend the working set once the user has seen this fraction of it. */
    const val EXTEND_AT_FRACTION = 0.8f

    fun sampleSizeFor(avgViewed: Float, totalCount: Int): Int {
        if (totalCount <= MIN_SAMPLE) return totalCount
        val target = (avgViewed.coerceAtLeast(1f) * SAMPLE_HEADROOM).roundToInt()
        return target.coerceIn(MIN_SAMPLE, totalCount)
    }

    fun updatedAverage(previous: Float, sessionCount: Int): Float {
        if (sessionCount <= 0) return previous
        val prev = previous.takeIf { it.isFinite() && it > 0f } ?: INITIAL_AVG_VIEWED
        return prev * (1f - EMA_ALPHA) + sessionCount * EMA_ALPHA
    }
}

/**
 * Draws a reproducible random slice of [keys] from a single [seed].
 *
 * This is a partial Fisher-Yates: only [count] swaps are performed, so taking 1,000 of
 * 10,000 keys costs 1,000 swaps instead of shuffling the whole library. Because the
 * shuffle is driven purely by the seed, asking for a larger [count] later returns the
 * same items in the same order plus the next ones — which is what makes "load more"
 * repeat-free and what lets swipe-back history store one Long per page.
 */
fun <T> seededSample(items: List<T>, seed: Long, count: Int): List<T> {
    val total = items.size
    if (total == 0) return emptyList()
    val take = count.coerceIn(1, total)

    val random = Random(seed)
    val order = IntArray(total) { it }
    val result = ArrayList<T>(take)
    for (i in 0 until take) {
        val j = i + random.nextInt(total - i)
        val pick = order[j]
        order[j] = order[i]
        order[i] = pick
        result.add(items[pick])
    }
    return result
}

fun newShuffleSeed(): Long = Random.nextLong()
