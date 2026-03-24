package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.Color
import com.artsunique.figbridge.api.FigmaNode
import com.artsunique.figbridge.api.FigmaStyle
import com.artsunique.figbridge.api.FigmaVariable
import com.artsunique.figbridge.api.Paint
import kotlin.math.roundToInt

/**
 * Maps Figma node properties to Tailwind CSS v4 utility classes.
 * Uses arbitrary values [value] when no standard class matches.
 */
object TailwindMapper {

    fun mapLayout(node: FigmaNode): List<String> {
        val classes = mutableListOf<String>()

        when (node.layoutMode) {
            "HORIZONTAL" -> {
                classes += "flex"
                classes += "flex-row"
            }
            "VERTICAL" -> {
                classes += "flex"
                classes += "flex-col"
            }
        }

        // Wrap
        if (node.layoutWrap == "WRAP") {
            classes += "flex-wrap"
        }

        // Gap / item spacing
        if (node.counterAxisSpacing != null && node.counterAxisSpacing > 0 && node.layoutWrap == "WRAP") {
            // Different gap per axis when wrapping
            node.itemSpacing?.let { gap ->
                if (gap > 0) classes += mapSpacing("gap-x", gap)
            }
            classes += mapSpacing("gap-y", node.counterAxisSpacing)
        } else {
            node.itemSpacing?.let { gap ->
                if (gap > 0) classes += mapSpacing("gap", gap)
            }
        }

        // Content alignment (multi-line / wrapped)
        if (node.layoutWrap == "WRAP" && node.counterAxisAlignContent != null) {
            classes += when (node.counterAxisAlignContent) {
                "SPACE_BETWEEN" -> "content-between"
                else -> "" // AUTO = default
            }
        }

        // Alignment
        if (node.layoutMode != null) {
            node.primaryAxisAlignItems?.let { align ->
                classes += when (align) {
                    "CENTER" -> "justify-center"
                    "MAX" -> "justify-end"
                    "SPACE_BETWEEN" -> "justify-between"
                    "SPACE_AROUND" -> "justify-around"
                    "SPACE_EVENLY" -> "justify-evenly"
                    else -> "" // MIN = default
                }
            }
            node.counterAxisAlignItems?.let { align ->
                classes += when (align) {
                    "CENTER" -> "items-center"
                    "MAX" -> "items-end"
                    "BASELINE" -> "items-baseline"
                    else -> "" // MIN = default
                }
            }
        }

        return classes.filter { it.isNotBlank() }
    }

    fun mapPadding(node: FigmaNode): List<String> {
        val classes = mutableListOf<String>()
        val t = node.paddingTop ?: 0f
        val b = node.paddingBottom ?: 0f
        val l = node.paddingLeft ?: 0f
        val r = node.paddingRight ?: 0f

        if (t == 0f && b == 0f && l == 0f && r == 0f) return emptyList()

        if (t == b && l == r && t == l) {
            // Uniform padding
            classes += mapSpacing("p", t)
        } else if (t == b && l == r) {
            // Symmetric padding
            classes += mapSpacing("px", l)
            classes += mapSpacing("py", t)
        } else {
            if (t > 0) classes += mapSpacing("pt", t)
            if (r > 0) classes += mapSpacing("pr", r)
            if (b > 0) classes += mapSpacing("pb", b)
            if (l > 0) classes += mapSpacing("pl", l)
        }

        return classes
    }

    fun mapSize(node: FigmaNode): List<String> {
        val classes = mutableListOf<String>()
        val hasMaxW = node.maxWidth != null && node.maxWidth > 0
        val hasMaxH = node.maxHeight != null && node.maxHeight > 0

        // Width
        when (node.layoutSizingHorizontal) {
            "FILL" -> classes += "w-full"
            "HUG" -> {} // hug = intrinsic, no class needed
            "FIXED" -> {
                if (!hasMaxW) {
                    val w = node.absoluteBoundingBox?.width?.roundToInt()
                    if (w != null && w > 0) classes += mapDimension("w", w)
                }
            }
            else -> {
                // No layoutSizing = standalone frame (not in auto layout parent)
                // Use absoluteBoundingBox as fixed width
                if (!hasMaxW) {
                    val w = node.absoluteBoundingBox?.width?.roundToInt()
                    if (w != null && w > 0) classes += mapDimension("w", w)
                }
            }
        }

        // Height
        when (node.layoutSizingVertical) {
            "FILL" -> classes += "h-full"
            "HUG" -> {} // hug = intrinsic, no class needed
            "FIXED" -> {
                if (!hasMaxH) {
                    val h = node.absoluteBoundingBox?.height?.roundToInt()
                    if (h != null && h > 0) classes += mapDimension("h", h)
                }
            }
            else -> {
                if (!hasMaxH) {
                    val h = node.absoluteBoundingBox?.height?.roundToInt()
                    if (h != null && h > 0) classes += mapDimension("h", h)
                }
            }
        }

        // Min/Max constraints — these express the designer's intent
        node.minWidth?.let { if (it > 0) classes += mapDimension("min-w", it.roundToInt()) }
        node.maxWidth?.let { if (it > 0) classes += mapDimension("max-w", it.roundToInt()) }
        node.minHeight?.let { if (it > 0) classes += mapDimension("min-h", it.roundToInt()) }
        node.maxHeight?.let { if (it > 0) classes += mapDimension("max-h", it.roundToInt()) }

        return classes
    }

