package com.artsunique.figbridge.api

object FigmaUrlParser {

    private val FILE_KEY_REGEX = Regex("""figma\.com/(?:file|design|board|proto)/([A-Za-z0-9_-]+)""")

    fun extractFileKey(input: String): String? {
        val cleaned = input.trim().lines().first().trim()
        // Direct file key (no URL)
        if (cleaned.matches(Regex("^[A-Za-z0-9_-]{10,}$"))) {
            return cleaned
        }
        return FILE_KEY_REGEX.find(cleaned)?.groupValues?.get(1)
    }
}
