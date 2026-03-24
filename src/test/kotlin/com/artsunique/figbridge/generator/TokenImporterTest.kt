package com.artsunique.figbridge.generator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TokenImporterTest {

    @Test
    fun `parses flat format with hex colors`() {
        val json = """
        {
            "Colors": {
                "Primary": "#3b82f6",
                "Secondary": "#64748b"
            },
            "Spacing": {
                "sm": 8,
                "md": 16
            }
        }
        """.trimIndent()

        val result = TokenImporter.parseJson(json)
        assertEquals(4, result.variables.size)
        assertEquals(2, result.collections.size)

        val css = TokenExporter.generateTokensCss(result.variables, result.collections)
        assertTrue(css.contains("--colors-primary: #3b82f6;"), "Should have primary color, got: $css")
        assertTrue(css.contains("--colors-secondary: #64748b;"), "Should have secondary color")
        assertTrue(css.contains("--spacing-sm: 8px;"), "Should have spacing sm")
        assertTrue(css.contains("--spacing-md: 16px;"), "Should have spacing md")
    }

    @Test
    fun `parses collections array format`() {
        val json = """
        {
            "collections": [
                {
                    "name": "Brand",
                    "variables": [
                        { "name": "Primary", "type": "COLOR", "value": { "r": 0.231, "g": 0.51, "b": 0.965, "a": 1 } },
                        { "name": "Font Size", "type": "FLOAT", "value": 16 }
                    ]
                }
            ]
        }
        """.trimIndent()

        val result = TokenImporter.parseJson(json)
        assertEquals(2, result.variables.size)
        assertEquals(1, result.collections.size)

        val css = TokenExporter.generateTokensCss(result.variables, result.collections)
        assertTrue(css.contains("/* Collection: Brand */"), "Should have collection name")
        assertTrue(css.contains("--brand-primary:"), "Should have primary")
        assertTrue(css.contains("--brand-font-size: 16px;"), "Should have font size")
    }

    @Test
    fun `parses Figma REST API format`() {
        val json = """
        {
            "meta": {
                "variableCollections": {
                    "c1": {
                        "name": "Colors",
                        "modes": [{ "modeId": "m1", "name": "Light" }],
                        "defaultModeId": "m1"
                    }
                },
                "variables": {
                    "v1": {
                        "name": "Primary",
                        "resolvedType": "COLOR",
                        "variableCollectionId": "c1",
                        "valuesByMode": {
                            "m1": { "r": 1, "g": 0, "b": 0, "a": 1 }
                        }
                    }
                }
            }
        }
        """.trimIndent()

        val result = TokenImporter.parseJson(json)
        assertEquals(1, result.variables.size)

        val css = TokenExporter.generateTokensCss(result.variables, result.collections)
        assertTrue(css.contains("--colors-primary: #ff0000;"), "Should have red color, got: $css")
    }

    @Test
    fun `handles string values in flat format`() {
        val json = """
        {
            "Typography": {
                "Font Family": "Inter",
                "Font Mono": "JetBrains Mono"
            }
        }
        """.trimIndent()

        val result = TokenImporter.parseJson(json)
        val css = TokenExporter.generateTokensCss(result.variables, result.collections)
        assertTrue(css.contains("--typography-font-family: \"Inter\";"), "Should have font family")
    }

    @Test
    fun `parses Oppermann Design Tokens plugin format`() {
        val json = """
        {
            "tw colors": {
                "slate": {
                    "50": { "type": "color", "value": "#f8fafcff" },
                    "100": { "type": "color", "value": "#f1f5f9ff" }
                },
                "blue": {
                    "500": { "type": "color", "value": "#3b82f6ff" }
                }
            },
            "tw spacing": {
                "spacing": {
                    "0": { "type": "dimension", "value": 0 },
                    "4": { "type": "dimension", "value": 16 }
                }
            }
        }
        """.trimIndent()

        val result = TokenImporter.parseJson(json)
        assertTrue(result.variables.isNotEmpty(), "Should have variables")
        assertEquals(2, result.collections.size, "Should have 2 collections")

        val css = TokenExporter.generateTokensCss(result.variables, result.collections)
        assertTrue(css.contains("/* Collection: tw colors */"), "Should have tw colors collection")
        assertTrue(css.contains("--tw-colors-slate-50:"), "Should have slate-50, got: $css")
        assertTrue(css.contains("--tw-colors-blue-500:"), "Should have blue-500")
        assertTrue(css.contains("--tw-spacing-spacing-0: 0px;"), "Should have spacing 0")
        assertTrue(css.contains("--tw-spacing-spacing-4: 16px;"), "Should have spacing 4")
    }

    @Test
    fun `extracts font properties from custom-fontStyle`() {
        val json = """
        {
            "font": {
                "tw": {
                    "text-base": {
                        "type": "custom-fontStyle",
                        "value": {
                            "fontSize": 16,
                            "fontFamily": "DM Sans",
                            "fontWeight": 400,
                            "lineHeight": 24,
                            "letterSpacing": 0
                        }
                    }
                }
            }
        }
        """.trimIndent()

        val result = TokenImporter.parseJson(json)
        assertTrue(result.variables.isNotEmpty(), "Should extract font properties")

        val css = TokenExporter.generateTokensCss(result.variables, result.collections)
        assertTrue(css.contains("font-size: 16px;"), "Should have font-size, got: $css")
        assertTrue(css.contains("font-family: \"DM Sans\";"), "Should have font-family")
    }

    @Test
    fun `converts hex with alpha to rgba`() {
        val json = """
        {
            "Colors": {
                "Overlay": "#00000080"
            }
        }
        """.trimIndent()

        val result = TokenImporter.parseJson(json)
        val css = TokenExporter.generateTokensCss(result.variables, result.collections)
        assertTrue(css.contains("rgba("), "Should use rgba for hex with alpha, got: $css")
    }
}
