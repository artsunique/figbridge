package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.FigmaNode
import com.artsunique.figbridge.config.CodeMode
import kotlin.math.roundToInt

object NodeParser {

    class ParseContext(val assetDir: String, val mode: CodeMode) {
        val assets = mutableListOf<AssetInfo>()
        private val usedNames = mutableMapOf<String, Int>()
        private val usedAssetNames = mutableMapOf<String, Int>()
        var bemBlock: String? = null

        /** Generate a unique BEM-style class name from a Figma layer name.
         *  Root node → block name. Children → block__element. */
        fun uniqueClassName(name: String): String {
            val slug = slugify(name)
            val base = if (bemBlock == null) {
                // First call = root node → becomes the block
                bemBlock = slug
                slug
            } else {
                // Child → block__element
                "${bemBlock}__$slug"
            }
            val count = usedNames.getOrDefault(base, 0)
            usedNames[base] = count + 1
            return if (count == 0) base else "$base-${count + 1}"
        }

        /** Generate a unique asset filename to avoid collisions (e.g. multiple "Vector" nodes) */
        fun uniqueAssetName(name: String, ext: String): String {
            val base = slugify(name)
            val count = usedAssetNames.getOrDefault(base, 0)
            usedAssetNames[base] = count + 1
            return if (count == 0) "$base.$ext" else "$base-${count + 1}.$ext"
        }
    }

    fun parse(node: FigmaNode, assetDir: String = "images", mode: CodeMode = CodeMode.TAILWIND): Pair<ParsedNode, List<AssetInfo>> {
        val ctx = ParseContext(assetDir, mode)

        if (!node.visible) {
            return when (mode) {
                CodeMode.TAILWIND -> ParsedNode(tag = "div", classes = listOf("hidden")) to emptyList()
                CodeMode.CUSTOM_CSS -> ParsedNode(tag = "div", classes = listOf("hidden"), cssProperties = mapOf("display" to "none")) to emptyList()
            }
        }

        val siblingFontSizes = collectFontSizes(node)
        val parsed = parseNode(node, siblingFontSizes, ctx, parentTag = null)
        return parsed to ctx.assets
    }

    /** Tags that are inline containers — text children should use span, not headings */
    private val INLINE_PARENT_TAGS = listOf("button", "a", "label")

    private fun parseNode(node: FigmaNode, siblingFontSizes: List<Float>, ctx: ParseContext, parentTag: String?): ParsedNode {
        var tag = SemanticDetector.detectTag(node, siblingFontSizes)

        // Text inside buttons/links → span (avoids h3 default margins bloating the element)
        if (node.type == "TEXT" && parentTag in INLINE_PARENT_TAGS && tag.startsWith("h")) {
            tag = "span"
        }

        return when (ctx.mode) {
            CodeMode.TAILWIND -> parseNodeTailwind(node, tag, siblingFontSizes, ctx)
            CodeMode.CUSTOM_CSS -> parseNodeCss(node, tag, siblingFontSizes, ctx)
        }
    }

