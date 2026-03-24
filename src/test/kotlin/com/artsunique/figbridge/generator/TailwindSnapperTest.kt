package com.artsunique.figbridge.generator

import com.artsunique.figbridge.generator.TailwindSnapper.Category
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TailwindSnapperTest {

    @Test
    fun `snaps width arbitrary values`() {
        val html = """<div class="w-[123px] h-[400px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, setOf(Category.WIDTH))
        assertTrue(result.contains("w-32"), "123px → w-32 (128px nearest), got: $result")
        assertTrue(result.contains("h-[400px]"), "Height should NOT be snapped")
    }

    @Test
    fun `snaps height arbitrary values`() {
        val html = """<div class="w-[123px] h-[400px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, setOf(Category.HEIGHT))
        assertTrue(result.contains("w-[123px]"), "Width should NOT be snapped")
        assertTrue(result.contains("h-96"), "400px → h-96 (384px nearest), got: $result")
    }

    @Test
    fun `snaps font size arbitrary values`() {
        val html = """<p class="text-[11px]">Hello</p>"""
        val result = TailwindSnapper.snap(html, setOf(Category.FONT_SIZE))
        assertTrue(result.contains("text-xs"), "11px → text-xs (12px), got: $result")
    }

    @Test
    fun `snaps position arbitrary values`() {
        val html = """<div class="absolute top-[110px] left-[120px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, setOf(Category.POSITION))
        assertTrue(result.contains("top-28"), "110px → top-28 (112px), got: $result")
        assertFalse(result.contains("top-["), "Should not have arbitrary top")
        assertFalse(result.contains("left-["), "Should not have arbitrary left")
    }

    @Test
    fun `snaps border radius`() {
        val html = """<div class="rounded-[10px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, setOf(Category.BORDER_RADIUS))
        assertTrue(result.contains("rounded-lg"), "10px → rounded-lg (8px nearest), got: $result")
    }

    @Test
    fun `snaps padding`() {
        val html = """<div class="px-[22px] py-[13px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, setOf(Category.PADDING))
        assertFalse(result.contains("["), "Should have no arbitrary values, got: $result")
    }

    @Test
    fun `snaps gap`() {
        val html = """<div class="gap-[15px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, setOf(Category.GAP))
        assertFalse(result.contains("["), "Should have no arbitrary values, got: $result")
    }

    @Test
    fun `does not snap when no categories selected`() {
        val html = """<div class="w-[123px] text-[11px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, emptySet())
        assertEquals(html, result, "Should return unchanged HTML")
    }

    @Test
    fun `snaps multiple categories at once`() {
        val html = """<div class="w-[123px] text-[11px] rounded-[10px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, setOf(Category.WIDTH, Category.FONT_SIZE, Category.BORDER_RADIUS))
        assertFalse(result.contains("["), "All arbitrary values should be snapped, got: $result")
    }

    @Test
    fun `does not modify exact tailwind classes`() {
        val html = """<div class="w-32 text-xl rounded-lg p-4">Hello</div>"""
        val result = TailwindSnapper.snap(html, TailwindSnapper.Category.entries.toSet())
        assertEquals(html, result, "Standard classes should not be modified")
    }

    @Test
    fun `snaps opacity`() {
        val html = """<div class="opacity-[33%]">Hello</div>"""
        val result = TailwindSnapper.snap(html, setOf(Category.OPACITY))
        assertTrue(result.contains("opacity-35"), "33% → opacity-35 (nearest), got: $result")
    }

    @Test
    fun `snap all categories`() {
        val html = """<div class="w-[123px] h-[45px] p-[7px] text-[11px] top-[50px] rounded-[10px]">Hello</div>"""
        val result = TailwindSnapper.snap(html, TailwindSnapper.Category.entries.toSet())
        assertFalse(result.contains("["), "All arbitrary values should be snapped, got: $result")
    }
}
