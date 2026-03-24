package com.artsunique.figbridge.ui

import com.artsunique.figbridge.api.FigmaAuth
import com.artsunique.figbridge.api.FigmaClient
import com.artsunique.figbridge.api.FigmaOAuth
import com.artsunique.figbridge.api.FigmaResult
import com.artsunique.figbridge.api.FigmaUrlParser
import com.artsunique.figbridge.api.FigmaUser
import com.artsunique.figbridge.config.AuthMethod
import com.artsunique.figbridge.config.FigBridgeSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.*

class ConnectPanel(
    private val project: Project,
    private val onConnected: (FigmaUser) -> Unit,
    private val onConnectedWithFile: ((FigmaUser, String) -> Unit)? = null,
) : JPanel(BorderLayout()) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val contentPanel = JPanel()

    init {
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.border = JBUI.Borders.empty(40, 20)
        add(contentPanel, BorderLayout.NORTH)
        showDisconnected()
    }

    private fun showDisconnected() {
        contentPanel.removeAll()

        val titleLabel = JBLabel("FigBridge").apply {
            font = font.deriveFont(Font.BOLD, 18f)
            alignmentX = CENTER_ALIGNMENT
        }

        val descLabel = JBLabel("Generate code from your Figma designs.").apply {
            alignmentX = CENTER_ALIGNMENT
            border = JBUI.Borders.emptyTop(12)
        }

        val connectButton = JButton("Connect Figma").apply {
            alignmentX = CENTER_ALIGNMENT
            addActionListener { startOAuthFlow() }
        }

        // Divider
        val orLabel = JBLabel("or paste a Figma design link").apply {
            alignmentX = CENTER_ALIGNMENT
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            border = JBUI.Borders.empty(16, 0, 8, 0)
        }

        // URL input
        val urlField = JBTextField().apply {
            emptyText.text = "https://www.figma.com/design/..."
            alignmentX = CENTER_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 32)
        }
        val loadButton = JButton("Open").apply {
            alignmentX = CENTER_ALIGNMENT
        }

        val urlRow = JPanel(BorderLayout(4, 0)).apply {
            alignmentX = CENTER_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 36)
            add(urlField, BorderLayout.CENTER)
            add(loadButton, BorderLayout.EAST)
        }

        val onLoadUrl = {
            val input = urlField.text.trim()
            if (input.isNotBlank()) {
                val fileKey = FigmaUrlParser.extractFileKey(input)
                if (fileKey != null) {
                    connectWithUrl(fileKey)
                } else {
                    Messages.showWarningDialog(project, "Could not extract a file key from this URL.", "Invalid URL")
                }
            }
        }

        loadButton.addActionListener { onLoadUrl() }
        urlField.addActionListener { onLoadUrl() }

        val trialLabel = JBLabel("14-day free trial \u00b7 No credit card required").apply {
            alignmentX = CENTER_ALIGNMENT
            horizontalAlignment = SwingConstants.CENTER
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            border = JBUI.Borders.emptyTop(20)
        }

        val patLink = JBLabel("Use Personal Access Token").apply {
            alignmentX = CENTER_ALIGNMENT
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
            font = font.deriveFont(11f)
            border = JBUI.Borders.emptyTop(16)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) { showPatDialog() }
            })
        }

        contentPanel.add(titleLabel)
        contentPanel.add(descLabel)
        contentPanel.add(Box.createVerticalStrut(20))
        contentPanel.add(connectButton)
        contentPanel.add(orLabel)
        contentPanel.add(urlRow)
        contentPanel.add(trialLabel)
        contentPanel.add(patLink)

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    /** User pasted a Figma URL — authenticate if needed, then open file */
    private fun connectWithUrl(fileKey: String) {
        if (FigmaAuth.getInstance().isAuthenticated()) {
            // Already has a token — verify and open
            showConnecting()
            scope.launch(Dispatchers.IO) {
                val result = FigmaClient.getInstance().getMe()
                launch(Dispatchers.Swing) {
                    when (result) {
                        is FigmaResult.Success -> {
                            onConnectedWithFile?.invoke(result.data, fileKey)
                                ?: onConnected(result.data)
                        }
                        is FigmaResult.Error -> {
                            // Token expired — re-authenticate via OAuth
                            FigmaAuth.getInstance().clearToken()
                            startOAuthFlowThenOpenFile(fileKey)
                        }
                    }
                }
            }
        } else {
            // Not authenticated — start OAuth, then open file
            startOAuthFlowThenOpenFile(fileKey)
        }
    }

    /** Start OAuth flow, then open the file directly after login */
    private fun startOAuthFlowThenOpenFile(fileKey: String) {
        showConnecting()
        scope.launch(Dispatchers.IO) {
            FigmaOAuth.startOAuthFlow(
                onSuccess = { token ->
                    scope.launch(Dispatchers.IO) {
                        FigBridgeSettings.getInstance().authMethod = AuthMethod.OAUTH
                        FigmaAuth.getInstance().storeToken(token)
                        val result = FigmaClient.getInstance().getMe()
                        launch(Dispatchers.Swing) {
                            when (result) {
                                is FigmaResult.Success -> {
                                    onConnectedWithFile?.invoke(result.data, fileKey)
                                        ?: onConnected(result.data)
                                }
                                is FigmaResult.Error -> {
                                    FigmaAuth.getInstance().clearToken()
                                    FigBridgeSettings.getInstance().authMethod = AuthMethod.NONE
                                    showDisconnected()
                                    Messages.showErrorDialog(project, "Could not connect: ${result.message}", "Connection Failed")
                                }
                            }
                        }
                    }
                },
                onError = { error ->
                    scope.launch(Dispatchers.Swing) {
                        showDisconnected()
                        val usePat = Messages.showYesNoDialog(
                            project,
                            "$error\n\nWould you like to use a Personal Access Token instead?",
                            "OAuth Error",
                            "Use Token",
                            "Cancel",
                            null,
                        )
                        if (usePat == Messages.YES) showPatDialog()
                    }
                },
            )
        }
    }

    private fun showConnecting() {
        contentPanel.removeAll()
        val spinnerLabel = JBLabel("Connecting to Figma...", AnimatedIcon.Default(), SwingConstants.CENTER).apply {
            alignmentX = CENTER_ALIGNMENT
            border = JBUI.Borders.emptyTop(40)
        }
        contentPanel.add(spinnerLabel)
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun startOAuthFlow() {
        showConnecting()
        scope.launch(Dispatchers.IO) {
            FigmaOAuth.startOAuthFlow(
                onSuccess = { token ->
                    scope.launch(Dispatchers.IO) {
                        FigBridgeSettings.getInstance().authMethod = AuthMethod.OAUTH
                        FigmaAuth.getInstance().storeToken(token)
                        val result = FigmaClient.getInstance().getMe()
                        launch(Dispatchers.Swing) {
                            when (result) {
                                is FigmaResult.Success -> onConnected(result.data)
                                is FigmaResult.Error -> {
                                    FigmaAuth.getInstance().clearToken()
                                    FigBridgeSettings.getInstance().authMethod = AuthMethod.NONE
                                    showDisconnected()
                                    Messages.showErrorDialog(project, "Could not connect: ${result.message}", "Connection Failed")
                                }
                            }
                        }
                    }
                },
                onError = { error ->
                    scope.launch(Dispatchers.Swing) {
                        showDisconnected()
                        val usePat = Messages.showYesNoDialog(
                            project,
                            "$error\n\nWould you like to use a Personal Access Token instead?",
                            "OAuth Error",
                            "Use Token",
                            "Cancel",
                            null,
                        )
                        if (usePat == Messages.YES) showPatDialog()
                    }
                },
            )
        }
    }

    private fun showPatDialog() {
        val token = Messages.showInputDialog(
            project,
            "Paste your Figma Personal Access Token.\n\nGenerate one at figma.com/developers \u2192 Personal Access Tokens",
            "Figma Personal Access Token",
            null,
        )
        if (token.isNullOrBlank()) return

        showConnecting()
        scope.launch(Dispatchers.IO) {
            FigBridgeSettings.getInstance().authMethod = AuthMethod.PAT
            FigmaAuth.getInstance().storeToken(token)
            val result = FigmaClient.getInstance().getMe()
            launch(Dispatchers.Swing) {
                when (result) {
                    is FigmaResult.Success -> onConnected(result.data)
                    is FigmaResult.Error -> {
                        FigmaAuth.getInstance().clearToken()
                        FigBridgeSettings.getInstance().authMethod = AuthMethod.NONE
                        showDisconnected()
                        Messages.showErrorDialog(project, "Could not connect: ${result.message}", "Connection Failed")
                    }
                }
            }
        }
    }
}
