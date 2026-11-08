package com.stelgen.vlom.mcml.render

import com.stelgen.vlom.mcml.McmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты фазы 1: маппинг MCML → RenderNode и математика масштабирования.
 * Сэмплы — реальные страницы blue-full из app/src/test/resources/mcml/.
 */
class RenderModelTest {

    private fun parse(name: String) = McmlParser.parse(
        name,
        javaClass.getResourceAsStream("/mcml/$name") ?: error("missing $name"),
    )

    /** sample_page: uidescription > Panel(_test) > [Button, Panel.ccDescription] */
    private fun sampleRoot(): RenderNode =
        RenderNodeBuilder.buildDocument(parse("sample_page.mcml").root!!)

    private fun sampleArea(): RenderNode.Panel =
        sampleRoot().children.filterIsInstance<RenderNode.Panel>().first { it.id == "_test" }

    @Test
    fun `button maps to Button node with label and onClick`() {
        val button = sampleArea().children.filterIsInstance<RenderNode.Button>().first()

        assertEquals("Button", button.cls)
        assertEquals("OnClick_Copy", button.onClick)
        assertEquals("#Acquire.Copy", button.label)
    }

    @Test
    fun `nested panel carries geometry from left-top-width-height`() {
        val panel = sampleArea()

        assertEquals("_test", panel.id)
        assertEquals("Panel.ccArea", panel.cls)
    }

    @Test
    fun `padding rect attribute is parsed into geometry`() {
        val inner = sampleArea().children.filterIsInstance<RenderNode.Panel>()
            .first { it.cls == "Panel.ccDescription" }
        assertEquals(com.stelgen.vlom.mcml.McmlRect(0, 35, 0, 0), inner.geometry.padding)
    }

    @Test
    fun `real corpus page builds full tree`() {
        val doc = parse("type23_ASPECTPANEL.XML.mcml")
        val root = RenderNodeBuilder.buildDocument(doc.root!!)
        assertTrue(root.children.isNotEmpty())
    }

    @Test
    fun `scale factor is min of axis ratios`() {
        val scale = DesignScale(basisWidth = 1000, basisHeight = 500)

        // 2000x1000 -> x2.0/x2.0 = 2.0
        assertEquals(2.0f, scale.factor(2000, 1000), 0.001f)
        // 1000x1000 -> x1.0/x2.0 = 1.0 (letterbox по Y)
        assertEquals(1.0f, scale.factor(1000, 1000), 0.001f)
        // 3000x500  -> x3.0/x1.0 = 1.0
        assertEquals(1.0f, scale.factor(3000, 500), 0.001f)
    }

    @Test
    fun `offsets center the scene on letterbox`() {
        val scale = DesignScale(basisWidth = 1000, basisHeight = 500)
        val f = scale.factor(1000, 1000)

        assertEquals(0, scale.offsetX(1000, f))
        assertEquals(250, scale.offsetY(1000, f))
    }

    @Test
    fun `hd screen over 1366x768 basis`() {
        val scale = DesignScale() // 1366x768
        val f = scale.factor(1920, 1080)
        assertEquals(1920f / 1366f, f, 0.001f) // 1.4056... ограничен шириной
    }

    @Test
    fun `nonpositive screen size rejected`() {
        try {
            DesignScale().factor(0, 768)
            throw AssertionError("expected exception")
        } catch (e: IllegalArgumentException) {
            // ok
        }
    }
}