    fun mapLayoutChild(node: FigmaNode): List<String> {
        val classes = mutableListOf<String>()

        // Absolute positioning within auto layout
        if (node.layoutPositioning == "ABSOLUTE") {
            classes += "absolute"
        }

        // flex-grow with basis-0 for equal distribution
        if (node.layoutGrow != null && node.layoutGrow > 0) {
            classes += "grow"
            classes += "basis-0"
        }

        // self-stretch (cross-axis alignment)
        if (node.layoutAlign == "STRETCH") {
            classes += "self-stretch"
        }

        return classes
    }

    /**
     * Maps any dimension (w, h, min-w, max-w, min-h, max-h) to Tailwind class.
     * Checks named container sizes first, then spacing scale, then arbitrary.
     */
    private fun mapDimension(prefix: String, px: Int): String {
        // Named container / breakpoint sizes (Tailwind v4)
        val named = NAMED_SIZES[px]
        if (named != null) return "$prefix-$named"

        // Spacing scale (0–384px)
        return mapSpacing(prefix, px.toFloat())
    }

    /** Tailwind v4 named size tokens: container widths, breakpoints, prose */
    private val NAMED_SIZES = mapOf(
        // Container / max-w named sizes
        320 to "xs",      // 20rem
        384 to "sm",      // 24rem
        448 to "md",      // 28rem
        512 to "lg",      // 32rem
        576 to "xl",      // 36rem
        672 to "2xl",     // 42rem
        768 to "3xl",     // 48rem
        896 to "4xl",     // 56rem
        1024 to "5xl",    // 64rem
        1152 to "6xl",    // 72rem
        1280 to "7xl",    // 80rem
        // Screen widths
        640 to "screen-sm",
        1536 to "screen-2xl",
        // Prose
        // 65ch ≈ 780px, not exact — skip
    )

    /** Variables map: variable ID → FigmaVariable (name, resolvedType) */
    var variables: Map<String, FigmaVariable> = emptyMap()
    /** File-level styles map: style ID → FigmaStyle (name, styleType) */
    var fileStyles: Map<String, FigmaStyle> = emptyMap()

    fun mapBackground(node: FigmaNode): List<String> {
        val fill = node.fills.firstOrNull { it.visible && it.type == "SOLID" && it.color != null }
            ?: return emptyList()

        // Check node's fill style reference first
        val styleName = resolveStyleName(node, "fill")
        if (styleName != null) return listOf("bg-${variableToTailwind(styleName)}")

        return listOf("bg-${resolveColor(fill)}")
    }

    fun mapTypography(node: FigmaNode): List<String> {
        val style = node.style ?: return emptyList()
        val classes = mutableListOf<String>()

        // Font family (arbitrary value if not a standard Tailwind font)
        if (style.fontFamily.isNotBlank()) {
            classes += when (style.fontFamily.lowercase()) {
                "inter", "system-ui", "ui-sans-serif" -> "font-sans"
                "ui-serif", "georgia", "times new roman" -> "font-serif"
                "ui-monospace", "jetbrains mono", "fira code" -> "font-mono"
                else -> "font-[${style.fontFamily.replace(" ", "_")}]"
            }
        }

        // Font size
        classes += mapFontSize(style.fontSize)

        // Font weight
        classes += mapFontWeight(style.fontWeight)

        // Line height
        style.lineHeightPx?.let { lh ->
            classes += mapLineHeight(lh, style.fontSize)
        }

        // Letter spacing
        style.letterSpacing?.let { ls ->
            if (ls != 0f) {
                classes += "tracking-[${formatFloat(ls)}px]"
            }
        }

        // Text alignment
        style.textAlignHorizontal?.let { align ->
            classes += when (align) {
                "CENTER" -> "text-center"
                "RIGHT" -> "text-right"
                "JUSTIFIED" -> "text-justify"
                else -> "" // LEFT = default
            }
        }

        // Text color (from character fills)
        val textStyleName = resolveStyleName(node, "text") ?: resolveStyleName(node, "fill")
        val textFill = node.fills.firstOrNull { it.visible && it.type == "SOLID" && it.color != null }
        if (textStyleName != null) {
            classes += "text-${variableToTailwind(textStyleName)}"
        } else if (textFill != null) {
            classes += "text-${resolveColor(textFill)}"
        }

        return classes.filter { it.isNotBlank() }
    }

