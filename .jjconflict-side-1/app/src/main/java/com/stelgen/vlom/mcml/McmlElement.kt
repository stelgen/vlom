package com.stelgen.vlom.mcml

/**
 * Узел MCML-разметки (фаза 0/S0.3).
 * MCML = XML от Microsoft (MediaCenter/ehshell namespaces). Модель максимально
 * общая: тег + namespace-URI + атрибуты + текст + дети. Семантика (Panel/Text/
 * Image/Clip/Rules) — слой рендера фазы 1, поверх этой модели.
 */
data class McmlElement(
    val tag: String,
    val namespace: String?,
    val attributes: Map<String, String>,
    val text: String?,
    val children: List<McmlElement>,
) {
    val allChildren: Sequence<McmlElement>
        get() = children.asSequence() + children.asSequence().flatMap { it.allChildren }

    /** Первый ребёнок (в любом уровне) с данным тегом (регистр не важен). */
    fun find(tagName: String): McmlElement? =
        allChildren.firstOrNull { it.tag.equals(tagName, ignoreCase = true) }

    fun attr(name: String): String? = attributes[name] ?: attributes[name.lowercase()]
}

/**
 * Разобранный MCML-документ: корень + карта пространств имён.
 */
data class McmlDocument(
    val sourceName: String,
    val namespaces: Map<String, String>, // prefix -> URI
    val root: McmlElement?,
)