    private fun parseNodeTailwind(node: FigmaNode, tag: String, siblingFontSizes: List<Float>, ctx: ParseContext): ParsedNode {
        // Text node
        if (node.type == "TEXT") {
            val classes = buildTailwindClasses(node, tag)
            return ParsedNode(tag = tag, classes = classes, text = node.characters ?: "")
        }

        // Vector/icon nodes → export as SVG
        if (node.type in VECTOR_TYPES) {
            val classes = buildTailwindClasses(node, "img")
            val fileName = ctx.uniqueAssetName(node.name, "svg")
            val relativePath = "${ctx.assetDir}/$fileName"
            ctx.assets += AssetInfo(node.id, fileName, "svg", relativePath)
            return ParsedNode(tag = "img", classes = classes, isSelfClosing = true, attributes = mapOf("alt" to node.name, "src" to "./$relativePath"))
        }

        // Icon component: frame/instance with only vector-like children → export as single SVG
        if (isIconComponent(node)) {
            val classes = buildTailwindClasses(node, "img")
            val fileName = ctx.uniqueAssetName(node.name, "svg")
            val relativePath = "${ctx.assetDir}/$fileName"
            ctx.assets += AssetInfo(node.id, fileName, "svg", relativePath)
            return ParsedNode(tag = "img", classes = classes, isSelfClosing = true, attributes = mapOf("alt" to node.name, "src" to "./$relativePath"))
        }

        // Image fill → export as PNG
        if (SemanticDetector.hasImageFill(node)) {
            val classes = buildTailwindClasses(node, "img")
            val fileName = ctx.uniqueAssetName(node.name, "png")
            val relativePath = "${ctx.assetDir}/$fileName"
            ctx.assets += AssetInfo(node.id, fileName, "png", relativePath)
            return ParsedNode(tag = "img", classes = classes, isSelfClosing = true, attributes = mapOf("alt" to node.name, "src" to "./$relativePath"))
        }

        // Guard: don't use self-closing tags for nodes with children
        val effectiveTag = if (node.children.isNotEmpty() && tag in listOf("img", "input")) "div" else tag

        // Self-closing input (only if no children)
        if (effectiveTag == "input") {
            val classes = buildTailwindClasses(node, effectiveTag)
            return ParsedNode(tag = "input", classes = classes, isSelfClosing = true, attributes = mapOf("type" to "text", "placeholder" to node.name))
        }

        // Img by name heuristic (only if no children)
        if (effectiveTag == "img") {
            val classes = buildTailwindClasses(node, "img")
            val fileName = ctx.uniqueAssetName(node.name, "png")
            val relativePath = "${ctx.assetDir}/$fileName"
            ctx.assets += AssetInfo(node.id, fileName, "png", relativePath)
            return ParsedNode(tag = "img", classes = classes, isSelfClosing = true, attributes = mapOf("alt" to node.name, "src" to "./$relativePath"))
        }

        val classes = buildTailwindClasses(node, effectiveTag).toMutableList()
        val absContainer = isAbsoluteContainer(node)

        // Add relative for absolute containers (non-auto-layout frames)
        if (absContainer && "relative" !in classes) {
            classes.add(0, "relative")
        }

        // Container node — recurse children
        val childFontSizes = collectFontSizes(node)
        val parentBounds = node.absoluteBoundingBox
        val children = node.children.filter { it.visible }.map { child ->
            var parsed = parseNode(child, childFontSizes, ctx, parentTag = effectiveTag)
            // Position children absolutely in non-auto-layout frames
            if (absContainer && parentBounds != null && child.absoluteBoundingBox != null) {
                val top = child.absoluteBoundingBox.y - parentBounds.y
                val left = child.absoluteBoundingBox.x - parentBounds.x
                val posClasses = mutableListOf("absolute")
                posClasses += "top-[${top.roundToInt()}px]"
                posClasses += "left-[${left.roundToInt()}px]"
                parsed = parsed.copy(classes = posClasses + parsed.classes)
            }
            parsed
        }
        return ParsedNode(tag = effectiveTag, classes = classes, children = children)
    }

