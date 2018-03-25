package com.roposo.creation.graphics.gles

import android.graphics.Bitmap
import android.util.LruCache
import com.roposo.core.constants.KB
import com.roposo.core.constants.MB

/**
 * @author Mayank on 24/01/18.
 */

// Get max available VM memory, exceeding this amount will throw an
// OutOfMemory exception. Stored in kilobytes as LruCache takes an
// int in its constructor.
object LruBitmapCache: LruCache<String, Bitmap>(cacheSize) {
    /**
     * @param maxSize for caches that do not override [.sizeOf], this is
     * the maximum number of entries in the cache. For all other caches,
     * this is the maximum sum of the sizes of the entries in this cache.
     */

    override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount / KB
}

private val cacheSize: Int
    get() {
        val maxMemory = Runtime.getRuntime().maxMemory() / KB
        return (maxMemory / 8).coerceIn(maxMemory / 2, 50L * MB).toInt()
    }