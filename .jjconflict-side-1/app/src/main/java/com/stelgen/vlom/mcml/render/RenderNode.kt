package com.stelgen.vlom.mcml.render

import com.stelgen.vlom.mcml.McmlElement
import com.stelgen.vlom.mcml.McmlRect

/**
 * S1.1: типизированная рендер-модель поверх дерева MCML.
 *
 * Маппинг тегов на типы узла (по корпусу blue-full, 470 страниц):
 *   Panel/ColumnPanel/BackgroundPage -> Panel (контейнер)
 *   Text/Label/CustomTextControl     -> Text
 *   Image                            -> Image (src)
 *   Button                           -> Button (кликабельный контейнер с текстом)
 *   остальное                        -> Unknown (рисуем как Panel, класс сохраняем)
 *
 * Геометрия: left/top/width/height — целые в базисе дизайна (см. DesignScale).
 * Отступы: padding/margin = "Rect(l,t,r,b)".
 */
sealed class RenderNode {
    abstract val id: String?
    abstract val cls: String?
    abstract val geometry: Geometry
    abstract val children: List<RenderNode>

    data class Geometry(
        val left: Int? = null,
        val top: Int? = null,
        val width: Int? = null,
        val height: Int? = null,
        val margin: McmlRect? = null,
        val padding: McmlRect? = null,
    )

    data class Panel(
        override val id: String?,
        override val cls: String?,
        override val geometry: Geometry,
        override val children: List<RenderNode>,
        val layoutPos: String? = null,
    ) : RenderNode()

    data class Text(
        override val id: String?,
        override val cls: String?,
        override val geometry: Geometry,
        val value: String,           // "#Resource.Key" или литерал
    ) : RenderNode() {
        override val children: List<RenderNode> get() = emptyList()
    }

    data class Image(
        override val id: String?,
        override val cls: String?,
        override val geometry: Geometry,
        val source: String?,         // имя ресурса/PNG из assets
    ) : RenderNode() {
        override val children: List<RenderNode> get() = emptyList()
    }

    data class Button(
        override val id: String?,
        override val cls: String?,
        override val geometry: Geometry,
        override val children: List<RenderNode>,
        val onClick: String? = null,
        val label: String? = null,
    ) : RenderNode()
}

object RenderNodeBuilder {

    fun build(element: McmlElement): RenderNode {
        val geometry = RenderNode.Geometry(
            left = element.attr("left")?.toIntOrNull(),
            top = element.attr("top")?.toIntOrNull(),
            width = element.attr("width")?.toIntOrNull(),
            height = element.attr("height")?.toIntOrNull(),
            margin = McmlRect.parse(element.attr("margin")),
            padding = McmlRect.parse(element.attr("padding")),
        )
        val id = element.attr("id")
        val cls = element.attr("class")

        return when (element.tag.lowercase()) {
            "button" -> RenderNode.Button(
                id = id, cls = cls, geometry = geometry,
                children = element.children.map { build(it) },
                onClick = element.attr("onClick"),
                label = element.text,
            )
            "text", "label", "customtextcontrol" -> RenderNode.Text(
                id = id, cls = cls, geometry = geometry,
                value = element.text ?: "",
            )
            "image" -> RenderNode.Image(
                id = id, cls = cls, geometry = geometry,
                source = element.attr("src") ?: element.attr("source"),
            )
            "panel", "columnpanel", "backgroundpage", "horizontalpanel", "verticalpanel" ->
                RenderNode.Panel(
                    id = id, cls = cls, geometry = geometry,
                    children = element.children.map { build(it) },
                    layoutPos = element.attr("LayoutPos"),
                )
            else -> RenderNode.Panel(
                id = id, cls = cls, geometry = geometry,
                children = element.children.map { build(it) },
                layoutPos = element.attr("LayoutPos"),
            )
        }
    }

    fun buildDocument(root: McmlElement): RenderNode = build(root)
}
