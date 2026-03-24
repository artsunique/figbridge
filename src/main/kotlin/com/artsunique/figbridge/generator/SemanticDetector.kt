package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.FigmaNode

object SemanticDetector {

    private val sectionKeywords = listOf("section", "block", "area", "region")
    private val headerKeywords = listOf("header", "topbar", "top-bar", "app-bar", "appbar")
    private val navKeywords = listOf("nav", "navbar", "navigation", "sidebar", "menu")
    private val footerKeywords = listOf("footer", "bottom-bar", "bottombar")
    private val mainKeywords = listOf("main", "content", "body")
    private val buttonKeywords = listOf("button", "btn", "cta")
    private val inputKeywords = listOf("input", "textfield", "text-field", "search")
    private val imageKeywords = listOf("image", "img", "photo", "picture", "thumbnail", "avatar", "icon")
    private val linkKeywords = listOf("link", "anchor")

    fun detectTag(node: FigmaNode, siblingFontSizes: List<Float> = emptyList()): String {
        val nameLower = node.name.lowercase().trim()

        // Text nodes
        if (node.type == "TEXT") {
            return detectTextTag(node, siblingFontSizes)
        }

        // Vector/Boolean → likely an icon or decorative element
        if (node.type in listOf("VECTOR", "BOOLEAN_OPERATION", "LINE", "ELLIPSE", "STAR", "REGULAR_POLYGON")) {
            return "div"
        }

        // Instance or Component with image fills → img
        if (hasImageFill(node)) {
            return "img"
        }

        // Name-based heuristics
        if (matchesAny(nameLower, headerKeywords)) return "header"
        if (matchesAny(nameLower, navKeywords)) return "nav"
        if (matchesAny(nameLower, footerKeywords)) return "footer"
        if (matchesAny(nameLower, mainKeywords)) return "main"
        if (matchesAny(nameLower, sectionKeywords)) return "section"
        if (matchesAny(nameLower, buttonKeywords)) return "button"
        if (matchesAny(nameLower, linkKeywords)) return "a"
        if (matchesAny(nameLower, inputKeywords)) return "input"
        if (matchesAny(nameLower, imageKeywords)) return "img"

        // Page-level canvases
        if (node.type == "CANVAS") return "div"

        return "div"
    }

    private fun detectTextTag(node: FigmaNode, siblingFontSizes: List<Float>): String {
        val fontSize = node.style?.fontSize ?: 16f
        val nameLower = node.name.lowercase()

        // Explicit heading names
        val headingMatch = Regex("^h([1-6])$").find(nameLower)
        if (headingMatch != null) return "h${headingMatch.groupValues[1]}"

        if (nameLower.contains("heading") || nameLower.contains("title")) {
            return inferHeadingLevel(fontSize, siblingFontSizes)
        }

        if (nameLower.contains("subtitle") || nameLower.contains("subhead")) return "h3"
        if (nameLower.contains("caption") || nameLower.contains("label")) return "span"
        if (nameLower.contains("paragraph") || nameLower.contains("body")) return "p"

        // Size-based: large text is likely a heading
        if (fontSize >= 32) return "h1"
        if (fontSize >= 24) return "h2"
        if (fontSize >= 20) return "h3"
        if (fontSize >= 14) return "p"

        return "span"
    }

    private fun inferHeadingLevel(fontSize: Float, siblingFontSizes: List<Float>): String {
        if (siblingFontSizes.isEmpty()) {
            return when {
                fontSize >= 32 -> "h1"
                fontSize >= 24 -> "h2"
                fontSize >= 20 -> "h3"
                fontSize >= 18 -> "h4"
                else -> "h5"
            }
        }
        val sorted = siblingFontSizes.distinct().sortedDescending()
        val rank = sorted.indexOf(fontSize)
        return "h${(rank + 1).coerceIn(1, 6)}"
    }

    fun hasImageFill(node: FigmaNode): Boolean {
        return node.fills.any { it.type == "IMAGE" && it.visible }
    }

    private fun matchesAny(name: String, keywords: List<String>): Boolean {
        return keywords.any { keyword ->
            name == keyword ||
                name.startsWith("$keyword-") || name.startsWith("${keyword}_") || name.startsWith("${keyword}/") ||
                name.endsWith("-$keyword") || name.endsWith("_$keyword") ||
                name.contains("-$keyword-") || name.contains("_${keyword}_") ||
                name.contains(" $keyword") || name.contains("$keyword ")
        }
    }
}