    fun mapBorderRadius(node: FigmaNode): List<String> {
        val radius = node.cornerRadius ?: return emptyList()
        if (radius <= 0) return emptyList()

        return listOf(mapRadius(radius))
    }

    fun mapBorder(node: FigmaNode): List<String> {
        val stroke = node.strokes.firstOrNull { it.visible && it.type == "SOLID" && it.color != null }
            ?: return emptyList()
        val classes = mutableListOf<String>()

        val weight = node.strokeWeight ?: 1f
        classes += when (weight.roundToInt()) {
            0 -> "border-0"
            1 -> "border"
            2 -> "border-2"
            4 -> "border-4"
            8 -> "border-8"
            else -> "border-[${weight.roundToInt()}px]"
        }

        val strokeStyleName = resolveStyleName(node, "stroke")
        if (strokeStyleName != null) {
            classes += "border-${variableToTailwind(strokeStyleName)}"
        } else {
            classes += "border-${resolveColor(stroke)}"
        }

        return classes
    }

    fun mapEffects(node: FigmaNode): List<String> {
        val classes = mutableListOf<String>()

        for (effect in node.effects) {
            if (!effect.visible) continue
            when (effect.type) {
                "DROP_SHADOW" -> {
                    val radius = effect.radius ?: 0f
                    classes += when {
                        radius <= 1 -> "shadow-sm"
                        radius <= 3 -> "shadow"
                        radius <= 6 -> "shadow-md"
                        radius <= 10 -> "shadow-lg"
                        radius <= 15 -> "shadow-xl"
                        else -> "shadow-2xl"
                    }
                }
                "INNER_SHADOW" -> classes += "shadow-inner"
                "LAYER_BLUR" -> {
                    val radius = effect.radius ?: 0f
                    classes += when {
                        radius <= 4 -> "blur-sm"
                        radius <= 8 -> "blur"
                        radius <= 12 -> "blur-md"
                        radius <= 16 -> "blur-lg"
                        else -> "blur-xl"
                    }
                }
                "BACKGROUND_BLUR" -> classes += "backdrop-blur"
            }
        }

        return classes
    }

    fun mapOpacity(node: FigmaNode): List<String> {
        val opacity = node.opacity ?: return emptyList()
        if (opacity >= 1f) return emptyList()

        val pct = (opacity * 100).roundToInt()
        return listOf(
            when (pct) {
                0 -> "opacity-0"
                5 -> "opacity-5"
                10 -> "opacity-10"
                15 -> "opacity-15"
                20 -> "opacity-20"
                25 -> "opacity-25"
                30 -> "opacity-30"
                35 -> "opacity-35"
                40 -> "opacity-40"
                45 -> "opacity-45"
                50 -> "opacity-50"
                55 -> "opacity-55"
                60 -> "opacity-60"
                65 -> "opacity-65"
                70 -> "opacity-70"
                75 -> "opacity-75"
                80 -> "opacity-80"
                85 -> "opacity-85"
                90 -> "opacity-90"
                95 -> "opacity-95"
                100 -> "opacity-100"
                else -> "opacity-[$pct%]"
            }
        )
    }

    fun mapOverflow(node: FigmaNode): List<String> {
        if (node.clipsContent == true) return listOf("overflow-hidden")
        return emptyList()
    }

    // --- Color resolution ---

    /**
     * Resolves a paint to a Tailwind color value.
     * Priority: 1) bound variable, 2) Tailwind palette match, 3) hex fallback.
     */
    fun resolveColor(paint: Paint): String {
        // 1. Check for bound variable (Enterprise only)
        val varId = paint.boundVariables?.get("color")?.id
        if (varId != null) {
            val variable = variables[varId]
            if (variable != null) {
                return variableToTailwind(variable.name)
            }
        }

        val color = paint.color ?: return "[#000000]"
        val hex = colorToHex(color, paint.opacity)

        // 2. Try exact match in Tailwind default palette
        if (!hex.contains(Regex("[0-9a-f]{8}"))) { // Only match non-alpha colors
            val twName = TailwindColors.lookup(hex)
            if (twName != null) return twName
        }

        // 3. Fallback to arbitrary hex
        return "[$hex]"
    }

    /** Look up a Figma style name from the node's styles map */
    private fun resolveStyleName(node: FigmaNode, styleKey: String): String? {
        val styleId = node.styles?.get(styleKey) ?: return null
        return fileStyles[styleId]?.name
    }

