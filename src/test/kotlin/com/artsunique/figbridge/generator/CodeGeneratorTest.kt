package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CodeGeneratorTest {

    @Test
    fun `generates flex layout from auto layout`() {
        val node = FigmaNode(
            id = "1:1",
            name = "Container",
            type = "FRAME",
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
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f)))
                ),
            ),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("flex"), "Should contain flex")
        assertTrue(code.contains("flex-row"), "Should contain flex-row")
        assertTrue(code.contains("gap-4"), "16px gap = gap-4")
        assertTrue(code.contains("justify-center"), "Should contain justify-center")
        assertTrue(code.contains("items-center"), "Should contain items-center")
        assertTrue(code.contains("px-6"), "24px padding = px-6")
        assertTrue(code.contains("py-4"), "16px padding = py-4")
        assertTrue(code.contains("Hello World"), "Should contain text content")
        assertTrue(code.contains("text-2xl"), "24px = text-2xl")
        assertTrue(code.contains("font-bold"), "700 = font-bold")
    }

    @Test
    fun `generates vertical layout`() {
        val node = FigmaNode(
            id = "2:1", name = "section", type = "FRAME",
            layoutMode = "VERTICAL",
            itemSpacing = 8f,
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("flex"), "Should contain flex")
        assertTrue(code.contains("flex-col"), "Should contain flex-col")
        assertTrue(code.contains("gap-2"), "8px gap = gap-2")
        assertTrue(code.contains("<section"), "Frame named 'section' → <section> tag")
    }

    @Test
    fun `generates image placeholder for image fill`() {
        val node = FigmaNode(
            id = "3:1", name = "Hero Image", type = "FRAME",
            fills = listOf(Paint(type = "IMAGE", visible = true, imageRef = "ref123")),
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("<img"), "Image fill → <img> tag")
        assertTrue(code.contains("alt=\"Hero Image\""), "Should have alt text")
        assertTrue(code.contains("/>"), "Should be self-closing")
    }

    @Test
    fun `detects heading from font size`() {
        val node = FigmaNode(
            id = "4:1", name = "Text", type = "TEXT",
            characters = "Big Title",
            style = TypeStyle(fontSize = 36f, fontWeight = 700),
            fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("<h1") || code.contains("<h2"), "Large text → heading tag")
        assertTrue(code.contains("Big Title"), "Should contain text")
    }

    @Test
    fun `maps border radius`() {
        val node = FigmaNode(
            id = "5:1", name = "Card", type = "FRAME",
            cornerRadius = 8f,
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("rounded-lg"), "8px radius = rounded-lg")
    }

    @Test
    fun `maps background color`() {
        val node = FigmaNode(
            id = "6:1", name = "Box", type = "FRAME",
            fills = listOf(Paint(type = "SOLID", color = Color(1f, 0f, 0f, 1f))),
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("bg-[#ff0000]"), "Red fill → bg-[#ff0000]")
    }

    @Test
    fun `maps shadow effects`() {
        val node = FigmaNode(
            id = "7:1", name = "Card", type = "FRAME",
            effects = listOf(Effect(type = "DROP_SHADOW", visible = true, radius = 6f)),
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("shadow-md"), "6px radius shadow = shadow-md")
    }

    @Test
    fun `generates button tag from name`() {
        val node = FigmaNode(
            id = "8:1", name = "Button Primary", type = "FRAME",
            layoutMode = "HORIZONTAL",
            paddingLeft = 16f, paddingRight = 16f,
            paddingTop = 8f, paddingBottom = 8f,
            cornerRadius = 6f,
            fills = listOf(Paint(type = "SOLID", color = Color(0.18f, 0.63f, 0.27f, 1f))),
            children = listOf(
                FigmaNode(
                    id = "8:2", name = "Label", type = "TEXT",
                    characters = "Click me",
                    style = TypeStyle(fontSize = 16f, fontWeight = 600),
                    fills = listOf(Paint(type = "SOLID", color = Color(1f, 1f, 1f, 1f))),
                ),
            ),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("<button"), "Name 'Button' → <button> tag")
        assertTrue(code.contains("Click me"), "Should contain button text")
        assertTrue(code.contains("rounded-md"), "6px = rounded-md")
    }

    @Test
    fun `color to hex conversion`() {
        assertEquals("#ff0000", TailwindMapper.colorToHex(Color(1f, 0f, 0f, 1f)))
        assertEquals("#00ff00", TailwindMapper.colorToHex(Color(0f, 1f, 0f, 1f)))
        assertEquals("#0000ff", TailwindMapper.colorToHex(Color(0f, 0f, 1f, 1f)))
        assertEquals("#ffffff", TailwindMapper.colorToHex(Color(1f, 1f, 1f, 1f)))
        assertEquals("#000000", TailwindMapper.colorToHex(Color(0f, 0f, 0f, 1f)))
    }

    @Test
    fun `uses variable name for bg color`() {
        val variables = mapOf(
            "VariableID:4:123" to FigmaVariable(
                id = "VariableID:4:123", name = "slate/300", resolvedType = "COLOR",
            ),
        )
        val node = FigmaNode(
            id = "10:1", name = "Box", type = "FRAME",
            fills = listOf(Paint(
                type = "SOLID", color = Color(0.8f, 0.84f, 0.88f, 1f),
                boundVariables = mapOf("color" to VariableAlias(type = "VARIABLE_ALIAS", id = "VariableID:4:123")),
            )),
            children = emptyList(),
        )

        val code = CodeGenerator.generate(node, variables = variables).html
        assertTrue(code.contains("bg-slate-300"), "Should use variable name: bg-slate-300, got: $code")
        assertFalse(code.contains("bg-[#"), "Should NOT fall back to hex")
    }

    @Test
    fun `uses variable name for text color`() {
        val variables = mapOf(
            "VariableID:5:1" to FigmaVariable(
                id = "VariableID:5:1", name = "colors/zinc/900", resolvedType = "COLOR",
            ),
        )
        val node = FigmaNode(
            id = "11:1", name = "Label", type = "TEXT",
            characters = "Hello",
            style = TypeStyle(fontSize = 16f, fontWeight = 400),
            fills = listOf(Paint(
                type = "SOLID", color = Color(0.1f, 0.1f, 0.1f, 1f),
                boundVariables = mapOf("color" to VariableAlias(type = "VARIABLE_ALIAS", id = "VariableID:5:1")),
            )),
        )

        val code = CodeGenerator.generate(node, variables = variables).html
        assertTrue(code.contains("text-zinc-900"), "Should use variable name: text-zinc-900, got: $code")
    }

    @Test
    fun `variable name stripping prefixes`() {
        assertEquals("slate-300", TailwindMapper.variableToTailwind("slate/300"))
        assertEquals("slate-300", TailwindMapper.variableToTailwind("colors/slate/300"))
        assertEquals("blue-500", TailwindMapper.variableToTailwind("primitives/blue/500"))
        assertEquals("white", TailwindMapper.variableToTailwind("color/white"))
        assertEquals("primary", TailwindMapper.variableToTailwind("tokens/primary"))
        assertEquals("primary-500", TailwindMapper.variableToTailwind("semantic/primary/500"))
    }

    @Test
    fun `falls back to hex when no variable bound`() {
        TailwindMapper.variables = emptyMap()
        TailwindMapper.fileStyles = emptyMap()
        val node = FigmaNode(
            id = "12:1", name = "Box", type = "FRAME",
            fills = listOf(Paint(type = "SOLID", color = Color(1f, 0f, 0f, 1f))),
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("bg-[#ff0000]"), "No variable → hex fallback")
    }

    @Test
    fun `matches Tailwind palette hex to class name`() {
        TailwindMapper.variables = emptyMap()
        TailwindMapper.fileStyles = emptyMap()
        // slate-300 = #cbd5e1
        val node = FigmaNode(
            id = "13:1", name = "Box", type = "FRAME",
            fills = listOf(Paint(type = "SOLID", color = Color(0.796f, 0.835f, 0.882f, 1f))), // ~#cbd5e1
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("bg-slate-300"), "Tailwind palette hex → bg-slate-300, got: $code")
    }

    @Test
    fun `uses Figma style name for color`() {
        val styles = mapOf(
            "S:abc123" to FigmaStyle(key = "abc123", name = "zinc/800", styleType = "FILL"),
        )
        val node = FigmaNode(
            id = "14:1", name = "Card", type = "FRAME",
            fills = listOf(Paint(type = "SOLID", color = Color(0.15f, 0.15f, 0.17f, 1f))),
            styles = mapOf("fill" to "S:abc123"),
            children = emptyList(),
        )

        val code = CodeGenerator.generate(node, fileStyles = styles).html
        assertTrue(code.contains("bg-zinc-800"), "Figma style name → bg-zinc-800, got: $code")
    }

    @Test
    fun `hex palette lookup`() {
        assertEquals("white", TailwindColors.lookup("#ffffff"))
        assertEquals("black", TailwindColors.lookup("#000000"))
        assertEquals("slate-300", TailwindColors.lookup("#cbd5e1"))
        assertEquals("blue-500", TailwindColors.lookup("#3b82f6"))
        assertEquals("red-500", TailwindColors.lookup("#ef4444"))
        assertNull(TailwindColors.lookup("#ff0000")) // Not a standard Tailwind color
    }

    @Test
    fun `hidden nodes get hidden class`() {
        val node = FigmaNode(
            id = "9:1", name = "Hidden", type = "FRAME",
            visible = false,
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("hidden"), "Invisible node → hidden class")
    }

    @Test
    fun `maps max-w-4xl from maxWidth 896`() {
        val node = FigmaNode(
            id = "15:1", name = "Container", type = "FRAME",
            maxWidth = 896f,
            layoutSizingHorizontal = "FILL",
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("max-w-4xl"), "896px maxWidth → max-w-4xl, got: $code")
        assertTrue(code.contains("w-full"), "FILL → w-full")
    }

    @Test
    fun `maps fill and fixed sizing`() {
        val node = FigmaNode(
            id = "16:1", name = "Box", type = "FRAME",
            layoutSizingHorizontal = "FILL",
            layoutSizingVertical = "FIXED",
            absoluteBoundingBox = BoundingBox(0f, 0f, 300f, 64f),
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("w-full"), "FILL horizontal → w-full")
        assertTrue(code.contains("h-16"), "64px fixed height → h-16, got: $code")
    }

    @Test
    fun `maps grow and self-stretch`() {
        val node = FigmaNode(
            id = "17:1", name = "Flexible", type = "FRAME",
            layoutGrow = 1f,
            layoutAlign = "STRETCH",
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("grow"), "layoutGrow 1 → grow")
        assertTrue(code.contains("self-stretch"), "STRETCH → self-stretch")
    }

    @Test
    fun `maps min and max constraints`() {
        val node = FigmaNode(
            id = "18:1", name = "Constrained", type = "FRAME",
            minWidth = 200f,
            maxWidth = 1280f,
            minHeight = 48f,
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("min-w-[200px]"), "minWidth 200 → min-w-[200px], got: $code")
        assertTrue(code.contains("max-w-7xl"), "1280px → max-w-7xl, got: $code")
        assertTrue(code.contains("min-h-12"), "minHeight 48px → min-h-12, got: $code")
    }

    @Test
    fun `maps flex-wrap from layoutWrap`() {
        val node = FigmaNode(
            id = "19:1", name = "Grid", type = "FRAME",
            layoutMode = "HORIZONTAL",
            layoutWrap = "WRAP",
            itemSpacing = 16f,
            counterAxisSpacing = 12f,
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("flex-wrap"), "WRAP → flex-wrap, got: $code")
        assertTrue(code.contains("gap-x-4"), "16px main gap → gap-x-4, got: $code")
        assertTrue(code.contains("gap-y-3"), "12px cross gap → gap-y-3, got: $code")
        assertFalse(code.contains("gap-4"), "Should NOT use unified gap when axes differ")
    }

    @Test
    fun `maps absolute positioning`() {
        val parent = FigmaNode(
            id = "20:1", name = "Container", type = "FRAME",
            layoutMode = "VERTICAL",
            children = listOf(
                FigmaNode(
                    id = "20:2", name = "Badge", type = "FRAME",
                    layoutPositioning = "ABSOLUTE",
                    children = emptyList(),
                ),
            ),
        )

        val code = CodeGenerator.generateTailwind(parent)
        assertTrue(code.contains("relative"), "Parent with absolute child → relative, got: $code")
        assertTrue(code.contains("absolute"), "ABSOLUTE positioning → absolute, got: $code")
    }

    @Test
    fun `maps content-between for wrapped layout`() {
        val node = FigmaNode(
            id = "21:1", name = "Tags", type = "FRAME",
            layoutMode = "HORIZONTAL",
            layoutWrap = "WRAP",
            itemSpacing = 8f,
            counterAxisAlignContent = "SPACE_BETWEEN",
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("content-between"), "SPACE_BETWEEN → content-between, got: $code")
    }

    @Test
    fun `standalone frame gets width from bounding box`() {
        val node = FigmaNode(
            id = "22:1", name = "Card", type = "FRAME",
            // No layoutSizingHorizontal → standalone frame
            absoluteBoundingBox = BoundingBox(0f, 0f, 672f, 400f),
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("w-2xl"), "672px standalone → w-2xl, got: $code")
        assertTrue(code.contains("h-[400px]"), "400px height → h-[400px], got: $code")
    }

    @Test
    fun `icon component exports as single SVG`() {
        val node = FigmaNode(
            id = "24:1", name = "Social Icons", type = "FRAME",
            layoutMode = "HORIZONTAL",
            itemSpacing = 24f,
            children = listOf(
                FigmaNode(
                    id = "24:2", name = "Discord", type = "INSTANCE",
                    absoluteBoundingBox = BoundingBox(0f, 0f, 36f, 36f),
                    children = listOf(
                        FigmaNode(id = "24:3", name = "Vector", type = "VECTOR",
                            fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
                            children = emptyList()),
                    ),
                ),
                FigmaNode(
                    id = "24:4", name = "Facebook", type = "INSTANCE",
                    absoluteBoundingBox = BoundingBox(0f, 0f, 36f, 36f),
                    children = listOf(
                        FigmaNode(id = "24:5", name = "Vector", type = "VECTOR",
                            fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
                            children = emptyList()),
                    ),
                ),
            ),
        )

        val result = CodeGenerator.generate(node)
        val html = result.html
        // Icon instances should be <img>, not <div><img/></div>
        assertTrue(html.contains("<img") , "Should have img tags")
        assertFalse(html.contains("<div class=\"\">") , "Should NOT have empty wrapper divs")
        assertTrue(html.contains("alt=\"Discord\""), "Discord icon alt text")
        assertTrue(html.contains("alt=\"Facebook\""), "Facebook icon alt text")
        assertTrue(html.contains("discord.svg"), "Discord exported as SVG")
        assertTrue(html.contains("facebook.svg"), "Facebook exported as SVG")
    }

    @Test
    fun `unique asset filenames for duplicate names`() {
        val node = FigmaNode(
            id = "25:1", name = "Logo Bar", type = "FRAME",
            layoutMode = "HORIZONTAL",
            children = listOf(
                FigmaNode(id = "25:2", name = "Vector", type = "VECTOR", children = emptyList()),
                FigmaNode(id = "25:3", name = "Vector", type = "VECTOR", children = emptyList()),
                FigmaNode(id = "25:4", name = "Vector", type = "VECTOR", children = emptyList()),
            ),
        )

        val result = CodeGenerator.generate(node)
        val assets = result.assets
        assertEquals(3, assets.size, "Should have 3 assets")
        val fileNames = assets.map { it.fileName }.toSet()
        assertEquals(3, fileNames.size, "All filenames should be unique: $fileNames")
        assertTrue(fileNames.contains("vector.svg"), "First → vector.svg")
        assertTrue(fileNames.contains("vector-2.svg"), "Second → vector-2.svg")
        assertTrue(fileNames.contains("vector-3.svg"), "Third → vector-3.svg")
    }

    @Test
    fun `node with children named image is not treated as img`() {
        val node = FigmaNode(
            id = "26:1", name = "Client Image & Info", type = "FRAME",
            layoutMode = "HORIZONTAL",
            itemSpacing = 16f,
            children = listOf(
                FigmaNode(id = "26:2", name = "Avatar", type = "FRAME",
                    fills = listOf(Paint(type = "IMAGE", visible = true, imageRef = "ref1")),
                    children = emptyList()),
                FigmaNode(id = "26:3", name = "Name", type = "TEXT",
                    characters = "John Doe",
                    style = TypeStyle(fontSize = 16f),
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f)))),
            ),
        )

        val code = CodeGenerator.generateTailwind(node)
        // Should NOT be a single <img> — it has children
        assertTrue(code.contains("<div"), "Node with children should render as div, got: $code")
        assertTrue(code.contains("John Doe"), "Should recurse into children")
    }

    @Test
    fun `no background-color on vector nodes`() {
        val node = FigmaNode(
            id = "27:1", name = "Star Icon", type = "VECTOR",
            fills = listOf(Paint(type = "SOLID", color = Color(1f, 0.84f, 0f, 1f))),
            absoluteBoundingBox = BoundingBox(0f, 0f, 20f, 20f),
            children = emptyList(),
        )

        val result = CodeGenerator.generateCss(node)
        val css = result.css!!
        // Vector fills are SVG path colors, not CSS backgrounds
        assertFalse(css.contains("background-color"), "Vectors should NOT get background-color, got: $css")
    }

    @Test
    fun `text inside button uses span not heading`() {
        val node = FigmaNode(
            id = "28:1", name = "Button Primary", type = "FRAME",
            layoutMode = "HORIZONTAL",
            paddingLeft = 24f, paddingRight = 24f,
            paddingTop = 12f, paddingBottom = 12f,
            fills = listOf(Paint(type = "SOLID", color = Color(0.18f, 0.18f, 0.18f, 1f))),
            children = listOf(
                FigmaNode(
                    id = "28:2", name = "Contact", type = "TEXT",
                    characters = "Contact",
                    style = TypeStyle(fontSize = 20f, fontWeight = 600),
                    fills = listOf(Paint(type = "SOLID", color = Color(1f, 1f, 1f, 1f))),
                ),
            ),
        )

        val code = CodeGenerator.generateTailwind(node)
        // 20px text inside a button should be <span>, not <h3> (h3 has default margins)
        assertTrue(code.contains("<span"), "Text inside button → <span>, got: $code")
        assertFalse(code.contains("<h3"), "Should NOT use <h3> inside button")
        assertTrue(code.contains("Contact"), "Should contain text")
    }

    @Test
    fun `non-auto-layout frame positions children absolutely`() {
        val node = FigmaNode(
            id = "29:1", name = "Portfolio", type = "FRAME",
            // No layoutMode → non-auto-layout
            absoluteBoundingBox = BoundingBox(0f, 0f, 1440f, 900f),
            children = listOf(
                FigmaNode(
                    id = "29:2", name = "Title", type = "TEXT",
                    characters = "Hello",
                    style = TypeStyle(fontSize = 32f, fontWeight = 700),
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
                    absoluteBoundingBox = BoundingBox(120f, 110f, 200f, 40f),
                ),
                FigmaNode(
                    id = "29:3", name = "Subtitle", type = "TEXT",
                    characters = "World",
                    style = TypeStyle(fontSize = 16f, fontWeight = 400),
                    fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
                    absoluteBoundingBox = BoundingBox(120f, 170f, 300f, 24f),
                ),
            ),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertTrue(code.contains("relative"), "Parent → relative, got: $code")
        assertTrue(code.contains("absolute"), "Children → absolute, got: $code")
        assertTrue(code.contains("top-[110px]"), "Title top offset, got: $code")
        assertTrue(code.contains("left-[120px]"), "Title left offset, got: $code")
        assertTrue(code.contains("top-[170px]"), "Subtitle top offset, got: $code")
    }

    @Test
    fun `icon component with GROUP children exports as single SVG`() {
        val node = FigmaNode(
            id = "30:1", name = "bi-linkedin", type = "INSTANCE",
            absoluteBoundingBox = BoundingBox(0f, 0f, 24f, 24f),
            children = listOf(
                FigmaNode(
                    id = "30:2", name = "Group", type = "GROUP",
                    children = listOf(
                        FigmaNode(id = "30:3", name = "Vector", type = "VECTOR", children = emptyList()),
                        FigmaNode(id = "30:4", name = "Vector", type = "VECTOR", children = emptyList()),
                    ),
                ),
            ),
        )

        val result = CodeGenerator.generate(node)
        val html = result.html
        assertTrue(html.contains("<img"), "GROUP-based icon → single <img>, got: $html")
        assertTrue(html.contains("bi-linkedin.svg"), "Uses parent name for SVG")
        assertFalse(html.contains("<div"), "Should NOT have wrapper div")
        assertEquals(1, result.assets.size, "Should export as single asset")
    }

    @Test
    fun `Roboto is not a system font`() {
        val node = FigmaNode(
            id = "31:1", name = "Text", type = "TEXT",
            characters = "Hello",
            style = TypeStyle(fontFamily = "Roboto", fontSize = 16f, fontWeight = 400),
            fills = listOf(Paint(type = "SOLID", color = Color(0f, 0f, 0f, 1f))),
        )

        val result = CodeGenerator.generate(node)
        assertNotNull(result.fontsLink, "Roboto should generate a Google Fonts link")
        assertTrue(result.fontsLink!!.contains("Roboto"), "Link should contain Roboto")
    }

    @Test
    fun `HUG sizing emits no width or height`() {
        val node = FigmaNode(
            id = "23:1", name = "Tag", type = "FRAME",
            layoutSizingHorizontal = "HUG",
            layoutSizingVertical = "HUG",
            absoluteBoundingBox = BoundingBox(0f, 0f, 120f, 32f),
            children = emptyList(),
        )

        val code = CodeGenerator.generateTailwind(node)
        assertFalse(code.contains("w-"), "HUG should not emit width, got: $code")
        assertFalse(code.contains("h-"), "HUG should not emit height, got: $code")
    }
}
