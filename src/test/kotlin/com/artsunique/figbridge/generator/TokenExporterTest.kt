package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.FigmaVariable
import com.artsunique.figbridge.api.FigmaVariableCollection
import com.artsunique.figbridge.api.FigmaVariableMode
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TokenExporterTest {

    private fun colorJson(r: Float, g: Float, b: Float, a: Float = 1f): JsonElement =
        buildJsonObject {
            put("r", r)
            put("g", g)
            put("b", b)
            put("a", a)
        }

    private fun floatJson(value: Float): JsonElement = JsonPrimitive(value)

    private fun stringJson(value: String): JsonElement = JsonPrimitive(value)

    private fun aliasJson(id: String): JsonElement =
        buildJsonObject {
            put("type", "VARIABLE_ALIAS")
            put("id", id)
        }

    @Test
    fun `generates color tokens`() {
        val variables = mapOf(
            "v1" to FigmaVariable(
                id = "v1", name = "Primary", resolvedType = "COLOR",
                variableCollectionId = "c1",
                valuesByMode = mapOf("mode1" to colorJson(0.231f, 0.51f, 0.965f)),
            ),
            "v2" to FigmaVariable(
                id = "v2", name = "Secondary", resolvedType = "COLOR",
                variableCollectionId = "c1",
                valuesByMode = mapOf("mode1" to colorJson(0.392f, 0.455f, 0.545f)),
            ),
        )
        val collections = mapOf(
            "c1" to FigmaVariableCollection(
                id = "c1", name = "Colors",
                modes = listOf(FigmaVariableMode("mode1", "Default")),
                defaultModeId = "mode1",
            ),
        )

        val css = TokenExporter.generateTokensCss(variables, collections)

        assertTrue(css.contains("/* Collection: Colors */"), "Should have collection comment")
        assertTrue(css.contains(":root {"), "Should have :root block")
        assertTrue(css.contains("--colors-primary:"), "Should have primary token")
        assertTrue(css.contains("--colors-secondary:"), "Should have secondary token")
        assertTrue(css.contains("#"), "Should have hex color")
    }

    @Test
    fun `generates float tokens with px unit`() {
        val variables = mapOf(
            "v1" to FigmaVariable(
                id = "v1", name = "Small", resolvedType = "FLOAT",
                variableCollectionId = "c1",
                valuesByMode = mapOf("m1" to floatJson(8f)),
            ),
            "v2" to FigmaVariable(
                id = "v2", name = "Medium", resolvedType = "FLOAT",
                variableCollectionId = "c1",
                valuesByMode = mapOf("m1" to floatJson(16f)),
            ),
        )
        val collections = mapOf(
            "c1" to FigmaVariableCollection(
                id = "c1", name = "Spacing",
                modes = listOf(FigmaVariableMode("m1", "Default")),
                defaultModeId = "m1",
            ),
        )

        val css = TokenExporter.generateTokensCss(variables, collections)

        assertTrue(css.contains("--spacing-small: 16px;") || css.contains("--spacing-small: 8px;"), "Should have px value")
        assertTrue(css.contains("--spacing-medium: 16px;"), "Should have 16px")
    }

    @Test
    fun `generates string tokens with quotes`() {
        val variables = mapOf(
            "v1" to FigmaVariable(
                id = "v1", name = "Font Family", resolvedType = "STRING",
                variableCollectionId = "c1",
                valuesByMode = mapOf("m1" to stringJson("Inter")),
            ),
        )
        val collections = mapOf(
            "c1" to FigmaVariableCollection(
                id = "c1", name = "Typography",
                modes = listOf(FigmaVariableMode("m1", "Default")),
                defaultModeId = "m1",
            ),
        )

        val css = TokenExporter.generateTokensCss(variables, collections)

        assertTrue(css.contains("--typography-font-family: \"Inter\";"), "Should have quoted string")
    }

    @Test
    fun `skips alias variables`() {
        val variables = mapOf(
            "v1" to FigmaVariable(
                id = "v1", name = "Alias", resolvedType = "COLOR",
                variableCollectionId = "c1",
                valuesByMode = mapOf("m1" to aliasJson("v2")),
            ),
        )
        val collections = mapOf(
            "c1" to FigmaVariableCollection(
                id = "c1", name = "Colors",
                modes = listOf(FigmaVariableMode("m1", "Default")),
                defaultModeId = "m1",
            ),
        )

        val css = TokenExporter.generateTokensCss(variables, collections)

        assertFalse(css.contains("--colors-alias"), "Should skip alias variable")
    }

    @Test
    fun `handles empty variables`() {
        val css = TokenExporter.generateTokensCss(emptyMap(), emptyMap())
        assertEquals("/* No design tokens found */\n", css)
    }

    @Test
    fun `generates rgba for semi-transparent colors`() {
        val variables = mapOf(
            "v1" to FigmaVariable(
                id = "v1", name = "Overlay", resolvedType = "COLOR",
                variableCollectionId = "c1",
                valuesByMode = mapOf("m1" to colorJson(0f, 0f, 0f, 0.5f)),
            ),
        )
        val collections = mapOf(
            "c1" to FigmaVariableCollection(
                id = "c1", name = "Colors",
                modes = listOf(FigmaVariableMode("m1", "Default")),
                defaultModeId = "m1",
            ),
        )

        val css = TokenExporter.generateTokensCss(variables, collections)

        assertTrue(css.contains("rgba(0, 0, 0,"), "Should use rgba for transparency, got: $css")
    }

    @Test
    fun `groups variables by collection`() {
        val variables = mapOf(
            "v1" to FigmaVariable(
                id = "v1", name = "Primary", resolvedType = "COLOR",
                variableCollectionId = "c1",
                valuesByMode = mapOf("m1" to colorJson(1f, 0f, 0f)),
            ),
            "v2" to FigmaVariable(
                id = "v2", name = "Base", resolvedType = "FLOAT",
                variableCollectionId = "c2",
                valuesByMode = mapOf("m1" to floatJson(16f)),
            ),
        )
        val collections = mapOf(
            "c1" to FigmaVariableCollection(
                id = "c1", name = "Colors",
                modes = listOf(FigmaVariableMode("m1", "Default")),
                defaultModeId = "m1",
            ),
            "c2" to FigmaVariableCollection(
                id = "c2", name = "Spacing",
                modes = listOf(FigmaVariableMode("m1", "Default")),
                defaultModeId = "m1",
            ),
        )

        val css = TokenExporter.generateTokensCss(variables, collections)

        assertTrue(css.contains("/* Collection: Colors */"), "Should have Colors collection")
        assertTrue(css.contains("/* Collection: Spacing */"), "Should have Spacing collection")
        // Should have two :root blocks
        assertEquals(2, Regex(":root \\{").findAll(css).count(), "Should have 2 :root blocks")
    }

    @Test
    fun `toCssName converts slashes and spaces to hyphens`() {
        assertEquals("colors-primary", TokenExporter.toCssName("Colors", "Primary"))
        assertEquals("colors-brand-primary", TokenExporter.toCssName("Colors", "Brand/Primary"))
        assertEquals("spacing-lg", TokenExporter.toCssName("Spacing", "LG"))
    }

    @Test
    fun `uses first mode when default mode not set`() {
        val variables = mapOf(
            "v1" to FigmaVariable(
                id = "v1", name = "Color", resolvedType = "COLOR",
                variableCollectionId = "c1",
                valuesByMode = mapOf("some-mode" to colorJson(1f, 0f, 0f)),
            ),
        )
        val collections = mapOf(
            "c1" to FigmaVariableCollection(
                id = "c1", name = "Colors",
                modes = listOf(FigmaVariableMode("some-mode", "Light")),
            ),
        )

        val css = TokenExporter.generateTokensCss(variables, collections)

        assertTrue(css.contains("--colors-color: #ff0000;"), "Should resolve first mode value")
    }
}
