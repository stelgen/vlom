package com.stelgen.vlom.mcml

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Парсер MCML (S0.3): InputStream -> [McmlDocument].
 *
 * Выбор javax DOM (а не XmlPullParser): работает и на JVM (unit-тесты), и на
 * Android без внешних зависимостей; 470 файлов небольшого размера — DOM ок.
 *
 * Особенности реальных страниц (проверено на blue-full):
 *  - префиксы произвольные (sb:, me:, ...) — важен URI namespace, не префикс;
 *  - корневые страницы <uidescription ...><backgroundpage url="..."/> ...;
 *  - атрибуты вида class="Page.FancyGalleryContent", padding="Rect(0,35,0,0)",
 *    строки "#Acquire.Title" (локализуемые) — сохраняются как есть.
 */
object McmlParser {

    fun newDocumentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // XXE-защита: MCML — наши собственные assets, но канон безопасности — выключить всегда
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isExpandEntityReferences = false
        }

    fun parse(sourceName: String, input: InputStream): McmlDocument {
        val dom: Document = newDocumentBuilderFactory().newDocumentBuilder().parse(input).also {
            it.documentElement.normalize()
        }
        val namespaces = collectNamespaces(dom.documentElement)
        val root = dom.documentElement.toMcml()
        return McmlDocument(sourceName, namespaces, root)
    }

    private fun collectNamespaces(root: Element): Map<String, String> {
        val map = mutableMapOf<String, String>()
        fun walk(e: Element) {
            val nsUri = e.namespaceURI
            if (nsUri != null) {
                val prefix = e.prefix ?: ""
                if (prefix !in map) map[prefix] = nsUri
            }
            var child: Element? = null
            val nodes = e.childNodes
            for (i in 0 until nodes.length) {
                val n = nodes.item(i)
                if (n is Element) {
                    walk(n)
                    if (child == null) child = n
                }
            }
        }
        walk(root)
        return map
    }

    private fun Element.toMcml(): McmlElement {
        val attrs = LinkedHashMap<String, String>()
        for (i in 0 until attributes.length) {
            val a = attributes.item(i)
            val key = if (a.prefix.isNullOrEmpty()) a.localName ?: a.nodeName
            else "${a.prefix}:${a.localName ?: a.nodeName}"
            attrs[key] = a.nodeValue
        }
        val sb = StringBuilder()
        val children = mutableListOf<McmlElement>()
        val nodes = childNodes
        for (i in 0 until nodes.length) {
            when (val n = nodes.item(i)) {
                is Element -> children += n.toMcml()
                is org.w3c.dom.Text -> sb.append(n.nodeValue)
            }
        }
        val text = sb.toString().trim().ifEmpty { null }
        return McmlElement(
            tag = localName ?: nodeName,
            namespace = namespaceURI,
            attributes = attrs,
            text = text,
            children = children,
        )
    }
}
