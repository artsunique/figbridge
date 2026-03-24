package com.artsunique.figbridge.ui

import com.artsunique.figbridge.api.FigmaAuth
import com.artsunique.figbridge.api.FigmaClient
import com.artsunique.figbridge.api.FigmaResult
import com.artsunique.figbridge.api.FigmaUser
import com.artsunique.figbridge.config.TrialManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import javax.swing.JPanel

class FigBridgeToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val wrapper = JPanel(BorderLayout())

        fun showConnect() {
            wrapper.removeAll()
            val connectPanel = ConnectPanel(
                project,
                onConnected = { user ->
                    wrapper.removeAll()
                    wrapper.add(MainPanel(project, user) { showConnect() }, BorderLayout.CENTER)
                    wrapper.revalidate()
                    wrapper.repaint()
                },
                onConnectedWithFile = { user, fileKey ->
                    wrapper.removeAll()
                    val mainPanel = MainPanel(project, user) { showConnect() }
                    mainPanel.loadFileByKey(fileKey)
                    wrapper.add(mainPanel, BorderLayout.CENTER)
                    wrapper.revalidate()
                    wrapper.repaint()
                },
            )
            wrapper.add(connectPanel, BorderLayout.CENTER)
            wrapper.revalidate()
            wrapper.repaint()
        }

        fun showMain(user: FigmaUser) {
            wrapper.removeAll()
            wrapper.add(MainPanel(project, user) { showConnect() }, BorderLayout.CENTER)
            wrapper.revalidate()
            wrapper.repaint()
        }

        // Activate trial on first use + check reminders
        TrialManager.activateIfNeeded()
        TrialManager.checkAndNotify(project)

        // Check if already authenticated
        if (FigmaAuth.getInstance().isAuthenticated()) {
            CoroutineScope(Dispatchers.Default).launch(Dispatchers.IO) {
                val result = FigmaClient.getInstance().getMe()
                launch(Dispatchers.Swing) {
                    when (result) {
                        is FigmaResult.Success -> showMain(result.data)
                        is FigmaResult.Error -> showConnect()
                    }
                }
            }
        } else {
            showConnect()
        }

        val content = ContentFactory.getInstance().createContent(wrapper, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
