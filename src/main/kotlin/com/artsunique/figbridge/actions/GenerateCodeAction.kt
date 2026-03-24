package com.artsunique.figbridge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class GenerateCodeAction : AnAction("FigBridge: Generate Code") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("FigBridge") ?: return
        toolWindow.show()
        // The actual generate is triggered inside MainPanel via its button
        // This action just ensures the tool window is visible and focused
    }
}

class RefreshAction : AnAction("FigBridge: Refresh") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("FigBridge") ?: return
        toolWindow.show()
    }
}
