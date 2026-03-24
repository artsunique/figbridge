package com.artsunique.figbridge.generator

data class ParsedNode(
    val tag: String = "div",
    val classes: List<String> = emptyList(),
    val text: String? = null,
    val children: List<ParsedNode> = emptyList(),
    val isSelfClosing: Boolean = false,
    val attributes: Map<String, String> = emptyMap(),
    val cssProperties: Map<String, String> = emptyMap(),
) {
    val isTextOnly: Boolean get() = text != null && children.isEmpty()
}

data class AssetInfo(
    val nodeId: String,
    val fileName: String,
    val format: String, // "png" or "svg"
    val relativePath: String, // e.g. "images/hero-image.png"
)

data class GeneratedResult(
    val html: String,
    val css: String? = null,
    val fontsLink: String? = null,
    val assets: List<AssetInfo>,
)
