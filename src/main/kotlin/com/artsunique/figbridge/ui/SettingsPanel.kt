package com.artsunique.figbridge.ui

import com.artsunique.figbridge.config.CodeMode
import com.artsunique.figbridge.config.FigBridgeSettings
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class SettingsPanel(
    private val onBack: () -> Unit,
) : JPanel(BorderLayout()) {

    private val settings = FigBridgeSettings.getInstance()

    private val assetDirField = JBTextField(settings.state.assetDir)
    private val tokenFileField = JBTextField(settings.state.tokenFile)
    private val codeModeCombo = JComboBox(arrayOf("CSS", "Tailwind")).apply {
        selectedIndex = when (settings.codeMode) {
            CodeMode.CUSTOM_CSS -> 0
            CodeMode.TAILWIND -> 1
        }
    }
    private val oauthClientIdField = JBTextField(settings.state.oauthClientId)
    private val oauthClientSecretField = JPasswordField(settings.state.oauthClientSecret)

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
        val titleLabel = JBLabel("Settings").apply {
            font = font.deriveFont(Font.BOLD, 14f)
        }
        header.add(backLink, BorderLayout.WEST)
        header.add(titleLabel, BorderLayout.CENTER)

        // Form
        val form = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8, 0)
        }

        form.add(buildField("Asset Directory", assetDirField))
        form.add(Box.createVerticalStrut(12))
        form.add(buildField("Token File", tokenFileField))
        form.add(Box.createVerticalStrut(12))
        form.add(buildField("Default Code Mode", codeModeCombo))
        form.add(Box.createVerticalStrut(24))

        val oauthTitle = JBLabel("OAuth (optional)").apply {
            font = font.deriveFont(Font.BOLD, 12f)
            alignmentX = LEFT_ALIGNMENT
        }
        val oauthHint = JBLabel("Register a Figma app to enable OAuth login.").apply {
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            alignmentX = LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(8)
        }
        form.add(oauthTitle)
        form.add(oauthHint)
        form.add(buildField("Client ID", oauthClientIdField))
        form.add(Box.createVerticalStrut(12))
        form.add(buildField("Client Secret", oauthClientSecretField))

        // Save button
        val saveButton = JButton("Save").apply {
            alignmentX = LEFT_ALIGNMENT
            addActionListener { save() }
        }
        form.add(Box.createVerticalStrut(20))
        form.add(saveButton)

        add(header, BorderLayout.NORTH)
        add(JScrollPane(form).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
    }

    private fun buildField(label: String, component: JComponent): JPanel {
        return JPanel(BorderLayout()).apply {
            alignmentX = LEFT_ALIGNMENT
            maximumSize = java.awt.Dimension(Int.MAX_VALUE, 48)
            add(JBLabel(label).apply {
                preferredSize = java.awt.Dimension(120, 24)
            }, BorderLayout.WEST)
            add(component, BorderLayout.CENTER)
        }
    }

    private fun save() {
        val state = settings.state
        state.assetDir = assetDirField.text.trim().ifEmpty { "images" }
        state.tokenFile = tokenFileField.text.trim().ifEmpty { "tokens.css" }
        settings.codeMode = when (codeModeCombo.selectedIndex) {
            1 -> CodeMode.TAILWIND
            else -> CodeMode.CUSTOM_CSS
        }
        state.oauthClientId = oauthClientIdField.text.trim()
        state.oauthClientSecret = String(oauthClientSecretField.password).trim()

        onBack()
    }
}