    private fun parseNodeCss(node: FigmaNode, tag: String, siblingFontSizes: List<Float>, ctx: ParseContext): ParsedNode {
        val className = ctx.uniqueClassName(node.name)

        // Text node
        if (node.type == "TEXT") {
            val cssProps = buildCssProperties(node, tag)
            return ParsedNode(tag = tag, classes = listOf(className), text = node.characters ?: "", cssProperties = cssProps)
        }

        // Vector/icon nodes → export as SVG
        if (node.type in VECTOR_TYPES) {
            val cssProps = buildCssProperties(node, "img")
            val fileName = ctx.uniqueAssetName(node.name, "svg")
            val relativePath = "${ctx.assetDir}/$fileName"
            ctx.assets += AssetInfo(node.id, fileName, "svg", relativePath)
            return ParsedNode(tag = "img", classes = listOf(className), isSelfClosing = true, attributes = mapOf("alt" to node.name, "src" to "./$relativePath"), cssProperties = cssProps)
        }

        // Icon component: frame/instance with only vector-like children → export as single SVG
        if (isIconComponent(node)) {
            val cssProps = buildCssProperties(node, "img")
            val fileName = ctx.uniqueAssetName(node.name, "svg")
            val relativePath = "${ctx.assetDir}/$fileName"
            ctx.assets += AssetInfo(node.id, fileName, "svg", relativePath)
            return ParsedNode(tag = "img", classes = listOf(className), isSelfClosing = true, attributes = mapOf("alt" to node.name, "src" to "./$relativePath"), cssProperties = cssProps)
        }

        // Image fill → export as PNG
        if (SemanticDetector.hasImageFill(node)) {
            val cssProps = buildCssProperties(node, "img")
            val fileName = ctx.uniqueAssetName(node.name, "png")
            val relativePath = "${ctx.assetDir}/$fileName"
            ctx.assets += AssetInfo(node.id, fileName, "png", relativePath)
            return ParsedNode(tag = "img", classes = listOf(className), isSelfClosing = true, attributes = mapOf("alt" to node.name, "src" to "./$relativePath"), cssProperties = cssProps)
        }

        // Guard: don't use self-closing tags for nodes with children
        val effectiveTag = if (node.children.isNotEmpty() && tag in listOf("img", "input")) "div" else tag

        // Self-closing input (only if no children)
        if (effectiveTag == "input") {
            val cssProps = buildCssProperties(node, effectiveTag)
            return ParsedNode(tag = "input", classes = listOf(className), isSelfClosing = true, attributes = mapOf("type" to "text", "placeholder" to node.name), cssProperties = cssProps)
        }

        // Img by name heuristic (only if no children)
        if (effectiveTag == "img") {
            val cssProps = buildCssProperties(node, "img")
            val fileName = ctx.uniqueAssetName(node.name, "png")
            val relativePath = "${ctx.assetDir}/$fileName"
            ctx.assets += AssetInfo(node.id, fileName, "png", relativePath)
            return ParsedNode(tag = "img", classes = listOf(className), isSelfClosing = true, attributes = mapOf("alt" to node.name, "src" to "./$relativePath"), cssProperties = cssProps)
        }

        val cssProps = buildCssProperties(node, effectiveTag)
        val absContainer = isAbsoluteContainer(node)

        // Container — recurse
        val childFontSizes = collectFontSizes(node)
        val parentBounds = node.absoluteBoundingBox
        val children = node.children.filter { it.visible }.map { child ->
            var parsed = parseNode(child, childFontSizes, ctx, parentTag = effectiveTag)
            // Position children absolutely in non-auto-layout frames
            if (absContainer && parentBounds != null && child.absoluteBoundingBox != null) {
                val top = child.absoluteBoundingBox.y - parentBounds.y
                val left = child.absoluteBoundingBox.x - parentBounds.x
                val posProps = linkedMapOf<String, String>()
                posProps["position"] = "absolute"
                posProps["top"] = CssMapper.toRem(top)
                posProps["left"] = CssMapper.toRem(left)
                parsed = parsed.copy(cssProperties = posProps + parsed.cssProperties)
            }
            parsed
        }

        // Add relative if any child is absolute (auto layout or non-auto-layout)
        val needsRelative = absContainer || node.children.any { it.layoutPositioning == "ABSOLUTE" }
        val finalProps = if (needsRelative) {
            cssProps + ("position" to "relative")
        } else {
            cssProps
        }

        return ParsedNode(tag = effectiveTag, classes = listOf(className), children = children, cssProperties = finalProps)
    }

