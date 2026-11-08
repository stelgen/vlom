package com.stelgen.vlom.shell

/**
 * S4.1: каталог скинов — соответствие тайтлов оригинальным PNG-ресурсам ehres.
 *
 * Факты из ресурсов (проверено по размерам IHDR):
 *  - QUICKLINK.<N>.FOCUS = 258×145 — большой стеклянный тайл фокуса (графика+текст вшиты);
 *  - QUICKLINK.<N>.NOFOCUS = 162×91 — маленькая иконка не-фокусного состояния;
 *  - QUICKLINK.BACKGROUND = 258×145 — общий фон фокусного тайла (для тайтлов
 *    без собственной FOCUS-картинки: Frigate/Погода/приложения/сайты);
 *  - COMMON.BACKGROUND = 500×300 — фон страницы.
 *
 * Все пути — относительно assets/images/<theme>/.
 */
object SkinCatalog {

    /** Пути скинов для одного тайтла (null = нет собственного, использовать fallback). */
    data class TileSkin(val focus: String?, val nofocus: String?)

    private const val QL = "type10_STARTMENU.QUICKLINK"
    private const val PNG = ".PNG.png"

    val homeBackground = "type10_COMMON.BACKGROUND.PNG.png"
    val focusBackground = "$QL.BACKGROUND$PNG"

    private val builtin = mapOf(
        BuiltInTiles.MUSIC to TileSkin("$QL.MUSIC.FOCUS$PNG", "$QL.MUSIC.NOFOCUS$PNG"),
        BuiltInTiles.PICTURES to TileSkin("$QL.PICTURES.FOCUS$PNG", "$QL.PICTURES.NOFOCUS$PNG"),
        BuiltInTiles.VIDEO to TileSkin("$QL.VIDEOS.FOCUS$PNG", "$QL.VIDEOS.NOFOCUS$PNG"),
        BuiltInTiles.SETTINGS to TileSkin("$QL.SETTINGS.FOCUS$PNG", "$QL.SETTINGS.NOFOCUS$PNG"),
        // нет собственных FOCUS-картинок в ehres — общий стеклянный фон + иконка
        BuiltInTiles.FRIGATE to TileSkin(null, "type10_NOTIFICATIONS.TOAST.ICON.CAMERA.PNG.png"),
        BuiltInTiles.WEATHER to TileSkin(null, "$QL.EXTRAS.NOFOCUS$PNG"),
    )

    fun forTile(tile: HomeTile): TileSkin? = builtin[tile.id]

    /** Иконка фокусного тайла (для тайтлов без FOCUS-картинки). */
    fun focusIcon(tile: HomeTile): String? = forTile(tile)?.nofocus
}
