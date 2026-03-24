package com.artsunique.figbridge.ui

import com.artsunique.figbridge.api.FigmaNode
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class FigmaTreeCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ) {
        val treeNode = value as? DefaultMutableTreeNode ?: return
        val figmaNode = treeNode.userObject

        if (figmaNode is FigmaNode) {
            renderFigmaNode(figmaNode)
        } else {
            // Root node = file name string
            append(figmaNode.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            icon = AllIcons.Nodes.Folder
        }
    }

    private fun renderFigmaNode(node: FigmaNode) {
        when (node.type) {
            "CANVAS" -> {
                icon = AllIcons.Nodes.Module
                append(node.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                val childCount = node.children.size
                if (childCount > 0) {
                    append("  $childCount", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
                }
            }
            "FRAME", "COMPONENT_SET" -> {
                icon = AllIcons.Nodes.PpWeb
                append(node.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                node.absoluteBoundingBox?.let { bb ->
                    append(
                        "  ${bb.width.toInt()}\u00d7${bb.height.toInt()}",
                        SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES,
                    )
                }
            }
            "COMPONENT" -> {
                icon = AllIcons.Nodes.Plugin
                append(node.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
            "GROUP" -> {
                icon = AllIcons.Nodes.Folder
                append(node.name, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            else -> {
                icon = AllIcons.FileTypes.Any_type
                append(node.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }
    }
}
