package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.FigmaNode
import com.artsunique.figbridge.api.FigmaStyle
import com.artsunique.figbridge.api.FigmaVariable
import com.artsunique.figbridge.api.Paint
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Maps Figma node properties to vanilla CSS property declarations.
 * Values use rem (px / 16) for sizing, hex for colors.
 */
object CssMapper {

    var variables: Map<String, FigmaVariable> = emptyMap()
    var fileStyles: Map<String, FigmaStyle> = emptyMap()

    fun mapLayout(node: FigmaNode): Map<String, String> {
        val props = mutableMapOf<String, String>()

        // Use inline-flex for children that grow (flex: 1 1 0) to match Figma rendering
        val isGrowChild = node.layoutGrow != null && node.layoutGrow > 0

        when (node.layoutMode) {
            "HORIZONTAL" -> {
                props["display"] = if (isGrowChild) "inline-flex" else "flex"
                props["flex-direction"] = "row"
            }
            "VERTICAL" -> {
                props["display"] = if (isGrowChild) "inline-flex" else "flex"
                props["flex-direction"] = "column"
            }
        }

        if (node.layoutWrap == "WRAP") {
            props["flex-wrap"] = "wrap"
        }

        // Gap
        if (node.counterAxisSpacing != null && node.counterAxisSpacing > 0 && node.layoutWrap == "WRAP") {
            node.itemSpacing?.let { if (it > 0) props["column-gap"] = toRem(it) }
            props["row-gap"] = toRem(node.counterAxisSpacing)
        } else {
            node.itemSpacing?.let { if (it > 0) props["gap"] = toRem(it) }
        }

        // Content alignment (wrapped)
        if (node.layoutWrap == "WRAP") {
            when (node.counterAxisAlignContent) {
                "SPACE_BETWEEN" -> props["align-content"] = "space-between"
                "AUTO" -> props["align-content"] = "flex-start"
            }
        }

        // Alignment
        if (node.layoutMode != null) {
            node.primaryAxisAlignItems?.let { align ->
                val value = when (align) {
                    "CENTER" -> "center"
                    "MAX" -> "flex-end"
                    "SPACE_BETWEEN" -> "space-between"
                    "SPACE_AROUND" -> "space-around"
                    "SPACE_EVENLY" -> "space-evenly"
                    else -> null
                }
                if (value != null) props["justify-content"] = value
            }
            node.counterAxisAlignItems?.let { align ->
                val value = when (align) {
                    "CENTER" -> "center"
                    "MAX" -> "flex-end"
                    "BASELINE" -> "baseline"
                    else -> null
                }
                if (value != null) props["align-items"] = value
            }
        }

        return props
    }

    fun mapPadding(node: FigmaNode): Map<String, String> {
        val t = node.paddingTop ?: 0f
        val b = node.paddingBottom ?: 0f
        val l = node.paddingLeft ?: 0f
        val r = node.paddingRight ?: 0f

        if (t == 0f && b == 0f && l == 0f && r == 0f) return emptyMap()

        return if (t == b && l == r && t == l) {
            mapOf("padding" to toRem(t))
        } else if (t == b && l == r) {
            mapOf("padding" to "${toRem(t)} ${toRem(l)}")
        } else {
            mapOf("padding" to "${toRem(t)} ${toRem(r)} ${toRem(b)} ${toRem(l)}")
        }
    }

    fun mapSize(node: FigmaNode): Map<String, String> {
        val props = mutableMapOf<String, String>()
        val hasMaxW = node.maxWidth != null && node.maxWidth > 0
        val hasMaxH = node.maxHeight != null && node.maxHeight > 0

        when (node.layoutSizingHorizontal) {
            "FILL" -> props["width"] = "100%"
            "HUG" -> {} // intrinsic
            "FIXED" -> {
                if (!hasMaxW) {
                    val w = node.absoluteBoundingBox?.width
                    if (w != null && w > 0) props["width"] = toRem(w)
                }
            }
            else -> {
                if (!hasMaxW) {
                    val w = node.absoluteBoundingBox?.width
                    if (w != null && w > 0) props["width"] = toRem(w)
                }
            }
        }

        when (node.layoutSizingVertical) {
            "FILL" -> props["height"] = "100%"
            "HUG" -> {}
            "FIXED" -> {
                if (!hasMaxH) {
                    val h = node.absoluteBoundingBox?.height
                    if (h != null && h > 0) props["height"] = toRem(h)
                }
            }
            else -> {
                if (!hasMaxH) {
                    val h = node.absoluteBoundingBox?.height
                    if (h != null && h > 0) props["height"] = toRem(h)
                }
            }
        }

        node.minWidth?.let { if (it > 0) props["min-width"] = toRem(it) }
        node.maxWidth?.let { if (it > 0) props["max-width"] = toRem(it) }
        node.minHeight?.let { if (it > 0) props["min-height"] = toRem(it) }
        node.maxHeight?.let { if (it > 0) props["max-height"] = toRem(it) }

        return props
    }

    fun mapLayoutChild(node: FigmaNode): Map<String, String> {
        val props = mutableMapOf<String, String>()

        if (node.layoutPositioning == "ABSOLUTE") {
            props["position"] = "absolute"
        }

        if (node.layoutGrow != null && node.layoutGrow > 0) {
            props["flex"] = "1 1 0"
        }

        if (node.layoutAlign == "STRETCH") {
            props["align-self"] = "stretch"
        }

        return props
    }

