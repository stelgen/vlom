package com.stelgen.vlom.mcml

import org.json.JSONArray
import org.json.JSONObject

/**
 * MCML -> JSON-модель (S0.3). JSON — транспортный формат движка фазы 1:
 * парсер выдаёт дерево, JSON используется для отладки/диффа страниц/кэша.
 * org.json — встроен в Android; для JVM-тестов включён как testImplementation
 * через org.json:json (см. build.gradle.kts).
 */
object McmlJson {

    fun elementToJson(e: McmlElement): JSONObject = JSONObject().apply {
        put("tag", e.tag)
        e.namespace?.let { put("ns", it) }
        if (e.attributes.isNotEmpty()) {
            put("attrs", JSONObject(e.attributes))
        }
        e.text?.let { put("text", it) }
        if (e.children.isNotEmpty()) {
            put("children", JSONArray().also { arr ->
                e.children.forEach { arr.put(elementToJson(it)) }
            })
        }
    }

    fun documentToJson(d: McmlDocument): JSONObject = JSONObject().apply {
        put("source", d.sourceName)
        put("namespaces", JSONObject(d.namespaces))
        d.root?.let { put("root", elementToJson(it)) }
    }

    /** Компактный отчёт по документу — для лога загрузки страниц движком. */
    fun summarize(d: McmlDocument): String {
        val root = d.root ?: return "${d.sourceName}: EMPTY"
        val nodes = 1 + root.allChildren.count()
        val classes = root.allChildren.mapNotNull { it.attr("class") }.distinct().count()
        return "${d.sourceName}: ns=${d.namespaces.size} nodes=$nodes classes=$classes"
    }
}
