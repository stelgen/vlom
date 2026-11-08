package com.stelgen.vlom.mcml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты парсера MCML (S0.3). Сэмплы — обрезки реальных страниц blue-full
 * (app/src/test/resources/mcml/). Канон: тест = контракт.
 */
class McmlParserTest {

    private fun assertNotNullRoot(doc: McmlDocument): McmlElement {
        assertNotNull(doc.root)
        return doc.root!!
    }

    private fun parseResource(name: String): McmlDocument =
        McmlParser.parse(
            name,
            javaClass.getResourceAsStream("/mcml/$name") ?: error("resource not found: $name"),
        )

    @Test
    fun `parses uidescription with namespaced children`() {
        val doc = parseResource("sample_page.mcml")

        assertEquals("uidescription", doc.root?.tag)
        assertTrue(doc.namespaces.isNotEmpty())
        // sb -> http://schemas.microsoft.com/MediaCenter/ehshell/ServiceBus.UIFramework
        assertEquals(
            "http://schemas.microsoft.com/MediaCenter/ehshell/ServiceBus.UIFramework",
            doc.namespaces["sb"],
        )
    }

    @Test
    fun `collects nested panels with class attributes`() {
        val doc = parseResource("sample_page.mcml")
        val root = assertNotNullRoot(doc)

        val panels = root.allChildren.filter { it.tag.equals("Panel", ignoreCase = true) }.toList()
        assertTrue(panels.isNotEmpty())
        assertTrue(panels.all { it.attr("class") != null })

        val button = root.find("Button")
        assertNotNull(button)
        assertEquals("Button", button?.attr("class"))
        assertNotNull(button?.attr("onClick"))
    }

    @Test
    fun `button text content is captured`() {
        val doc = parseResource("sample_page.mcml")
        val button = doc.root?.find("Button")
        assertEquals("#Acquire.Copy", button?.text)
    }

    @Test
    fun `rect attribute parsing`() {
        assertEquals(McmlRect(0, 35, 0, 0), McmlRect.parse("Rect(0,35,0,0)"))
        assertEquals(McmlRect(-5, 0, 5, 10), McmlRect.parse("Rect(-5,0,5,10)"))
        assertNull(McmlRect.parse("Rect(0,35)"))
        assertNull(McmlRect.parse(null))
    }

    @Test
    fun `json conversion roundtrip fields`() {
        val doc = parseResource("sample_page.mcml")
        val json = McmlJson.documentToJson(doc)

        assertEquals("sample_page.mcml", json.getString("source"))
        val root = json.getJSONObject("root")
        assertEquals("uidescription", root.getString("tag"))
        assertTrue(root.has("children"))
        assertTrue(McmlJson.summarize(doc).contains("nodes="))
    }

    @Test
    fun `real corpus pages parse cleanly`() {
        // мини-корпус реальных страниц: имена файлов 1:1 из blue-full
        val corpus = listOf(
            "type23_ACQUIREWIZARD.XML.mcml",
            "type23_ASPECTPANEL.XML.mcml",
        )
        corpus.forEach { name ->
            val doc = parseResource(name)
            assertNotNull(doc.root)
            assertTrue(McmlJson.summarize(doc).isNotEmpty())
        }
    }
}
