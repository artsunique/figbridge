package com.artsunique.figbridge.ui

import com.artsunique.figbridge.api.FigmaAuth
import com.artsunique.figbridge.api.FigmaClient
import com.artsunique.figbridge.api.FigmaOAuth
import com.artsunique.figbridge.api.FigmaResult
import com.artsunique.figbridge.api.FigmaUser
import com.artsunique.figbridge.config.AuthMethod
import com.artsunique.figbridge.config.FigBridgeSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

class ConnectPanel(
    private val project: Project,
    private val onConnected: (FigmaUser) -> Unit,
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

        val descLabel = JBLabel("Connect your Figma account to start generating code.").apply {
            alignmentX = CENTER_ALIGNMENT
            border = JBUI.Borders.emptyTop(12)
        }

        val oauthConfigured = FigmaOAuth.isConfigured()
        val connectButton = JButton("Connect Figma").apply {
            alignmentX = CENTER_ALIGNMENT
            isEnabled = oauthConfigured
            toolTipText = if (oauthConfigured) "Connect via Figma OAuth" else "OAuth not yet configured \u2014 use Personal Access Token"
            if (oauthConfigured) {
                addActionListener { startOAuthFlow() }
            }
        }

        val patLabel = JBLabel("Use Personal Access Token").apply {
            alignmentX = CENTER_ALIGNMENT
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.emptyTop(12)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    showPatDialog()
                }
            })
        }

        val trialLabel = JBLabel("14-day free trial \u00b7 No card required").apply {
            alignmentX = CENTER_ALIGNMENT
            horizontalAlignment = SwingConstants.CENTER
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            border = JBUI.Borders.emptyTop(20)
        }

        contentPanel.add(titleLabel)
        contentPanel.add(descLabel)
        contentPanel.add(javax.swing.Box.createVerticalStrut(20))
        contentPanel.add(connectButton)
        contentPanel.add(patLabel)
        contentPanel.add(trialLabel)

        contentPanel.revalidate()
        contentPanel.repaint()
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
                                is FigmaResult.Success -> {
                                    onConnected(result.data)
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
                        Messages.showErrorDialog(project, error, "OAuth Error")
                    }
                },
            )
        }
    }

    private fun showPatDialog() {
        val token = Messages.showInputDialog(
            project,
            "Paste your Figma Personal Access Token.\n\nGenerate one at: figma.com/developers \u2192 Personal Access Tokens",
            "Figma Personal Access Token",
            null
        )

        if (token.isNullOrBlank()) return

        showConnecting()

        scope.launch(Dispatchers.IO) {
            FigmaAuth.getInstance().storeToken(token)
            val result = FigmaClient.getInstance().getMe()

            launch(Dispatchers.Swing) {
                when (result) {
                    is FigmaResult.Success -> {
                        FigBridgeSettings.getInstance().authMethod = AuthMethod.PAT
                        onConnected(result.data)
                    }
                    is FigmaResult.Error -> {
                        FigmaAuth.getInstance().clearToken()
                        FigBridgeSettings.getInstance().authMethod = AuthMethod.NONE
                        showDisconnected()
                        Messages.showErrorDialog(
                            project,
                            "Could not connect: ${result.message}",
                            "Connection Failed"
                        )
                    }
                }
            }
        }
    }
}
