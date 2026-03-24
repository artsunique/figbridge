package com.artsunique.figbridge.api

object FigmaUrlParser {

    private val FILE_KEY_REGEX = Regex("""figma\.com/(?:file|design)/([A-Za-z0-9_-]+)""")

    fun extractFileKey(input: String): String? {
        // Direct file key (no URL)
        if (input.matches(Regex("^[A-Za-z0-9_-]{10,}$"))) {
            return input
        }
        return FILE_KEY_REGEX.find(input)?.groupValues?.get(1)
    }
}
