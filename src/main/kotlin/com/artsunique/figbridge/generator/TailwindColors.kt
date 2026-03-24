package com.artsunique.figbridge.generator

/**
 * Tailwind CSS v4 default color palette.
 * Maps hex values (lowercase, no alpha) to Tailwind class names.
 */
object TailwindColors {

    private val hexToName: Map<String, String> by lazy { buildMap() }

    fun lookup(hex: String): String? {
        return hexToName[hex.lowercase().trimStart('#')]
    }

    private fun buildMap(): Map<String, String> {
        val m = mutableMapOf<String, String>()

        m["000000"] = "black"
        m["ffffff"] = "white"

        // Slate
        palette(m, "slate", mapOf(
            50 to "f8fafc", 100 to "f1f5f9", 200 to "e2e8f0", 300 to "cbd5e1",
            400 to "94a3b8", 500 to "64748b", 600 to "475569", 700 to "334155",
            800 to "1e293b", 900 to "0f172a", 950 to "020617",
        ))
        // Gray
        palette(m, "gray", mapOf(
            50 to "f9fafb", 100 to "f3f4f6", 200 to "e5e7eb", 300 to "d1d5db",
            400 to "9ca3af", 500 to "6b7280", 600 to "4b5563", 700 to "374151",
            800 to "1f2937", 900 to "111827", 950 to "030712",
        ))
        // Zinc
        palette(m, "zinc", mapOf(
            50 to "fafafa", 100 to "f4f4f5", 200 to "e4e4e7", 300 to "d4d4d8",
            400 to "a1a1aa", 500 to "71717a", 600 to "52525b", 700 to "3f3f46",
            800 to "27272a", 900 to "18181b", 950 to "09090b",
        ))
        // Neutral
        palette(m, "neutral", mapOf(
            50 to "fafafa", 100 to "f5f5f5", 200 to "e5e5e5", 300 to "d4d4d4",
            400 to "a3a3a3", 500 to "737373", 600 to "525252", 700 to "404040",
            800 to "262626", 900 to "171717", 950 to "0a0a0a",
        ))
        // Stone
        palette(m, "stone", mapOf(
            50 to "fafaf9", 100 to "f5f5f4", 200 to "e7e5e4", 300 to "d6d3d1",
            400 to "a8a29e", 500 to "78716c", 600 to "57534e", 700 to "44403c",
            800 to "292524", 900 to "1c1917", 950 to "0c0a09",
        ))
        // Red
        palette(m, "red", mapOf(
            50 to "fef2f2", 100 to "fee2e2", 200 to "fecaca", 300 to "fca5a5",
            400 to "f87171", 500 to "ef4444", 600 to "dc2626", 700 to "b91c1c",
            800 to "991b1b", 900 to "7f1d1d", 950 to "450a0a",
        ))
        // Orange
        palette(m, "orange", mapOf(
            50 to "fff7ed", 100 to "ffedd5", 200 to "fed7aa", 300 to "fdba74",
            400 to "fb923c", 500 to "f97316", 600 to "ea580c", 700 to "c2410c",
            800 to "9a3412", 900 to "7c2d12", 950 to "431407",
        ))
        // Amber
        palette(m, "amber", mapOf(
            50 to "fffbeb", 100 to "fef3c7", 200 to "fde68a", 300 to "fcd34d",
            400 to "fbbf24", 500 to "f59e0b", 600 to "d97706", 700 to "b45309",
            800 to "92400e", 900 to "78350f", 950 to "451a03",
        ))
        // Yellow
        palette(m, "yellow", mapOf(
            50 to "fefce8", 100 to "fef9c3", 200 to "fef08a", 300 to "fde047",
            400 to "facc15", 500 to "eab308", 600 to "ca8a04", 700 to "a16207",
            800 to "854d0e", 900 to "713f12", 950 to "422006",
        ))
        // Lime
        palette(m, "lime", mapOf(
            50 to "f7fee7", 100 to "ecfccb", 200 to "d9f99d", 300 to "bef264",
            400 to "a3e635", 500 to "84cc16", 600 to "65a30d", 700 to "4d7c0f",
            800 to "3f6212", 900 to "365314", 950 to "1a2e05",
        ))
        // Green
        palette(m, "green", mapOf(
            50 to "f0fdf4", 100 to "dcfce7", 200 to "bbf7d0", 300 to "86efac",
            400 to "4ade80", 500 to "22c55e", 600 to "16a34a", 700 to "15803d",
            800 to "166534", 900 to "14532d", 950 to "052e16",
        ))
        // Emerald
        palette(m, "emerald", mapOf(
            50 to "ecfdf5", 100 to "d1fae5", 200 to "a7f3d0", 300 to "6ee7b7",
            400 to "34d399", 500 to "10b981", 600 to "059669", 700 to "047857",
            800 to "065f46", 900 to "064e3b", 950 to "022c22",
        ))
        // Teal
        palette(m, "teal", mapOf(
            50 to "f0fdfa", 100 to "ccfbf1", 200 to "99f6e4", 300 to "5eead4",
            400 to "2dd4bf", 500 to "14b8a6", 600 to "0d9488", 700 to "0f766e",
            800 to "115e59", 900 to "134e4a", 950 to "042f2e",
        ))
        // Cyan
        palette(m, "cyan", mapOf(
            50 to "ecfeff", 100 to "cffafe", 200 to "a5f3fc", 300 to "67e8f9",
            400 to "22d3ee", 500 to "06b6d4", 600 to "0891b2", 700 to "0e7490",
            800 to "155e75", 900 to "164e63", 950 to "083344",
        ))
        // Sky
        palette(m, "sky", mapOf(
            50 to "f0f9ff", 100 to "e0f2fe", 200 to "bae6fd", 300 to "7dd3fc",
            400 to "38bdf8", 500 to "0ea5e9", 600 to "0284c7", 700 to "0369a1",
            800 to "075985", 900 to "0c4a6e", 950 to "082f49",
        ))
        // Blue
        palette(m, "blue", mapOf(
            50 to "eff6ff", 100 to "dbeafe", 200 to "bfdbfe", 300 to "93c5fd",
            400 to "60a5fa", 500 to "3b82f6", 600 to "2563eb", 700 to "1d4ed8",
            800 to "1e40af", 900 to "1e3a8a", 950 to "172554",
        ))
        // Indigo
        palette(m, "indigo", mapOf(
            50 to "eef2ff", 100 to "e0e7ff", 200 to "c7d2fe", 300 to "a5b4fc",
            400 to "818cf8", 500 to "6366f1", 600 to "4f46e5", 700 to "4338ca",
            800 to "3730a3", 900 to "312e81", 950 to "1e1b4b",
        ))
        // Violet
        palette(m, "violet", mapOf(
            50 to "f5f3ff", 100 to "ede9fe", 200 to "ddd6fe", 300 to "c4b5fd",
            400 to "a78bfa", 500 to "8b5cf6", 600 to "7c3aed", 700 to "6d28d9",
            800 to "5b21b6", 900 to "4c1d95", 950 to "2e1065",
        ))
        // Purple
        palette(m, "purple", mapOf(
            50 to "faf5ff", 100 to "f3e8ff", 200 to "e9d5ff", 300 to "d8b4fe",
            400 to "c084fc", 500 to "a855f7", 600 to "9333ea", 700 to "7e22ce",
            800 to "6b21a8", 900 to "581c87", 950 to "3b0764",
        ))
        // Fuchsia
        palette(m, "fuchsia", mapOf(
            50 to "fdf4ff", 100 to "fae8ff", 200 to "f5d0fe", 300 to "f0abfc",
            400 to "e879f9", 500 to "d946ef", 600 to "c026d3", 700 to "a21caf",
            800 to "86198f", 900 to "701a75", 950 to "4a044e",
        ))
        // Pink
        palette(m, "pink", mapOf(
            50 to "fdf2f8", 100 to "fce7f3", 200 to "fbcfe8", 300 to "f9a8d4",
            400 to "f472b6", 500 to "ec4899", 600 to "db2777", 700 to "be185d",
            800 to "9d174d", 900 to "831843", 950 to "500724",
        ))
        // Rose
        palette(m, "rose", mapOf(
            50 to "fff1f2", 100 to "ffe4e6", 200 to "fecdd3", 300 to "fda4af",
            400 to "fb7185", 500 to "f43f5e", 600 to "e11d48", 700 to "be123c",
            800 to "9f1239", 900 to "881337", 950 to "4c0519",
        ))

        return m
    }

    private fun palette(m: MutableMap<String, String>, name: String, shades: Map<Int, String>) {
        for ((shade, hex) in shades) {
            m[hex] = "$name-$shade"
        }
    }
}
