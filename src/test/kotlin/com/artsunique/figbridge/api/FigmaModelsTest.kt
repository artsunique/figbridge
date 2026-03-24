package com.artsunique.figbridge.api

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FigmaModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parse FigmaUser from JSON`() {
        val input = """
            {
              "id": "123456",
              "handle": "Test User",
              "email": "test@example.com",
              "img_url": "https://example.com/avatar.png"
            }
        """.trimIndent()

        val user = json.decodeFromString<FigmaUser>(input)

        assertEquals("123456", user.id)
        assertEquals("Test User", user.handle)
        assertEquals("test@example.com", user.email)
        assertEquals("https://example.com/avatar.png", user.imgUrl)
    }

    @Test
    fun `parse FigmaUser with unknown fields`() {
        val input = """
            {
              "id": "1",
              "handle": "User",
              "email": "u@e.com",
              "img_url": "",
              "unknownField": true,
              "anotherField": 42
            }
        """.trimIndent()

        val user = json.decodeFromString<FigmaUser>(input)
        assertEquals("1", user.id)
    }

    @Test
    fun `parse FigmaNode with layout properties`() {
        val input = """
            {
              "id": "1:2",
              "name": "Hero Section",
              "type": "FRAME",
              "layoutMode": "VERTICAL",
              "primaryAxisAlignItems": "CENTER",
              "counterAxisAlignItems": "CENTER",
              "paddingLeft": 24.0,
              "paddingRight": 24.0,
              "paddingTop": 64.0,
              "paddingBottom": 64.0,
              "itemSpacing": 32.0,
              "children": [
                {
                  "id": "1:3",
                  "name": "Headline",
                  "type": "TEXT",
                  "characters": "Welcome",
                  "style": {
                    "fontFamily": "Inter",
                    "fontWeight": 700,
                    "fontSize": 48.0,
                    "lineHeightPx": 56.0
                  }
                }
              ]
            }
        """.trimIndent()

        val node = json.decodeFromString<FigmaNode>(input)

        assertEquals("Hero Section", node.name)
        assertEquals("VERTICAL", node.layoutMode)
        assertEquals(24f, node.paddingLeft)
        assertEquals(32f, node.itemSpacing)
        assertEquals(1, node.children.size)

        val textNode = node.children[0]
        assertEquals("Welcome", textNode.characters)
        assertEquals(700, textNode.style?.fontWeight)
        assertEquals(48f, textNode.style?.fontSize)
    }

    @Test
    fun `parse FigmaImageResponse`() {
        val input = """
            {
              "images": {
                "1:2": "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/abc123",
                "1:3": null
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<FigmaImageResponse>(input)

        assertEquals(2, response.images.size)
        assertNotNull(response.images["1:2"])
        assertNull(response.images["1:3"])
    }

    @Test
    fun `parse Color values`() {
        val input = """
            {
              "type": "SOLID",
              "color": { "r": 0.067, "g": 0.094, "b": 0.153, "a": 1.0 },
              "opacity": 1.0,
              "visible": true
            }
        """.trimIndent()

        val paint = json.decodeFromString<Paint>(input)

        assertNotNull(paint.color)
        assertEquals(0.067f, paint.color!!.r, 0.001f)
        assertEquals(1f, paint.color!!.a)
    }
}
