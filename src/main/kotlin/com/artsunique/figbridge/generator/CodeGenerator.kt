package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.FigmaNode
import com.artsunique.figbridge.api.FigmaStyle
import com.artsunique.figbridge.api.FigmaVariable
import com.artsunique.figbridge.config.CodeMode

object CodeGenerator {

    fun generate(
        node: FigmaNode,
        assetDir: String = "images",
        variables: Map<String, FigmaVariable> = emptyMap(),
        fileStyles: Map<String, FigmaStyle> = emptyMap(),
        mode: CodeMode = CodeMode.TAILWIND,
    ): GeneratedResult {
        TailwindMapper.variables = variables
        TailwindMapper.fileStyles = fileStyles
        CssMapper.variables = variables
        CssMapper.fileStyles = fileStyles

        val (parsed, assets) = NodeParser.parse(node, assetDir, mode)
        val html = renderHtml(parsed, 0)

        val css = if (mode == CodeMode.CUSTOM_CSS) {
            collectCss(parsed)
        } else {
            null
        }

        val fontsLink = buildGoogleFontsLink(collectFonts(node))

        return GeneratedResult(html, css, fontsLink, assets)
    }

    fun generateTailwind(node: FigmaNode): String {
        return generate(node).html
    }

    fun generateCss(node: FigmaNode): GeneratedResult {
        return generate(node, mode = CodeMode.CUSTOM_CSS)
    }

    private fun renderHtml(node: ParsedNode, indent: Int): String {
        val sb = StringBuilder()
        val pad = "  ".repeat(indent)
        val classAttr = if (node.classes.isNotEmpty()) " class=\"${node.classes.joinToString(" ")}\"" else ""
        val extraAttrs = node.attributes.entries.joinToString("") { " ${it.key}=\"${it.value}\"" }

        if (node.isSelfClosing) {
            sb.append("$pad<${node.tag}$classAttr$extraAttrs />\n")
            return sb.toString()
        }

        if (node.isTextOnly) {
            val text = escapeHtml(node.text ?: "")
            if (text.length < 80 && !text.contains('\n')) {
                sb.append("$pad<${node.tag}$classAttr>$text</${node.tag}>\n")
            } else {
                sb.append("$pad<${node.tag}$classAttr>\n")
                for (line in text.lines()) {
                    sb.append("$pad  ${line.trim()}\n")
                }
                sb.append("$pad</${node.tag}>\n")
            }
            return sb.toString()
        }

        sb.append("$pad<${node.tag}$classAttr$extraAttrs>\n")
        for (child in node.children) {
            sb.append(renderHtml(child, indent + 1))
        }
        sb.append("$pad</${node.tag}>\n")

        return sb.toString()
    }

    /** Collect all CSS rules from the ParsedNode tree */
    private fun collectCss(node: ParsedNode): String {
        val rules = mutableListOf<String>()
        collectCssRules(node, rules)
        return rules.joinToString("\n\n")
    }

    private fun collectCssRules(node: ParsedNode, rules: MutableList<String>) {
        if (node.cssProperties.isNotEmpty() && node.classes.isNotEmpty()) {
            val selector = ".${node.classes.first()}"
            val declarations = node.cssProperties.entries.joinToString("\n") { (prop, value) ->
                "  $prop: $value;"
            }
            rules += "$selector {\n$declarations\n}"
        }
        for (child in node.children) {
            collectCssRules(child, rules)
        }
    }

    // --- Google Fonts ---

    private val SYSTEM_FONTS = setOf(
        "system-ui", "ui-sans-serif", "ui-serif", "ui-monospace",
        "arial", "helvetica", "times new roman", "courier new",
        "georgia", "verdana", "trebuchet ms", "tahoma",
        "segoe ui", "san francisco", "sf pro",
        "inter", // bundled with many frameworks
    )

    /** Collect all font families + weights from the node tree */
    private fun collectFonts(node: FigmaNode): Map<String, Set<Int>> {
        val fonts = mutableMapOf<String, MutableSet<Int>>()
        collectFontsRecursive(node, fonts)
        return fonts
    }

    private fun collectFontsRecursive(node: FigmaNode, fonts: MutableMap<String, MutableSet<Int>>) {
        if (node.type == "TEXT" && node.style != null) {
            val family = node.style.fontFamily
            if (family.isNotBlank() && family.lowercase() !in SYSTEM_FONTS) {
                fonts.getOrPut(family) { mutableSetOf() }.add(node.style.fontWeight)
            }
        }
        for (child in node.children) {
            collectFontsRecursive(child, fonts)
        }
    }

    /** Build a Google Fonts <link> tag, or null if no custom fonts */
    private fun buildGoogleFontsLink(fonts: Map<String, Set<Int>>): String? {
        if (fonts.isEmpty()) return null
        val params = fonts.entries.sortedBy { it.key }.joinToString("&") { (family, weights) ->
            val sortedWeights = weights.sorted().joinToString(";")
            "family=${family.replace(" ", "+")}:wght@$sortedWeights"
        }
        return """<link rel="stylesheet" href="https://fonts.googleapis.com/css2?$params&display=swap" />"""
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
