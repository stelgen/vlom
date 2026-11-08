package com.stelgen.vlom.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты автомержа (S2.2): хранилище юзера + PackageManager-факты.
 */
class TileRepositoryTest {

    /** In-memory хранилище для JVM-тестов. */
    private class MemStore : TileStore {
        var value: TileSettings? = null
        var saves = 0
        override fun load(): TileSettings = value ?: TileSettings()
        override fun save(settings: TileSettings) {
            value = settings
            saves++
        }
    }

    @Test
    fun `empty store yields defaults without builtins duplication`() {
        val store = MemStore()
        val s = TileRepository.load(store, emptySet())
        assertEquals(BuiltInTiles.defaults, s.tiles)
    }

    @Test
    fun `uninstalled app tiles are pruned and store updated`() {
        val store = MemStore().apply {
            value = TileSettings()
                .addApp("wink", "Wink", "com.winktv", null)
                .addApp("yt", "YouTube", "com.google.android.youtube.tv", null)
        }
        val s = TileRepository.load(store, setOf("com.winktv")) // yt снесён юзером

        assertTrue(s.tiles.any { it.packageName == "com.winktv" })
        assertFalse(s.tiles.any { it.packageName == "com.google.android.youtube.tv" })
        assertTrue(s.tiles.any { it.id == BuiltInTiles.MUSIC }) // builtIn цел
        assertEquals(1, store.saves) // изменилось — сохранилось
    }

    @Test
    fun `installed apps are kept as-is`() {
        val store = MemStore().apply {
            value = TileSettings().addApp("wink", "Wink", "com.winktv", null)
        }
        val s = TileRepository.load(store, setOf("com.winktv"))
        assertTrue(s.tiles.any { it.packageName == "com.winktv" })
        assertEquals(0, store.saves) // ничего не изменилось — записи нет
    }

    @Test
    fun `builtins are restored if store lost them`() {
        val store = MemStore().apply {
            value = TileSettings(tiles = listOf(HomeTile("app.x", "X", packageName = "x")))
        }
        val s = TileRepository.load(store, setOf("x"))
        // builtIn вернулись в начало, пользовательский тайтл не потерян
        assertEquals(BuiltInTiles.MUSIC, s.tiles.first().id)
        assertTrue(s.tiles.any { it.id == "app.x" })
    }

    @Test
    fun `ensureBuiltIns is idempotent`() {
        val s = TileRepository.ensureBuiltIns(TileSettings())
        assertEquals(BuiltInTiles.defaults, TileRepository.ensureBuiltIns(s).tiles)
    }
}
