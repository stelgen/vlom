package com.stelgen.vlom.shell

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable

/**
 * S2.2: иконки тайтлов (autoicon).
 *  - builtIn  -> оригинальные PNG из темы (images/blue, 1:1 ресурсы ehres);
 *  - appTile  -> иконка APK из PackageManager (autoicon по T3).
 */
class TileIconProvider(private val context: Context) {

    private val cache = mutableMapOf<String, Drawable?>()

    fun iconFor(tile: HomeTile): Drawable? = cache.getOrPut(tile.id) {
        when {
            tile.builtIn -> assetIcon(tile.id)
            tile.packageName != null -> appIcon(tile.packageName)
            else -> null
        }
    }

    private fun appIcon(pkg: String): Drawable? = try {
        context.packageManager.getApplicationIcon(pkg)
    } catch (e: Exception) {
        null
    }

    private fun assetIcon(builtInId: String): Drawable? {
        val file = BUILTIN_ASSETS[builtInId] ?: return null
        return try {
            context.assets.open("images/blue/$file").use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                android.graphics.drawable.BitmapDrawable(context.resources, bmp)
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** Иконки оригинального старт-меню Vista MCE (извлечены из ehres.blue). */
        val BUILTIN_ASSETS = mapOf(
            BuiltInTiles.MUSIC to "type10_STARTMENU.QUICKLINK.MUSIC.NOFOCUS.PNG.png",
            BuiltInTiles.PICTURES to "type10_STARTMENU.QUICKLINK.PICTURES.NOFOCUS.PNG.png",
            BuiltInTiles.VIDEO to "type10_STARTMENU.QUICKLINK.VIDEOS.NOFOCUS.PNG.png",
            BuiltInTiles.FRIGATE to "type10_NOTIFICATIONS.TOAST.ICON.CAMERA.PNG.png",
            BuiltInTiles.WEATHER to "type10_STARTMENU.QUICKLINK.EXTRAS.NOFOCUS.PNG.png",
            BuiltInTiles.SETTINGS to "type10_STARTMENU.QUICKLINK.SETTINGS.NOFOCUS.PNG.png",
        )
    }
}
