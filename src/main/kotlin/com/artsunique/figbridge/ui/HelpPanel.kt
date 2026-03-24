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
        val titleLabel = JBLabel("Help & How to Use").apply {
            font = font.deriveFont(Font.BOLD, 14f)
        }
        header.add(backLink, BorderLayout.WEST)
        header.add(titleLabel, BorderLayout.CENTER)

        // Content
        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8, 0)
        }

        // --- Getting Started ---
        content.add(section("Getting Started"))
        content.add(bullet("Click \"Connect Figma\" to log in via your browser (OAuth)"))
        content.add(bullet("Alternatively, use a Personal Access Token (PAT) from figma.com/developers"))
        content.add(bullet("Paste a Figma file URL or select from your recent files"))
        content.add(bullet("Browse pages and frames in the tree view"))
        content.add(bullet("Select a frame and click \"Generate Code\""))
        content.add(bullet("Choose between Tailwind CSS and Custom CSS (BEM) output"))
        content.add(bullet("Copy the code to clipboard or export assets to your project"))
        content.add(spacer())

        // --- Features ---
        content.add(section("Features"))
        content.add(bullet("Tailwind CSS v4 \u2014 utility classes with @theme token support"))
        content.add(bullet("Custom CSS (BEM) \u2014 HTML + separate CSS with block__element naming"))
        content.add(bullet("Semantic HTML \u2014 section, nav, header, footer, button, h1\u2013h6, img, input, a"))
        content.add(bullet("Auto Layout \u2192 Flexbox with gap, padding, alignment"))
        content.add(bullet("Flex Wrap \u2014 wrapped Auto Layouts with row/column gap"))
        content.add(bullet("Non-Auto-Layout \u2192 absolute positioning with top/left"))
        content.add(bullet("Design Tokens \u2014 import Figma Variables as CSS custom properties"))
        content.add(bullet("Asset Export \u2014 SVG icons, PNG/WebP images, deduplicated"))
        content.add(bullet("Google Fonts \u2014 auto-detects custom fonts and generates <link> tag"))
        content.add(bullet("Tailwind Snap \u2014 convert arbitrary values to standard Tailwind classes"))
        content.add(spacer())

        // --- Keyboard Shortcuts ---
        content.add(section("Keyboard Shortcuts"))
        content.add(bullet("Ctrl+Alt+F \u2014 Open FigBridge / Generate Code"))
        content.add(bullet("Ctrl+Alt+R \u2014 Refresh current file"))
        content.add(spacer())

        // --- Tips for Best Results ---
        content.add(section("Tips for Best Results"))
        content.add(bullet("Name your Figma frames descriptively \u2192 better HTML tags and CSS classes"))
        content.add(bullet("Use Auto Layout in Figma \u2192 generates clean Flexbox code"))
        content.add(bullet("Use Figma Variables for colors/spacing \u2192 CSS custom properties in output"))
        content.add(bullet("Group related layers \u2192 better semantic HTML structure"))
        content.add(bullet("Keep frames flat when possible \u2192 less nesting in generated code"))
        content.add(spacer())

        // --- Design Token Sync ---
        content.add(section("Design Token Sync"))
        content.add(bullet("Import JSON: Export variables from Figma as JSON, then import in FigBridge"))
        content.add(bullet("Export via API: Requires a Professional or Enterprise Figma plan"))
        content.add(bullet("Tailwind mode: generates @theme { --color-*: ...; } syntax"))
        content.add(bullet("Custom CSS mode: generates :root { --color-*: ...; } syntax"))
        content.add(bullet("Token file location is configurable in Settings"))
        content.add(spacer())

        // --- Known Limitations ---
        content.add(section("Known Limitations"))
        content.add(bullet("Only top-level frames can be generated (no nested component variants)"))
        content.add(bullet("No CSS Grid \u2014 all layouts use Flexbox or absolute positioning"))
        content.add(bullet("Gradients are exported as a fallback solid color"))
        content.add(bullet("Complex text styles (mixed fonts in one text node) use the first style"))
        content.add(bullet("Figma Variables API requires Professional or Enterprise plan"))
        content.add(bullet("Very large files (100+ frames) may take a few seconds to load"))
        content.add(spacer())

        // --- Known Issues ---
        content.add(section("Known Issues"))
        content.add(bullet("Safari may block OAuth redirects \u2014 use Chrome/Firefox or a PAT instead"))
        content.add(bullet("Thumbnail preview may not load for very complex frames"))
        content.add(bullet("Token export via API returns empty if no variables are defined in the file"))
        content.add(spacer())

        // --- Authentication ---
        content.add(section("Authentication"))
        content.add(bullet("OAuth (recommended): Click \"Connect Figma\" \u2192 log in via browser"))
        content.add(bullet("PAT: Go to figma.com/developers \u2192 Personal Access Tokens \u2192 generate \u2192 paste"))
        content.add(bullet("OAuth tokens expire after 90 days and are refreshed automatically"))
        content.add(bullet("PAT tokens do not expire unless revoked"))
        content.add(bullet("Tokens are stored securely in the system keychain (JetBrains PasswordSafe)"))
        content.add(spacer())

        // --- Trial ---
        content.add(section("Trial & Licensing"))
        content.add(bullet("14-day free trial with full access \u2014 no credit card required"))
        content.add(bullet("After trial: preview, inspect, and token sync remain free"))
        content.add(bullet("Code generation and asset export require a Pro license"))
        content.add(spacer())

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

    private fun spacer(): java.awt.Component = Box.createVerticalStrut(16)
}
