package com.stelgen.vlom.shell

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

/**
 * S4.1: загрузчик скин-битмапов из assets с LruCache.
 * Ключ — "theme/path"; ограничение ~1/8 кучи (низкорамные устройства P4).
 */
class SkinBitmaps(private val context: Context, private val theme: String = "blue") {

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt().coerceAtLeast(4 * 1024 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(path: String): Bitmap? {
        cache.get(path)?.let { return it }
        return try {
            context.assets.open("images/$theme/$path").use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.also { cache.put(path, it) }
        } catch (e: Exception) {
            null
        }
    }
}
