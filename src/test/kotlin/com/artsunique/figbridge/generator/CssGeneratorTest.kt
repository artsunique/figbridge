package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.*
import com.artsunique.figbridge.config.CodeMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CssGeneratorTest {

    @Test
    fun `generates flex layout CSS`() {
        val node = FigmaNode(
            id = "1:1", name = "Container", type = "FRAME",
            layoutMode = "HORIZONTAL",
            itemSpacing = 16f,
            primaryAxisAlignItems = "CENTER",
            counterAxisAlignItems = "CENTER",
            paddingLeft = 24f, paddingRight = 24f,
            paddingTop = 16f, paddingBottom = 16f,
            children = listOf(
                FigmaNode(
                    id = "1:2", name = "Title", type = "TEXT",
                    characters = "Hello World",
                    style = TypeStyle(fontFamily = "Inter", fontWeight = 700, fontSize = 24f),
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
                ),
            ),
        )

        val result = CodeGenerator.generateCss(node)
        // HTML
        assertTrue(result.html.contains("class=\"container\""), "Root should have block class, got: ${result.html}")
        assertTrue(result.html.contains("class=\"container__title\""), "Child should have BEM element class, got: ${result.html}")
        assertTrue(result.html.contains("Hello World"), "Should contain text")
        // CSS
        val css = result.css!!
        assertTrue(css.contains("display: flex;"), "Should have display flex")
        assertTrue(css.contains("flex-direction: row;"), "Should have flex-direction row")
        assertTrue(css.contains("gap: 1rem;"), "16px = 1rem gap, got: $css")
        assertTrue(css.contains("justify-content: center;"), "Should have justify-content")
        assertTrue(css.contains("align-items: center;"), "Should have align-items")
        assertTrue(css.contains("padding: 1rem 1.5rem;"), "16/24 padding, got: $css")
    }

    @Test
    fun `generates vertical layout CSS`() {
        val node = FigmaNode(
            id = "2:1", name = "Section", type = "FRAME",
            layoutMode = "VERTICAL",
            itemSpacing = 8f,
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("flex-direction: column;"), "VERTICAL → column")
        assertTrue(css.contains("gap: 0.5rem;"), "8px = 0.5rem")
        assertTrue(result.html.contains("<section"), "Frame named 'Section' → <section> tag")
    }

    @Test
    fun `generates background color CSS`() {
        val node = FigmaNode(
            id = "3:1", name = "Box", type = "FRAME",
            fills = listOf(Paint(type = "SOLID", color = Color(1f, 0f, 0f, 1f))),
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("background-color: #ff0000;"), "Red fill → background-color: #ff0000, got: $css")
    }

    @Test
    fun `generates typography CSS`() {
        val node = FigmaNode(
            id = "4:1", name = "Heading", type = "TEXT",
            characters = "Big Title",
            style = TypeStyle(fontSize = 36f, fontWeight = 700),
            fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("font-size: 2.25rem;"), "36px = 2.25rem, got: $css")
        assertTrue(css.contains("font-weight: 700;"), "Should have font-weight")
        assertTrue(css.contains("color: #000000;"), "Should have text color")
    }

    @Test
    fun `generates border-radius CSS`() {
        val node = FigmaNode(
            id = "5:1", name = "Card", type = "FRAME",
            cornerRadius = 8f,
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("border-radius: 0.5rem;"), "8px = 0.5rem, got: $css")
    }

    @Test
    fun `generates border CSS`() {
        val node = FigmaNode(
            id = "6:1", name = "Bordered", type = "FRAME",
            strokes = listOf(Paint(type = "SOLID", color = Color(0.8f, 0.8f, 0.8f, 1f))),
            strokeWeight = 2f,
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("border: 2px solid #cccccc;"), "2px solid border, got: $css")
    }

    @Test
    fun `generates shadow CSS`() {
        val node = FigmaNode(
            id = "7:1", name = "Card", type = "FRAME",
            effects = listOf(Effect(
                type = "DROP_SHADOW", visible = true, radius = 6f,
                color = Color(0f, 0f, 0f, 0.1f),
                offset = Vector(0f, 4f),
            )),
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("box-shadow:"), "Should have box-shadow, got: $css")
        assertTrue(css.contains("0px 4px 6px"), "Should have offset and blur")
    }

    @Test
    fun `generates size CSS from bounding box`() {
        val node = FigmaNode(
            id = "8:1", name = "Rectangle", type = "FRAME",
            absoluteBoundingBox = BoundingBox(0f, 0f, 672f, 400f),
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("width: 42rem;"), "672px = 42rem, got: $css")
        assertTrue(css.contains("height: 25rem;"), "400px = 25rem, got: $css")
    }

    @Test
    fun `generates max-width CSS`() {
        val node = FigmaNode(
            id = "9:1", name = "Container", type = "FRAME",
            maxWidth = 896f,
            layoutSizingHorizontal = "FILL",
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("width: 100%;"), "FILL → width: 100%")
        assertTrue(css.contains("max-width: 56rem;"), "896px = 56rem, got: $css")
    }

    @Test
    fun `generates unique class names for duplicates`() {
        val node = FigmaNode(
            id = "10:1", name = "Container", type = "FRAME",
            layoutMode = "VERTICAL",
            children = listOf(
                FigmaNode(id = "10:2", name = "Item", type = "TEXT", characters = "First", style = TypeStyle(fontSize = 16f)),
                FigmaNode(id = "10:3", name = "Item", type = "TEXT", characters = "Second", style = TypeStyle(fontSize = 16f)),
            ),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains(".container__item {"), "First 'Item' → .container__item")
        assertTrue(css.contains(".container__item-2 {"), "Second 'Item' → .container__item-2, got: $css")
    }

    @Test
    fun `CSS mode does not output tailwind classes`() {
        val node = FigmaNode(
            id = "11:1", name = "Box", type = "FRAME",
            layoutMode = "HORIZONTAL",
            itemSpacing = 16f,
            paddingLeft = 24f, paddingRight = 24f,
            paddingTop = 16f, paddingBottom = 16f,
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        assertFalse(result.html.contains("flex "), "Should NOT contain tailwind 'flex' class")
        assertFalse(result.html.contains("gap-4"), "Should NOT contain tailwind gap-4")
        assertFalse(result.html.contains("px-6"), "Should NOT contain tailwind px-6")
        assertTrue(result.html.contains("class=\"box\""), "Should have BEM class name")
    }

    @Test
    fun `generates opacity CSS`() {
        val node = FigmaNode(
            id = "12:1", name = "Faded", type = "FRAME",
            opacity = 0.5f,
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("opacity: 0.5;"), "50% opacity, got: $css")
    }

    @Test
    fun `generates overflow hidden CSS`() {
        val node = FigmaNode(
            id = "13:1", name = "Clipped", type = "FRAME",
            clipsContent = true,
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("overflow: hidden;"), "clipsContent → overflow: hidden")
    }

    @Test
    fun `generates flex-wrap CSS`() {
        val node = FigmaNode(
            id = "14:1", name = "Grid", type = "FRAME",
            layoutMode = "HORIZONTAL",
            layoutWrap = "WRAP",
            itemSpacing = 16f,
            counterAxisSpacing = 12f,
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("flex-wrap: wrap;"), "WRAP → flex-wrap: wrap, got: $css")
        assertTrue(css.contains("column-gap: 1rem;"), "16px column gap, got: $css")
        assertTrue(css.contains("row-gap: 0.75rem;"), "12px row gap, got: $css")
    }

    @Test
    fun `generates absolute positioning CSS`() {
        val parent = FigmaNode(
            id = "15:1", name = "Wrapper", type = "FRAME",
            layoutMode = "VERTICAL",
            children = listOf(
                FigmaNode(
                    id = "15:2", name = "Badge", type = "FRAME",
                    layoutPositioning = "ABSOLUTE",
                    children = emptyList(),
                ),
            ),
        )

        val result = CodeGenerator.generateCss(parent)
        val css = result.css!!
        assertTrue(css.contains("position: relative;"), "Parent → position: relative, got: $css")
        assertTrue(css.contains("position: absolute;"), "Child → position: absolute, got: $css")
    }

    @Test
    fun `icon component exports as single SVG in CSS mode`() {
        val node = FigmaNode(
            id = "16:1", name = "Discord", type = "COMPONENT",
            absoluteBoundingBox = BoundingBox(0f, 0f, 36f, 36f),
            children = listOf(
                FigmaNode(id = "16:2", name = "Vector", type = "VECTOR",
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
                    children = emptyList()),
                FigmaNode(id = "16:3", name = "Circle", type = "ELLIPSE",
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
                    children = emptyList()),
            ),
        )

        val result = CodeGenerator.generateCss(node)
        val html = result.html
        assertTrue(html.contains("<img"), "Icon component → single <img>")
        assertTrue(html.contains("discord.svg"), "Uses parent name for SVG")
        assertFalse(html.contains("<div"), "Should NOT have wrapper div")
    }

    @Test
    fun `truncates long class names`() {
        val node = FigmaNode(
            id = "17:1", name = "This is a template Figma file turned into code using Anima Learn more at AnimaApp com", type = "TEXT",
            characters = "Long text",
            style = TypeStyle(fontSize = 16f),
            fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
        )

        val result = CodeGenerator.generateCss(node)
        val html = result.html
        // Class name should be truncated to <= 40 chars
        val classMatch = Regex("class=\"([^\"]+)\"").find(html)
        assertNotNull(classMatch, "Should have a class attribute")
        val className = classMatch!!.groupValues[1]
        assertTrue(className.length <= 40, "Class name should be <= 40 chars, got: $className (${className.length})")
    }

    @Test
    fun `generates Google Fonts link`() {
        val node = FigmaNode(
            id = "18:1", name = "Page", type = "FRAME",
            layoutMode = "VERTICAL",
            children = listOf(
                FigmaNode(id = "18:2", name = "Title", type = "TEXT",
                    characters = "Hello",
                    style = TypeStyle(fontFamily = "Epilogue", fontSize = 32f, fontWeight = 600),
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f)))),
                FigmaNode(id = "18:3", name = "Body", type = "TEXT",
                    characters = "World",
                    style = TypeStyle(fontFamily = "Epilogue", fontSize = 16f, fontWeight = 400),
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f)))),
            ),
        )

        val result = CodeGenerator.generateCss(node)
        val link = result.fontsLink
        assertNotNull(link, "Should generate Google Fonts link")
        assertTrue(link!!.contains("fonts.googleapis.com"), "Should be Google Fonts URL")
        assertTrue(link.contains("Epilogue"), "Should contain font family")
        assertTrue(link.contains("400"), "Should contain weight 400")
        assertTrue(link.contains("600"), "Should contain weight 600")
        assertTrue(link.contains("display=swap"), "Should have display=swap")
    }

    @Test
    fun `no Google Fonts link for system fonts`() {
        val node = FigmaNode(
            id = "19:1", name = "Text", type = "TEXT",
            characters = "Hello",
            style = TypeStyle(fontFamily = "Arial", fontSize = 16f, fontWeight = 400),
            fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
        )

        val result = CodeGenerator.generateCss(node)
        assertNull(result.fontsLink, "System font should NOT generate Google Fonts link")
    }

    @Test
    fun `root frame includes all CSS properties`() {
        val node = FigmaNode(
            id = "20:1", name = "Frame 427", type = "FRAME",
            layoutMode = "VERTICAL",
            absoluteBoundingBox = BoundingBox(0f, 0f, 384f, 2909f),
            fills = listOf(Paint(type = "SOLID", color = Color(1f, 1f, 1f, 1f))),
            clipsContent = true,
            children = listOf(
                FigmaNode(
                    id = "20:2", name = "Badge", type = "FRAME",
                    layoutPositioning = "ABSOLUTE",
                    children = emptyList(),
                ),
                FigmaNode(
                    id = "20:3", name = "Title", type = "TEXT",
                    characters = "Hello",
                    style = TypeStyle(fontFamily = "Epilogue", fontSize = 32f, fontWeight = 700),
                    fills = listOf(Paint(type = "SOLID", color = Color(0.18f, 0.18f, 0.18f, 1f))),
                ),
            ),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        val rootRule = css.substringBefore("}") + "}"
        assertTrue(rootRule.contains("width: 24rem;"), "Root frame should have width, got: $rootRule")
        assertTrue(rootRule.contains("height:"), "Root frame should have height, got: $rootRule")
        assertTrue(rootRule.contains("background-color: #ffffff;"), "Root frame should have bg, got: $rootRule")
        assertTrue(rootRule.contains("overflow: hidden;"), "Root frame should have overflow, got: $rootRule")
        assertTrue(rootRule.contains("position: relative;"), "Root frame should have relative (has absolute child), got: $rootRule")
    }

    @Test
    fun `non-auto-layout frame positions children absolutely in CSS`() {
        val node = FigmaNode(
            id = "21:1", name = "Portfolio", type = "FRAME",
            absoluteBoundingBox = BoundingBox(0f, 0f, 1440f, 900f),
            children = listOf(
                FigmaNode(
                    id = "21:2", name = "Title", type = "TEXT",
                    characters = "Hello",
                    style = TypeStyle(fontSize = 32f, fontWeight = 700),
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
                    absoluteBoundingBox = BoundingBox(120f, 160f, 200f, 40f),
                ),
            ),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        assertTrue(css.contains("position: relative;"), "Parent → position: relative, got: $css")
        assertTrue(css.contains("position: absolute;"), "Child → position: absolute, got: $css")
        assertTrue(css.contains("top: 10rem;"), "160px = 10rem top, got: $css")
        assertTrue(css.contains("left: 7.5rem;"), "120px = 7.5rem left, got: $css")
    }

    @Test
    fun `icon with GROUP children exports as SVG in CSS mode`() {
        val node = FigmaNode(
            id = "22:1", name = "bi-envelope", type = "COMPONENT",
            absoluteBoundingBox = BoundingBox(0f, 0f, 24f, 24f),
            children = listOf(
                FigmaNode(
                    id = "22:2", name = "Group", type = "GROUP",
                    children = listOf(
                        FigmaNode(id = "22:3", name = "Path", type = "VECTOR", children = emptyList()),
                    ),
                ),
            ),
        )

        val result = CodeGenerator.generateCss(node)
        assertTrue(result.html.contains("<img"), "GROUP-based icon → <img>, got: ${result.html}")
        assertTrue(result.html.contains("bi-envelope.svg"), "Uses parent name")
        assertEquals(1, result.assets.size, "Single asset export")
    }

    @Test
    fun `toRem conversion`() {
        assertEquals("0", CssMapper.toRem(0f))
        assertEquals("1rem", CssMapper.toRem(16f))
        assertEquals("1.5rem", CssMapper.toRem(24f))
        assertEquals("0.5rem", CssMapper.toRem(8f))
        assertEquals("0.25rem", CssMapper.toRem(4f))
        assertEquals("2.25rem", CssMapper.toRem(36f))
    }
}