    private fun buildTailwindClasses(node: FigmaNode, tag: String): List<String> {
        val classes = mutableListOf<String>()

        // Relative positioning when children are absolute (auto layout case)
        if (node.children.any { it.layoutPositioning == "ABSOLUTE" }) {
            classes += "relative"
        }

        classes += TailwindMapper.mapLayout(node)
        classes += TailwindMapper.mapSize(node)
        classes += TailwindMapper.mapLayoutChild(node)
        classes += TailwindMapper.mapPadding(node)

        if (node.type == "TEXT") {
            classes += TailwindMapper.mapTypography(node)
        } else if (tag != "img") {
            classes += TailwindMapper.mapBackground(node)
        }

        classes += TailwindMapper.mapBorderRadius(node)
        classes += TailwindMapper.mapBorder(node)
        classes += TailwindMapper.mapEffects(node)
        classes += TailwindMapper.mapOpacity(node)
        classes += TailwindMapper.mapOverflow(node)
        return classes
    }

    private fun buildCssProperties(node: FigmaNode, tag: String): Map<String, String> {
        val props = linkedMapOf<String, String>()

        props += CssMapper.mapLayout(node)
        props += CssMapper.mapSize(node)
        props += CssMapper.mapLayoutChild(node)
        props += CssMapper.mapPadding(node)

        if (node.type == "TEXT") {
            props += CssMapper.mapTypography(node)
        } else if (tag != "img") {
            props += CssMapper.mapBackground(node)
        }

        props += CssMapper.mapBorderRadius(node)
        props += CssMapper.mapBorder(node)
        props += CssMapper.mapEffects(node)
        props += CssMapper.mapOpacity(node)
        props += CssMapper.mapOverflow(node)
        return props
    }

    private fun collectFontSizes(parent: FigmaNode): List<Float> {
        return parent.children
            .filter { it.type == "TEXT" && it.style != null }
            .map { it.style!!.fontSize }
    }

    private val VECTOR_TYPES = listOf("VECTOR", "BOOLEAN_OPERATION", "LINE", "ELLIPSE", "STAR", "REGULAR_POLYGON")
    private val CONTAINER_TYPES = listOf("FRAME", "COMPONENT", "INSTANCE", "COMPONENT_SET", "GROUP")

    /** Check if a node is vector-like (vector type, or GROUP containing only vectors) */
    private fun isVectorLike(node: FigmaNode): Boolean {
        if (node.type in VECTOR_TYPES) return true
        if (node.type == "GROUP") {
            val visibleChildren = node.children.filter { it.visible }
            return visibleChildren.isNotEmpty() && visibleChildren.all { isVectorLike(it) }
        }
        return false
    }

    /** Detect icon components: frames/instances where ALL visible children are vector-like.
     *  Excludes layout containers (which have auto layout and are structural, not icon-like). */
    private fun isIconComponent(node: FigmaNode): Boolean {
        if (node.type !in CONTAINER_TYPES) return false
        if (node.layoutMode != null) return false
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return false
        return visibleChildren.all { isVectorLike(it) }
    }

    /** Detect non-auto-layout frames where children need absolute positioning */
    private fun isAbsoluteContainer(node: FigmaNode): Boolean {
        if (node.layoutMode != null) return false
        if (node.type !in CONTAINER_TYPES) return false
        val visibleChildren = node.children.filter { it.visible }
        if (visibleChildren.isEmpty()) return false
        // Don't treat icon components as absolute containers (they export as single SVG)
        if (visibleChildren.all { isVectorLike(it) }) return false
        return true
    }
}

fun slugify(name: String): String {
    val slug = name.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifEmpty { "asset" }
    // Truncate long names (e.g. text content used as layer name) at word boundary
    if (slug.length <= 40) return slug
    val truncated = slug.substring(0, 40)
    val lastDash = truncated.lastIndexOf('-')
    return if (lastDash > 10) truncated.substring(0, lastDash) else truncated
}