    /**
     * Converts a Figma variable name to a Tailwind color class.
     * Examples:
     *   "slate/300"           → "slate-300"
     *   "colors/slate/300"    → "slate-300"
     *   "primitives/blue/500" → "blue-500"
     *   "white"               → "white"
     *   "primary"             → "primary"
     */
    fun variableToTailwind(name: String): String {
        val parts = name.lowercase().split("/")
        // Strip common prefixes like "colors", "primitives", "tokens", "color"
        val prefixes = setOf("colors", "primitives", "tokens", "color", "semantic", "theme")
        val meaningful = parts.dropWhile { it in prefixes }
        return if (meaningful.isEmpty()) {
            parts.last().replace(" ", "-")
        } else {
            meaningful.joinToString("-") { it.replace(" ", "-") }
        }
    }

    // --- Helpers ---

    private fun mapSpacing(prefix: String, value: Float): String {
        val px = value.roundToInt()
        return when (px) {
            0 -> "$prefix-0"
            1 -> "$prefix-px"
            2 -> "$prefix-0.5"
            4 -> "$prefix-1"
            6 -> "$prefix-1.5"
            8 -> "$prefix-2"
            10 -> "$prefix-2.5"
            12 -> "$prefix-3"
            14 -> "$prefix-3.5"
            16 -> "$prefix-4"
            20 -> "$prefix-5"
            24 -> "$prefix-6"
            28 -> "$prefix-7"
            32 -> "$prefix-8"
            36 -> "$prefix-9"
            40 -> "$prefix-10"
            44 -> "$prefix-11"
            48 -> "$prefix-12"
            56 -> "$prefix-14"
            64 -> "$prefix-16"
            80 -> "$prefix-20"
            96 -> "$prefix-24"
            112 -> "$prefix-28"
            128 -> "$prefix-32"
            144 -> "$prefix-36"
            160 -> "$prefix-40"
            176 -> "$prefix-44"
            192 -> "$prefix-48"
            208 -> "$prefix-52"
            224 -> "$prefix-56"
            240 -> "$prefix-60"
            256 -> "$prefix-64"
            288 -> "$prefix-72"
            320 -> "$prefix-80"
            384 -> "$prefix-96"
            else -> "$prefix-[${px}px]"
        }
    }

    private fun mapFontSize(size: Float): String {
        val px = size.roundToInt()
        return when (px) {
            12 -> "text-xs"
            14 -> "text-sm"
            16 -> "text-base"
            18 -> "text-lg"
            20 -> "text-xl"
            24 -> "text-2xl"
            30 -> "text-3xl"
            36 -> "text-4xl"
            48 -> "text-5xl"
            60 -> "text-6xl"
            72 -> "text-7xl"
            96 -> "text-8xl"
            128 -> "text-9xl"
            else -> "text-[${px}px]"
        }
    }

    private fun mapFontWeight(weight: Int): String {
        return when (weight) {
            100 -> "font-thin"
            200 -> "font-extralight"
            300 -> "font-light"
            400 -> "" // normal = default
            500 -> "font-medium"
            600 -> "font-semibold"
            700 -> "font-bold"
            800 -> "font-extrabold"
            900 -> "font-black"
            else -> "font-[$weight]"
        }
    }

    private fun mapLineHeight(lineHeightPx: Float, fontSize: Float): String {
        val ratio = lineHeightPx / fontSize
        return when {
            ratio <= 1.0 -> "leading-none"
            ratio <= 1.15 -> "leading-tight"
            ratio <= 1.35 -> "leading-snug"
            ratio <= 1.5 -> "leading-normal"
            ratio <= 1.65 -> "leading-relaxed"
            ratio <= 2.0 -> "leading-loose"
            else -> "leading-[${formatFloat(lineHeightPx)}px]"
        }
    }

    private fun mapRadius(radius: Float): String {
        val px = radius.roundToInt()
        return when (px) {
            0 -> "rounded-none"
            2 -> "rounded-sm"
            4 -> "rounded"
            6 -> "rounded-md"
            8 -> "rounded-lg"
            12 -> "rounded-xl"
            16 -> "rounded-2xl"
            24 -> "rounded-3xl"
            in 9999..Int.MAX_VALUE -> "rounded-full"
            else -> "rounded-[${px}px]"
        }
    }

    fun colorToHex(color: Color, opacity: Float = 1f): String {
        val r = (color.r * 255).roundToInt().coerceIn(0, 255)
        val g = (color.g * 255).roundToInt().coerceIn(0, 255)
        val b = (color.b * 255).roundToInt().coerceIn(0, 255)
        val a = (color.a * opacity * 255).roundToInt().coerceIn(0, 255)

        return if (a < 255) {
            "#%02x%02x%02x%02x".format(r, g, b, a)
        } else {
            "#%02x%02x%02x".format(r, g, b)
        }
    }

    private fun formatFloat(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            "%.1f".format(value)
        }
    }
}
