package com.stelgen.vlom.mcml.render

import com.stelgen.vlom.mcml.McmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты S1.3: фокус-модель D-pad. Сэмпл — реальная страница с initialFocus.
 */
class FocusModelTest {

    private fun build(name: String): RenderNode = RenderNodeBuilder.build(
        McmlParser.parse(
            name,
            javaClass.getResourceAsStream("/mcml/$name") ?: error("missing $name"),
        ).root!!,
    )

    private fun button(id: String, left: Int, top: Int, w: Int = 100, h: Int = 40) =
        RenderNode.Button(
            id = id, cls = "Button", geometry = RenderNode.Geometry(left, top, w, h),
            children = emptyList(),
        )

    @Test
    fun `initialFocus attribute selects element by id`() {
        val root = build("sample_focused.mcml")
        val fm = FocusModel(root, initialFocusId = "_btnNext")
        assertEquals("_btnNext", fm.focused?.id)
    }

    @Test
    fun `initialFocus unknown id falls back to first focusable`() {
        val root = build("sample_focused.mcml")
        val fm = FocusModel(root, initialFocusId = "_nonexistent")
        assertTrue(fm.focused?.id != null && fm.focusables.contains(fm.focused))
    }

    @Test
    fun `spatial navigation moves right`() {
        val root = button("_a", 0, 0).let {
            RenderNode.Panel("root", null, RenderNode.Geometry(), listOf(it, button("_b", 500, 0), button("_c", 500, 300)), null)
        }
        val fm = FocusModel(root, "_a")
        assertTrue(fm.move(FocusModel.Direction.RIGHT))
        assertEquals("_b", fm.focused?.id)
        // сверху нет ничего
        assertFalse(fm.move(FocusModel.Direction.UP))
    }

    @Test
    fun `spatial navigation prefers aligned column`() {
        val grid = RenderNode.Panel(
            "root", null, RenderNode.Geometry(),
            listOf(
                button("_a1", 0, 0), button("_a2", 0, 300),
                button("_b1", 500, 0), button("_b2", 500, 300),
            ),
            null,
        )
        val fm = FocusModel(grid, "_a1")
        assertTrue(fm.move(FocusModel.Direction.DOWN))
        assertEquals("_a2", fm.focused?.id)
        assertTrue(fm.move(FocusModel.Direction.RIGHT))
        assertEquals("_b2", fm.focused?.id)
    }

    @Test
    fun `single focusable does not move`() {
        val root = RenderNode.Panel("r", null, RenderNode.Geometry(), listOf(button("_only", 0, 0)), null)
        val fm = FocusModel(root, "_only")
        assertFalse(fm.move(FocusModel.Direction.RIGHT))
        assertEquals("_only", fm.focused?.id)
    }

    @Test
    fun `no focusables is safe`() {
        val root = RenderNode.Panel("r", null, RenderNode.Geometry(), emptyList(), null)
        val fm = FocusModel(root, null)
        assertNull(fm.focused?.id ?: fm.focused?.let { it.id })
        assertFalse(fm.move(FocusModel.Direction.DOWN))
    }

    @Test
    fun `real focused page has focusables`() {
        val root = build("sample_focused.mcml")
        val fm = FocusModel(root, null)
        assertTrue(fm.focusables.isNotEmpty())
    }
}
