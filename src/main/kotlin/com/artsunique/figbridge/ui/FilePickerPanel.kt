package com.artsunique.figbridge.ui

import com.artsunique.figbridge.api.FigmaCache
import com.artsunique.figbridge.api.FigmaUrlParser
import com.artsunique.figbridge.config.FigBridgeSettings
import com.artsunique.figbridge.config.RecentFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO
import javax.swing.*

class FilePickerPanel(
    private val onFileSelected: (String) -> Unit,
) : JPanel(BorderLayout()) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val cardsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(4)
    }

    init {
        border = JBUI.Borders.empty(4)

        // Title
        val titleLabel = JBLabel("Recent Files").apply {
            font = font.deriveFont(Font.BOLD, 13f)
            border = JBUI.Borders.emptyBottom(8)
        }

        // Scrollable cards area
        val scrollPane = JBScrollPane(cardsPanel).apply {
            border = JBUI.Borders.empty()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        // URL input at bottom
        val urlPanel = JPanel(BorderLayout(4, 0)).apply {
            border = JBUI.Borders.emptyTop(8)
        }
        val urlField = JBTextField().apply {
            emptyText.text = "Paste Figma file URL..."
        }
        val loadButton = JButton("Load")

        val onLoad = {
            val input = urlField.text.trim()
            if (input.isNotBlank()) {
                val fileKey = FigmaUrlParser.extractFileKey(input)
                if (fileKey != null) {
                    onFileSelected(fileKey)
                }
            }
        }

        loadButton.addActionListener { onLoad() }
        urlField.addActionListener { onLoad() }

        urlPanel.add(urlField, BorderLayout.CENTER)
        urlPanel.add(loadButton, BorderLayout.EAST)

        // Layout
        val topPanel = JPanel(BorderLayout()).apply {
            add(titleLabel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }

        add(topPanel, BorderLayout.CENTER)
        add(urlPanel, BorderLayout.SOUTH)

        loadRecentFiles()
    }

    private fun loadRecentFiles() {
        val recentFiles = FigBridgeSettings.getInstance().state.recentFiles
        cardsPanel.removeAll()

        if (recentFiles.isEmpty()) {
            val emptyLabel = JBLabel("No recent files. Paste a Figma URL below to get started.").apply {
                foreground = JBUI.CurrentTheme.Label.disabledForeground()
                alignmentX = CENTER_ALIGNMENT
                border = JBUI.Borders.empty(20)
            }
            cardsPanel.add(emptyLabel)
        } else {
            // Build rows of 2 cards
            val rows = recentFiles.chunked(2)
            for (row in rows) {
                val rowPanel = JPanel(GridLayout(1, 2, 8, 0)).apply {
                    maximumSize = Dimension(Int.MAX_VALUE, 140)
                    alignmentX = LEFT_ALIGNMENT
                }
                for (file in row) {
                    rowPanel.add(createFileCard(file))
                }
                // Fill empty slot if odd number
                if (row.size == 1) {
                    rowPanel.add(JPanel())
                }
                cardsPanel.add(rowPanel)
                cardsPanel.add(Box.createVerticalStrut(8))
            }
        }

        cardsPanel.revalidate()
        cardsPanel.repaint()
    }

    private fun createFileCard(file: RecentFile): JPanel {
        val card = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1),
                JBUI.Borders.empty(0)
            )
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(0, 130)
        }

        // Thumbnail area
        val thumbLabel = JBLabel().apply {
            horizontalAlignment = SwingConstants.CENTER
            verticalAlignment = SwingConstants.CENTER
            preferredSize = Dimension(0, 90)
            minimumSize = Dimension(0, 90)
            background = JBUI.CurrentTheme.Editor.BORDER_COLOR
            isOpaque = true
        }

        // File name
        val nameLabel = JBLabel(file.name).apply {
            font = font.deriveFont(11f)
            border = JBUI.Borders.empty(4, 6)
        }

        card.add(thumbLabel, BorderLayout.CENTER)
        card.add(nameLabel, BorderLayout.SOUTH)

        // Right-click context menu
        val popupMenu = JPopupMenu().apply {
            add(JMenuItem("Remove").apply {
                addActionListener {
                    FigBridgeSettings.getInstance().removeRecentFile(file.fileKey)
                    loadRecentFiles()
                }
            })
        }

        // Click + hover handler
        card.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger) {
                    popupMenu.show(e.component, e.x, e.y)
                } else {
                    onFileSelected(file.fileKey)
                }
            }
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) popupMenu.show(e.component, e.x, e.y)
            }
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) popupMenu.show(e.component, e.x, e.y)
            }
            override fun mouseEntered(e: MouseEvent) {
                card.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(JBUI.CurrentTheme.Focus.defaultButtonColor(), 1),
                    JBUI.Borders.empty(0)
                )
            }
            override fun mouseExited(e: MouseEvent) {
                val p = SwingUtilities.convertPoint(e.component, e.point, card)
                if (!card.contains(p)) {
                    card.border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1),
                        JBUI.Borders.empty(0)
                    )
                }
            }
        })

        // Load thumbnail async
        if (file.thumbnailUrl.isNotBlank()) {
            loadCardThumbnail(file.thumbnailUrl, thumbLabel, file.fileKey)
        }

        return card
    }

    private fun loadCardThumbnail(url: String, label: JBLabel, cacheKey: String) {
        // Check cache
        val cached = FigmaCache.getInstance().getCachedThumbnail("card_$cacheKey")
        if (cached != null) {
            setScaledIcon(label, cached)
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val image = ImageIO.read(URI(url).toURL())
                if (image != null) {
                    FigmaCache.getInstance().cacheThumbnail("card_$cacheKey", image)
                    launch(Dispatchers.Swing) { setScaledIcon(label, image) }
                }
            } catch (_: Exception) {
                // Thumbnail load failed — leave empty
            }
        }
    }

    private fun setScaledIcon(label: JBLabel, image: BufferedImage) {
        val targetWidth = (label.width.coerceAtLeast(100))
        val targetHeight = label.preferredSize.height.coerceAtLeast(90)

        val scaleW = targetWidth.toFloat() / image.width
        val scaleH = targetHeight.toFloat() / image.height
        val scale = maxOf(scaleW, scaleH)

        val w = (image.width * scale).toInt().coerceAtLeast(1)
        val h = (image.height * scale).toInt().coerceAtLeast(1)

        val scaled = image.getScaledInstance(w, h, Image.SCALE_SMOOTH)
        label.icon = ImageIcon(scaled)
    }

    fun refresh() {
        loadRecentFiles()
    }
}
