package com.stelgen.vlom.sound

/**
 * S1.4: звуковой движок (SoundPool, WAV 1:1 из assets/audio).
 *
 * В MCML-страницах звуков НЕТ (проверено по корпусу) — карта событий→звуки
 * живёт на стороне shell (ehshell). Здесь — явная карта:
 *   фокус сместился   -> ControlsJellyButtonAquaHover   (hover Jelly-кнопки)
 *   нажатие кнопки    -> ControlsJellyButtonAquaSelected
 *   выбор/активация   -> ControlsJellyButtonAquaSelecting
 *   вход на страницу  -> Launch
 *   выход (back)      -> ViewItemsVideoGlassBreaking
 *
 * SoundEngine — тонкая Android-обёртка над SoundPool; карта SoundMap — чистая
 * и тестируется в JVM.
 */
enum class SoundId(val asset: String) {
    HOVER("ControlsJellyButtonAquaHover.wav"),
    SELECTING("ControlsJellyButtonAquaSelecting.wav"),
    SELECTED("ControlsJellyButtonAquaSelected.wav"),
    LAUNCH("Launch.wav"),
    GLASS("ViewItemsVideoGlassBreaking.wav"),
}

object SoundMap {
    enum class UiEvent { FOCUS_CHANGED, BUTTON_CLICK, PAGE_ENTER, PAGE_LEAVE }

    fun soundFor(event: UiEvent): SoundId = when (event) {
        UiEvent.FOCUS_CHANGED -> SoundId.HOVER
        UiEvent.BUTTON_CLICK -> SoundId.SELECTED
        UiEvent.PAGE_ENTER -> SoundId.LAUNCH
        UiEvent.PAGE_LEAVE -> SoundId.GLASS
    }
}

/**
 * Android-обёртка. Ленивая загрузка WAV из assets/audio (U3: 1:1, без перекодирования).
 */
class SoundEngine(private val context: android.content.Context) {

    private val soundPool: android.media.SoundPool = android.media.SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = mutableMapOf<SoundId, Int>()

    /** Загрузить все WAV; безопасно вызывать повторно. */
    fun loadAll() {
        if (ids.isNotEmpty()) return
        SoundId.entries.forEach { id ->
            try {
                context.assets.openFd("audio/${id.asset}").use { afd ->
                    ids[id] = soundPool.load(afd, 1)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "no sound asset ${id.asset}", e)
            }
        }
    }

    fun play(event: SoundMap.UiEvent) {
        val id = ids[SoundMap.soundFor(event)] ?: return
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
        ids.clear()
    }

    companion object {
        private const val TAG = "SoundEngine"
    }
}
