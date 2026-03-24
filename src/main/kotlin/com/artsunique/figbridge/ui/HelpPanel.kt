package com.artsunique.figbridge.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class HelpPanel(
    private val onBack: () -> Unit,
) : JPanel(BorderLayout()) {

    init {
        border = JBUI.Borders.empty(4)

        // Header
        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(8)
        }
        val backLink = JBLabel("\u2190 Back").apply {
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { onBack() }
            })
        }
        val titleLabel = JBLabel("FigBridge Help").apply {
            font = font.deriveFont(Font.BOLD, 14f)
        }
        header.add(backLink, BorderLayout.WEST)
        header.add(titleLabel, BorderLayout.CENTER)

        // Content
        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8, 0)
        }

        content.add(section("What FigBridge can do"))
        content.add(bullet("Tailwind CSS v4 & Vanilla CSS from Figma Frames"))
        content.add(bullet("Semantic HTML (section, nav, button, h1\u2013h6, img, input, a)"))
        content.add(bullet("Auto Layout \u2192 Flexbox, Non-Auto-Layout \u2192 Absolute Positioning"))
        content.add(bullet("Flex Wrap support for wrapped Auto Layouts"))
        content.add(bullet("Google Fonts auto-detection"))
        content.add(bullet("Design Token Export (Figma Variables \u2192 CSS Custom Properties)"))
        content.add(bullet("Asset Export (SVG icons, PNG/WebP images)"))
        content.add(bullet("Tailwind Snap (arbitrary values \u2192 standard classes)"))

        content.add(Box.createVerticalStrut(16))

        content.add(section("Known Limitations"))
        content.add(bullet("Only top-level frames (no nested component variants)"))
        content.add(bullet("No CSS Grid support (only Flexbox)"))
        content.add(bullet("Gradients exported as fallback color"))
        content.add(bullet("Variables require Professional/Enterprise Figma plan"))
        content.add(bullet("OAuth requires a registered Figma app"))

        content.add(Box.createVerticalStrut(16))

        content.add(section("Tips for Best Results"))
        content.add(bullet("Name your frames \u2192 better HTML tags and CSS classes"))
        content.add(bullet("Use Auto Layout \u2192 clean Flexbox output"))
        content.add(bullet("Use Figma Variables for colors \u2192 CSS Custom Properties"))
        content.add(bullet("Group related layers \u2192 semantic HTML structure"))

        content.add(Box.createVerticalStrut(16))

        val versionLabel = JBLabel("FigBridge v1.0.0 \u00b7 arts-unique.com").apply {
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            alignmentX = LEFT_ALIGNMENT
        }
        content.add(versionLabel)

        add(header, BorderLayout.NORTH)
        add(JBScrollPane(content).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
    }

    private fun section(title: String): JBLabel {
        return JBLabel(title).apply {
            font = font.deriveFont(Font.BOLD, 12f)
            alignmentX = LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(4)
        }
    }

    private fun bullet(text: String): JBLabel {
        return JBLabel("\u2022 $text").apply {
            alignmentX = LEFT_ALIGNMENT
            border = JBUI.Borders.empty(1, 8, 1, 0)
        }
    }
}
