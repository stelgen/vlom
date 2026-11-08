package com.stelgen.vlom.sound

import org.junit.Assert.assertEquals
import org.junit.Test

/** Тесты звуковой карты (S1.4): событие -> WAV 1:1 из assets. */
class SoundMapTest {

    @Test
    fun `focus change maps to hover jelly`() {
        assertEquals(SoundId.HOVER, SoundMap.soundFor(SoundMap.UiEvent.FOCUS_CHANGED))
    }

    @Test
    fun `click maps to selected jelly`() {
        assertEquals(SoundId.SELECTED, SoundMap.soundFor(SoundMap.UiEvent.BUTTON_CLICK))
    }

    @Test
    fun `page enter maps to launch`() {
        assertEquals(SoundId.LAUNCH, SoundMap.soundFor(SoundMap.UiEvent.PAGE_ENTER))
    }

    @Test
    fun `all mapped assets are unique`() {
        val names = SoundId.entries.map { it.asset }
        assertEquals(names.size, names.toSet().size)
    }
}
