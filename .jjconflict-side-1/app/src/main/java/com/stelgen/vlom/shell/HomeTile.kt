package com.stelgen.vlom.shell

/**
 * S2.1/S2.2: тайтл home-сетки (REQ-001 T1-T5).
 *
 * builtIn  — оригинальные тайтлы Vista MCE (Музыка/Видео/Картинки/Настройки) и
 *            служебные (Frigate v1 — веб-морда); запускаются внутренними экранами
 *            или назначенным приложением.
 * appTile  — любой установленный APK, добавленный пользователем (T3);
 *            autoicon из PackageManager, ручная замена иконки/имени — v2 (кастом).
 * webTile  — произвольный сайт (S3.3/B1): открывается встроенным браузером (WebActivity).
 */
data class HomeTile(
    val id: String,
    val label: String,            // русская строка (P6) или имя приложения
    val builtIn: Boolean = false,
    val packageName: String? = null, // для appTile: целевой APK
    val activityName: String? = null,
    val hidden: Boolean = false,
    val webUrl: String? = null,      // для webTile: адрес сайта
) {
    val isAppTile: Boolean get() = packageName != null
    val isWebTile: Boolean get() = webUrl != null
}

/** Встроенные тайтлы старт-скрина (порядок = оригинальный Vista MCE). */
object BuiltInTiles {
    const val MUSIC = "builtin.music"
    const val PICTURES = "builtin.pictures"
    const val VIDEO = "builtin.video"
    const val SETTINGS = "builtin.settings"
    const val FRIGATE = "builtin.frigate"
    const val WEATHER = "builtin.weather"

    val defaults = listOf(
        HomeTile(MUSIC, "Музыка", builtIn = true),
        HomeTile(PICTURES, "Картинки", builtIn = true),
        HomeTile(VIDEO, "Видео", builtIn = true),
        HomeTile(FRIGATE, "Наблюдение", builtIn = true),
        HomeTile(WEATHER, "Погода", builtIn = true),
        HomeTile(SETTINGS, "Настройки", builtIn = true),
    )
}
