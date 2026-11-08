package com.stelgen.vlom.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты геометрии полосы старт-скрина (S2.1) и менеджера тайтлов (S2.2).
 * Чистая математика и чистая логика — JVM.
 */
class ShellTest {

    // ---------- HomeLayout.stripe ----------

    @Test
    fun `stripe places focus tile centered`() {
        val p = HomeLayout.stripe(3, 1)
        val focus = p[1]
        val centerX = focus.x + focus.width / 2f
        assertEquals(1366f / 2f, centerX, 0.5f)
        assertEquals(HomeLayout.FOCUS_SCALE, focus.scale, 0.001f)
    }

    @Test
    fun `neighbours are smaller than the focus tile`() {
        val p = HomeLayout.stripe(3, 1)
        assertTrue(p[0].scale < p[1].scale)
        assertTrue(p[2].scale < p[1].scale)
        assertEquals(HomeLayout.NEIGHBOUR_SCALE, p[0].scale, 0.001f)
    }

    @Test
    fun `tiles keep the stripe pitch`() {
        val p = HomeLayout.stripe(4, 1)
        // мировые центры идут с шагом TILE_W+GAP
        val c = p.map { it.x + it.width / 2f }
        assertEquals(HomeLayout.TILE_W + HomeLayout.GAP, c[1] - c[0], 0.5f)
        assertEquals(HomeLayout.TILE_W + HomeLayout.GAP, c[2] - c[1], 0.5f)
    }

    @Test
    fun `empty stripe is empty and out-of-range focus is safe`() {
        assertTrue(HomeLayout.stripe(0, 0).isEmpty())
        assertTrue(HomeLayout.stripe(3, 99).isNotEmpty())
        assertTrue(HomeLayout.stripe(3, -5).isNotEmpty())
    }

    // ---------- TileSettings ----------

    private val base = TileSettings()

    @Test
    fun `defaults are the original Vista MCE titles`() {
        assertEquals(
            listOf("Музыка", "Картинки", "Видео", "Наблюдение", "Погода", "Настройки"),
            base.tiles.map { it.label },
        )
        assertTrue(base.visible.all { !it.hidden })
    }

    @Test
    fun `addApp deduplicates by package and activity`() {
        val s = base.addApp("wink", "Wink", "com.winktv", null)
        assertEquals(7, s.tiles.size)
        val again = s.addApp("wink2", "Wink", "com.winktv", null)
        assertEquals(7, again.tiles.size)
    }

    @Test
    fun `hide and show`() {
        val s = base.setHidden(BuiltInTiles.MUSIC, true)
        assertTrue(s.visible.none { it.id == BuiltInTiles.MUSIC })
        val back = s.setHidden(BuiltInTiles.MUSIC, false)
        assertTrue(back.visible.any { it.id == BuiltInTiles.MUSIC })
    }

    @Test
    fun `move reorders and builtins survive remove`() {
        val s = base.move(0, 4)
        assertEquals("Музыка", s.tiles[4].label)
        val s2 = s.remove(BuiltInTiles.MUSIC) // builtIn: остаётся
        assertTrue(s2.tiles.any { it.id == BuiltInTiles.MUSIC })
        val withApp = s.addApp("yt", "YouTube", "com.google.android.youtube.tv", null)
        val gone = withApp.remove("yt") // appTile: удаляется
        assertTrue(gone.tiles.none { it.id == "yt" })
    }

    @Test
    fun `prune drops tiles of uninstalled apps only`() {
        val s = base.addApp("wink", "Wink", "com.winktv", null)
        val pruned = s.pruneRemovedApps(setOf("com.winktv"))
        assertTrue(pruned.tiles.any { it.id == "wink" })
        val pruned2 = pruned.pruneRemovedApps(emptySet())
        assertTrue(pruned2.tiles.none { it.id == "wink" })
        assertTrue(pruned2.tiles.any { it.id == BuiltInTiles.MUSIC }) // builtIn остаётся
    }

    @Test
    fun `codec roundtrip preserves settings`() {
        val s = base.addApp("wink", "Wink", "com.winktv", "com.winktv.MainActivity")
            .addWebsite("web.jf", "Jellyfin", "http://192.168.1.10:8096")
            .setHidden(BuiltInTiles.VIDEO, true)
            .move(0, 2)
        val back = TileSettingsCodec.decode(TileSettingsCodec.encode(s))
        assertEquals(s.tiles, back.tiles)
        assertEquals(s.visible.map { it.id }, back.visible.map { it.id })
    }

    @Test
    fun `codec of garbage falls back to defaults`() {
        assertEquals(base.tiles, TileSettingsCodec.decode("not json").tiles)
    }
}
