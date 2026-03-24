package com.artsunique.figbridge.generator

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Post-processor that selectively snaps arbitrary Tailwind values like `w-[123px]`
 * to the nearest standard Tailwind class like `w-32`.
 *
 * Users choose which categories to snap via the UI dropdown.
 */
object TailwindSnapper {

    enum class Category(val label: String, val prefixes: List<String>) {
        WIDTH("Width (w-)", listOf("w-")),
        HEIGHT("Height (h-)", listOf("h-")),
        PADDING("Padding (p-)", listOf("p-", "px-", "py-", "pt-", "pr-", "pb-", "pl-")),
        MARGIN("Margin (m-)", listOf("m-", "mx-", "my-", "mt-", "mr-", "mb-", "ml-")),
        GAP("Gap", listOf("gap-", "gap-x-", "gap-y-")),
        POSITION("Position (top/left)", listOf("top-", "left-", "right-", "bottom-")),
        MIN_MAX("Min/Max (min-w, max-w)", listOf("min-w-", "max-w-", "min-h-", "max-h-")),
        FONT_SIZE("Font Size (text-)", listOf("text-")),
        BORDER_RADIUS("Border Radius (rounded-)", listOf("rounded-")),
        BORDER_WIDTH("Border Width", listOf("border-")),
        LEADING("Line Height (leading-)", listOf("leading-")),
        TRACKING("Letter Spacing (tracking-)", listOf("tracking-")),
        OPACITY("Opacity", listOf("opacity-")),
    }

    // Spacing scale: px → Tailwind suffix
    private val SPACING_SCALE = listOf(
        0 to "0", 1 to "px", 2 to "0.5", 4 to "1", 6 to "1.5",
        8 to "2", 10 to "2.5", 12 to "3", 14 to "3.5", 16 to "4",
        20 to "5", 24 to "6", 28 to "7", 32 to "8", 36 to "9",
        40 to "10", 44 to "11", 48 to "12", 56 to "14", 64 to "16",
        80 to "20", 96 to "24", 112 to "28", 128 to "32", 144 to "36",
        160 to "40", 176 to "44", 192 to "48", 208 to "52", 224 to "56",
        240 to "60", 256 to "64", 288 to "72", 320 to "80", 384 to "96",
    )

    private val FONT_SIZE_SCALE = listOf(
        12 to "xs", 14 to "sm", 16 to "base", 18 to "lg", 20 to "xl",
        24 to "2xl", 30 to "3xl", 36 to "4xl", 48 to "5xl",
        60 to "6xl", 72 to "7xl", 96 to "8xl", 128 to "9xl",
    )

    private val RADIUS_SCALE = listOf(
        0 to "none", 2 to "sm", 4 to "", 6 to "md",
        8 to "lg", 12 to "xl", 16 to "2xl", 24 to "3xl",
    )

    private val BORDER_SCALE = listOf(0 to "0", 1 to "", 2 to "2", 4 to "4", 8 to "8")

    private val OPACITY_SCALE = listOf(
        0 to "0", 5 to "5", 10 to "10", 15 to "15", 20 to "20", 25 to "25",
        30 to "30", 35 to "35", 40 to "40", 45 to "45", 50 to "50",
        55 to "55", 60 to "60", 65 to "65", 70 to "70", 75 to "75",
        80 to "80", 85 to "85", 90 to "90", 95 to "95", 100 to "100",
    )

    private val LEADING_SCALE = listOf(
        12 to "3", 16 to "4", 20 to "5", 24 to "6", 28 to "7", 32 to "8",
    )

    /** Regex matching arbitrary values like prefix-[123px] or prefix-[0.5px] */
    private val ARBITRARY_PATTERN = Regex("""(\w[\w-]*?)-\[(-?\d+(?:\.\d+)?)\s*px]""")
    /** Regex for opacity-[N%] */
    private val OPACITY_PATTERN = Regex("""opacity-\[(\d+)%]""")

    /**
     * Snap arbitrary Tailwind values in the given HTML for the selected categories.
     */
    fun snap(html: String, categories: Set<Category>): String {
        if (categories.isEmpty()) return html

        val activePrefixes = categories.flatMap { it.prefixes }.toSet()
        var result = html

        // Handle opacity-[N%] separately
        if (Category.OPACITY in categories) {
            result = OPACITY_PATTERN.replace(result) { match ->
                val pct = match.groupValues[1].toIntOrNull() ?: return@replace match.value
                val (_, suffix) = snapToNearest(pct, OPACITY_SCALE)
                "opacity-$suffix"
            }
        }

        // Handle all prefix-[Npx] patterns
        result = ARBITRARY_PATTERN.replace(result) { match ->
            val fullPrefix = match.groupValues[1] // e.g. "w", "top", "rounded", "text"
            val pxValue = match.groupValues[2].toFloatOrNull()?.roundToInt() ?: return@replace match.value

            // Check if this prefix matches any active category
            val matchedCategory = categories.firstOrNull { cat ->
                cat.prefixes.any { p -> "$fullPrefix-".endsWith(p) || "$fullPrefix-" == p }
            } ?: return@replace match.value

            snapValue(fullPrefix, pxValue, matchedCategory)
        }

        return result
    }

    private fun snapValue(prefix: String, px: Int, category: Category): String {
        return when (category) {
            Category.FONT_SIZE -> {
                val (_, suffix) = snapToNearest(px, FONT_SIZE_SCALE)
                "text-$suffix"
            }
            Category.BORDER_RADIUS -> {
                val (_, suffix) = snapToNearest(px, RADIUS_SCALE)
                if (suffix.isEmpty()) "rounded" else "rounded-$suffix"
            }
            Category.BORDER_WIDTH -> {
                val (_, suffix) = snapToNearest(px, BORDER_SCALE)
                if (suffix.isEmpty()) "border" else "border-$suffix"
            }
            Category.LEADING -> {
                val (_, suffix) = snapToNearest(px, LEADING_SCALE)
                "leading-$suffix"
            }
            Category.TRACKING -> {
                val (_, suffix) = snapToNearest(px, SPACING_SCALE)
                "tracking-$suffix"
            }
            else -> {
                // Spacing-based: w, h, p, m, gap, top, left, min-w, max-w, etc.
                val (_, suffix) = snapToNearest(px, SPACING_SCALE)
                "$prefix-$suffix"
            }
        }
    }

    private fun snapToNearest(value: Int, scale: List<Pair<Int, String>>): Pair<Int, String> {
        scale.firstOrNull { it.first == value }?.let { return it }
        return scale.minBy { abs(it.first - value) }
    }
}