    fun mapBackground(node: FigmaNode): Map<String, String> {
        val fill = node.fills.firstOrNull { it.visible && it.type == "SOLID" && it.color != null }
            ?: return emptyMap()

        val styleName = resolveStyleName(node, "fill")
        val hex = if (styleName != null) {
            // Use style name as CSS custom property reference
            resolveColorHex(fill)
        } else {
            resolveColorHex(fill)
        }

        return mapOf("background-color" to hex)
    }

    fun mapTypography(node: FigmaNode): Map<String, String> {
        val style = node.style ?: return emptyMap()
        val props = mutableMapOf<String, String>()

        // Font family
        if (style.fontFamily.isNotBlank()) {
            props["font-family"] = style.fontFamily
        }

        props["font-size"] = toRem(style.fontSize)

        if (style.fontWeight != 400) {
            props["font-weight"] = style.fontWeight.toString()
        }

        style.lineHeightPx?.let { lh ->
            props["line-height"] = String.format(Locale.US, "%.2f", lh / style.fontSize).trimEnd('0').trimEnd('.')
        }

        style.letterSpacing?.let { ls ->
            if (ls != 0f) props["letter-spacing"] = "${formatFloat(ls)}px"
        }

        style.textAlignHorizontal?.let { align ->
            val value = when (align) {
                "CENTER" -> "center"
                "RIGHT" -> "right"
                "JUSTIFIED" -> "justify"
                else -> null
            }
            if (value != null) props["text-align"] = value
        }

        // Text color
        val textFill = node.fills.firstOrNull { it.visible && it.type == "SOLID" && it.color != null }
        if (textFill != null) {
            props["color"] = resolveColorHex(textFill)
        }

        // Prevent text overflow
        props["word-wrap"] = "break-word"

        return props
    }

    fun mapBorderRadius(node: FigmaNode): Map<String, String> {
        val radius = node.cornerRadius ?: return emptyMap()
        if (radius <= 0) return emptyMap()
        return mapOf("border-radius" to toRem(radius))
    }

    fun mapBorder(node: FigmaNode): Map<String, String> {
        val stroke = node.strokes.firstOrNull { it.visible && it.type == "SOLID" && it.color != null }
            ?: return emptyMap()
        val weight = node.strokeWeight ?: 1f
        val hex = resolveColorHex(stroke)
        return mapOf("border" to "${weight.roundToInt()}px solid $hex")
    }

    fun mapEffects(node: FigmaNode): Map<String, String> {
        val props = mutableMapOf<String, String>()
        val shadows = mutableListOf<String>()

        for (effect in node.effects) {
            if (!effect.visible) continue
            when (effect.type) {
                "DROP_SHADOW" -> {
                    val x = effect.offset?.x?.roundToInt() ?: 0
                    val y = effect.offset?.y?.roundToInt() ?: 4
                    val blur = effect.radius?.roundToInt() ?: 4
                    val color = effect.color?.let { TailwindMapper.colorToHex(it) } ?: "rgba(0,0,0,0.1)"
                    shadows += "${x}px ${y}px ${blur}px $color"
                }
                "INNER_SHADOW" -> {
                    val x = effect.offset?.x?.roundToInt() ?: 0
                    val y = effect.offset?.y?.roundToInt() ?: 2
                    val blur = effect.radius?.roundToInt() ?: 4
                    val color = effect.color?.let { TailwindMapper.colorToHex(it) } ?: "rgba(0,0,0,0.1)"
                    shadows += "inset ${x}px ${y}px ${blur}px $color"
                }
                "LAYER_BLUR" -> {
                    val radius = effect.radius?.roundToInt() ?: 4
                    props["filter"] = "blur(${radius}px)"
                }
                "BACKGROUND_BLUR" -> {
                    val radius = effect.radius?.roundToInt() ?: 8
                    props["backdrop-filter"] = "blur(${radius}px)"
                }
            }
        }

        if (shadows.isNotEmpty()) {
            props["box-shadow"] = shadows.joinToString(", ")
        }

        return props
    }

    fun mapOpacity(node: FigmaNode): Map<String, String> {
        val opacity = node.opacity ?: return emptyMap()
        if (opacity >= 1f) return emptyMap()
        return mapOf("opacity" to formatFloat(opacity))
    }

    fun mapOverflow(node: FigmaNode): Map<String, String> {
        if (node.clipsContent == true) return mapOf("overflow" to "hidden")
        return emptyMap()
    }

    // --- Helpers ---

    private fun resolveColorHex(paint: Paint): String {
        val color = paint.color ?: return "#000000"
        return TailwindMapper.colorToHex(color, paint.opacity)
    }

    private fun resolveStyleName(node: FigmaNode, styleKey: String): String? {
        val styleId = node.styles?.get(styleKey) ?: return null
        return fileStyles[styleId]?.name
    }

    fun toRem(px: Float): String {
        if (px == 0f) return "0"
        val rem = px / 16f
        return if (rem == rem.toInt().toFloat()) {
            "${rem.toInt()}rem"
        } else {
            String.format(Locale.US, "%.3f", rem).trimEnd('0').trimEnd('.') + "rem"
        }
    }

    private fun formatFloat(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
