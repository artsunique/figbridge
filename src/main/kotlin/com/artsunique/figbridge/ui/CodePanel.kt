package com.artsunique.figbridge.ui

import com.artsunique.figbridge.generator.TailwindSnapper
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class CodePanel(
    private val project: Project,
    private val onBack: () -> Unit,
    private val onGetAssets: ((statusCallback: (String) -> Unit) -> Unit)? = null,
) : JPanel(BorderLayout()) {

    private val editors = mutableListOf<EditorEx>()
    private var tabbedPane: JTabbedPane? = null
    private val statusLabel = JBLabel("").apply {
        foreground = JBUI.CurrentTheme.Label.disabledForeground()
        border = JBUI.Borders.empty(4)
    }
    private var assetsButton: JBLabel? = null
    private val rightPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
    }
    private var isTailwindMode = false
    private var originalHtml: String? = null

    init {
        border = JBUI.Borders.empty(4)

        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(4)
        }
        val backButton = JBLabel("\u2190 Back").apply {
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { onBack() }
            })
        }

        val copyButton = JBLabel("Copy").apply {
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    copyToClipboard()
                    val label = e.source as JBLabel
                    val originalText = label.text
                    val originalColor = label.foreground
                    label.text = "\u2713 Copied!"
                    label.foreground = java.awt.Color(0x2E, 0xA0, 0x43)
                    Timer(1500) {
                        label.text = originalText
                        label.foreground = originalColor
                    }.apply { isRepeats = false; start() }
                }
            })
        }
        rightPanel.add(copyButton)

        header.add(backButton, BorderLayout.WEST)
        header.add(rightPanel, BorderLayout.EAST)

        add(header, BorderLayout.NORTH)
        add(statusLabel, BorderLayout.SOUTH)
    }

    fun showCode(code: String, frameName: String) {
        showCode(code, null, null, 0, frameName)
    }

    fun showCode(html: String, css: String?, fontsLink: String?, assetCount: Int, frameName: String) {
        disposeEditors()

        // Prepend Google Fonts link to HTML if present
        val fullHtml = if (fontsLink != null) {
            "<!-- Google Fonts -->\n$fontsLink\n\n$html"
        } else {
            html
        }

        isTailwindMode = css == null
        originalHtml = if (isTailwindMode) fullHtml else null

        if (css != null) {
            // Tabbed view: HTML + CSS
            val tabs = JTabbedPane()
            tabs.addTab("HTML", createEditor(fullHtml, "html"))
            tabs.addTab("CSS", createEditor(css, "css"))
            tabbedPane = tabs
            add(tabs, BorderLayout.CENTER)

            val htmlLines = fullHtml.lines().size
            val cssLines = css.lines().size
            statusLabel.text = "$frameName \u00b7 HTML $htmlLines lines \u00b7 CSS $cssLines lines"
        } else {
            // Single editor (Tailwind mode)
            val editorComponent = createEditor(fullHtml, "html")
            add(editorComponent, BorderLayout.CENTER)

            val lineCount = fullHtml.lines().size
            statusLabel.text = "$frameName \u00b7 $lineCount lines"
        }

        // "Get Assets" button in header
        if (assetCount > 0 && onGetAssets != null) {
            val btn = JBLabel("\u2b07 $assetCount Assets").apply {
                foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        val label = e.source as JBLabel
                        label.cursor = Cursor.getDefaultCursor()
                        label.foreground = JBUI.CurrentTheme.Label.disabledForeground()
                        onGetAssets.invoke { status ->
                            label.text = "\u2713 $status"
                            label.foreground = java.awt.Color(0x2E, 0xA0, 0x43)
                        }
                    }
                })
            }
            assetsButton = btn
            rightPanel.add(Box.createHorizontalStrut(12), 0)
            rightPanel.add(btn, 0)
        }

        // "Snap" dropdown for Tailwind mode
        if (isTailwindMode) {
            val snapButton = JBLabel("Snap \u25be").apply {
                foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        showSnapMenu(e.component, e.x, e.y)
                    }
                })
            }
            rightPanel.add(Box.createHorizontalStrut(12), 0)
            rightPanel.add(snapButton, 0)
        }

        revalidate()
        repaint()
    }

    private fun showSnapMenu(component: java.awt.Component, x: Int, y: Int) {
        val popup = JPopupMenu()
        for (category in TailwindSnapper.Category.entries) {
            popup.add(JMenuItem(category.label).apply {
                addActionListener { applySnap(setOf(category)) }
            })
        }
        popup.addSeparator()
        popup.add(JMenuItem("All").apply {
            addActionListener { applySnap(TailwindSnapper.Category.entries.toSet()) }
        })
        popup.add(JMenuItem("Reset").apply {
            addActionListener { resetSnap() }
        })
        popup.show(component, x, y)
    }

    private fun applySnap(categories: Set<TailwindSnapper.Category>) {
        val editor = getActiveEditor() ?: return
        val currentText = editor.document.text
        val snapped = TailwindSnapper.snap(currentText, categories)
        if (snapped != currentText) {
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.setText(snapped)
            }
        }
    }

    private fun resetSnap() {
        val html = originalHtml ?: return
        val editor = getActiveEditor() ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.setText(html)
        }
    }

    private fun createEditor(code: String, extension: String): JPanel {
        val document = EditorFactory.getInstance().createDocument(code)
        val editor = EditorFactory.getInstance().createEditor(document, project) as EditorEx
        editor.settings.apply {
            isLineNumbersShown = true
            isFoldingOutlineShown = false
            isWhitespacesShown = false
            isCaretRowShown = false
            additionalLinesCount = 0
        }
        editor.isViewer = true

        val fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension)
        editor.highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)

        editors += editor
        return JPanel(BorderLayout()).apply { add(editor.component, BorderLayout.CENTER) }
    }

    private fun copyToClipboard() {
        val text = getActiveEditorText() ?: return
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    private fun getActiveEditor(): EditorEx? {
        val tabs = tabbedPane
        if (tabs != null) {
            return editors.getOrNull(tabs.selectedIndex)
        }
        return editors.firstOrNull()
    }

    private fun getActiveEditorText(): String? {
        return getActiveEditor()?.document?.text
    }

    private fun disposeEditors() {
        tabbedPane?.let { remove(it) }
        tabbedPane = null
        for (editor in editors) {
            editor.component.parent?.let { remove(it) }
            EditorFactory.getInstance().releaseEditor(editor)
        }
        editors.clear()
    }

    fun dispose() {
        disposeEditors()
    }
}
