package com.artsunique.figbridge.ui

import com.artsunique.figbridge.api.*
import com.artsunique.figbridge.config.CodeMode
import com.artsunique.figbridge.config.FigBridgeSettings
import com.artsunique.figbridge.config.TrialManager
import com.artsunique.figbridge.generator.AssetExporter
import com.artsunique.figbridge.generator.AssetInfo
import com.artsunique.figbridge.generator.CodeGenerator
import com.artsunique.figbridge.generator.GeneratedResult
import com.artsunique.figbridge.generator.TokenExporter
import com.artsunique.figbridge.generator.TokenImporter
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.SpeedSearchComparator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import com.intellij.ui.AnimatedIcon
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Font
import java.awt.Image
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class MainPanel(
    private val project: Project,
    private val user: FigmaUser,
    private val onDisconnect: () -> Unit,
) : JPanel(BorderLayout()) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentFileKey: String? = null
    private var selectedNode: FigmaNode? = null
    private var codePanel: CodePanel? = null
    private var currentFileStyles: Map<String, com.artsunique.figbridge.api.FigmaStyle> = emptyMap()

    // Content area that switches between file picker, tree view, and code view
    private val contentArea = JPanel(BorderLayout())

    // File picker
    private val filePickerPanel = FilePickerPanel { fileKey -> loadFile(fileKey) }

    // Tree view components
    private val rootNode = DefaultMutableTreeNode("No file loaded")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true
        cellRenderer = FigmaTreeCellRenderer()
    }
    private val previewImageLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.CENTER
    }
    private val previewInfoLabel = JBLabel("Select a frame to preview").apply {
        horizontalAlignment = SwingConstants.CENTER
        foreground = JBUI.CurrentTheme.Label.disabledForeground()
        border = JBUI.Borders.empty(8)
    }

    init {
        border = JBUI.Borders.empty(4)
        add(buildHeader(), BorderLayout.NORTH)
        add(contentArea, BorderLayout.CENTER)

        setupTreeListener()
        TreeSpeedSearch.installOn(tree)

        showFilePicker()
    }

    private fun buildHeader(): JPanel {
        val header = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(8)
        }

        val statusLabel = JBLabel("\u25cf ${user.handle}").apply {
            font = font.deriveFont(Font.BOLD, 12f)
            foreground = java.awt.Color(0x2E, 0xA0, 0x43) // Green
        }

        val trialDays = TrialManager.daysRemaining()
        val trialLabel = JBLabel(
            if (trialDays > 0) "$trialDays days left" else "Trial expired"
        ).apply {
            foreground = if (trialDays <= 2) java.awt.Color(0xE0, 0x40, 0x40) else JBUI.CurrentTheme.Label.disabledForeground()
            font = font.deriveFont(11f)
        }

        val rightIcons = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
        }
        val helpLink = JBLabel("Help").apply {
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { showHelp() }
            })
        }
        val settingsLink = JBLabel("Settings").apply {
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { showSettings() }
            })
        }
        val disconnectLink = JBLabel("Disconnect").apply {
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    FigmaAuth.getInstance().clearToken()
                    onDisconnect()
                }
            })
        }
        rightIcons.add(helpLink)
        rightIcons.add(Box.createHorizontalStrut(10))
        rightIcons.add(settingsLink)
        rightIcons.add(Box.createHorizontalStrut(10))
        rightIcons.add(disconnectLink)

        val leftPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(statusLabel)
            add(Box.createHorizontalStrut(10))
            add(trialLabel)
        }
        header.add(leftPanel, BorderLayout.WEST)
        header.add(rightIcons, BorderLayout.EAST)

        return header
    }

    private fun buildTreeView(): JPanel {
        val panel = JPanel(BorderLayout())

        // Back button + file name header
        val treeHeader = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(4)
        }
        val backButton = JBLabel("\u2190 Files").apply {
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    showFilePicker()
                }
            })
        }
        val refreshButton = JBLabel("\u21bb").apply {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Refresh"
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val fileKey = currentFileKey ?: return
                    FigmaCache.getInstance().invalidate()
                    loadFile(fileKey)
                }
            })
        }
        treeHeader.add(backButton, BorderLayout.WEST)
        treeHeader.add(refreshButton, BorderLayout.EAST)

        // Splitter: tree + preview
        val splitter = JBSplitter(true, 0.6f).apply {
            border = JBUI.Borders.empty()
            firstComponent = JBScrollPane(tree)
            secondComponent = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.emptyTop(4)
                add(JBScrollPane(previewImageLabel).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
                add(previewInfoLabel, BorderLayout.SOUTH)
            }
        }

        // Action bar
        val trialExpired = TrialManager.isExpired()
        val generateButton = JButton(if (trialExpired) "Generate Code (Trial Expired)" else "Generate Code").apply {
            isEnabled = false
            if (trialExpired) toolTipText = "Upgrade to FigBridge Pro to continue generating code"
            addActionListener {
                if (TrialManager.isExpired()) {
                    com.intellij.openapi.ui.Messages.showWarningDialog(
                        project,
                        "Your 14-day trial has expired.\nUpgrade to FigBridge Pro to continue generating code and exporting assets.\n\nPreview and inspect remain free.",
                        "Trial Expired",
                    )
                } else {
                    generateCode()
                }
            }
        }
        // Enable button when a frame is selected
        tree.addTreeSelectionListener {
            val selected = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            val node = selected?.userObject as? FigmaNode
            generateButton.isEnabled = node != null &&
                node.type in listOf("FRAME", "COMPONENT", "COMPONENT_SET")
        }
        val cssButton = javax.swing.JToggleButton("CSS")
        val twButton = javax.swing.JToggleButton("Tailwind")
        val modeGroup = javax.swing.ButtonGroup().apply {
            add(cssButton)
            add(twButton)
        }
        when (FigBridgeSettings.getInstance().codeMode) {
            CodeMode.CUSTOM_CSS -> cssButton.isSelected = true
            CodeMode.TAILWIND -> twButton.isSelected = true
        }
        cssButton.addActionListener { FigBridgeSettings.getInstance().codeMode = CodeMode.CUSTOM_CSS }
        twButton.addActionListener { FigBridgeSettings.getInstance().codeMode = CodeMode.TAILWIND }

        val modePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(JBLabel("Output:  ").apply { foreground = JBUI.CurrentTheme.Label.disabledForeground() })
            add(cssButton)
            add(twButton)
        }
        val tokensButton = JButton("Tokens \u25be").apply {
            addActionListener { e ->
                val popup = JPopupMenu()
                popup.add(JMenuItem("Import JSON...").apply {
                    addActionListener { importTokensFromJson() }
                })
                popup.add(JMenuItem("Export via API").apply {
                    toolTipText = "Requires Enterprise Figma plan"
                    addActionListener { exportTokens() }
                })
                popup.show(e.source as java.awt.Component, 0, (e.source as java.awt.Component).height)
            }
        }
        val actionBar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = JBUI.Borders.emptyTop(8)
            add(generateButton)
            add(Box.createHorizontalStrut(8))
            add(tokensButton)
            add(Box.createHorizontalStrut(12))
            add(modePanel)
            add(Box.createHorizontalGlue())
        }

        panel.add(treeHeader, BorderLayout.NORTH)
        panel.add(splitter, BorderLayout.CENTER)
        panel.add(actionBar, BorderLayout.SOUTH)

        return panel
    }

    private fun showFilePicker() {
        currentFileKey = null
        filePickerPanel.refresh()
        contentArea.removeAll()
        contentArea.add(filePickerPanel, BorderLayout.CENTER)
        contentArea.revalidate()
        contentArea.repaint()
    }

    private fun showTreeView() {
        contentArea.removeAll()
        contentArea.add(buildTreeView(), BorderLayout.CENTER)
        contentArea.revalidate()
        contentArea.repaint()
    }

    private fun setupTreeListener() {
        tree.addTreeSelectionListener {
            val selected = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val figmaNode = selected.userObject as? FigmaNode ?: return@addTreeSelectionListener

            selectedNode = figmaNode

            if (figmaNode.type in listOf("FRAME", "COMPONENT", "COMPONENT_SET")) {
                val fileKey = currentFileKey ?: return@addTreeSelectionListener
                loadThumbnail(fileKey, figmaNode)
            }
        }
    }

    /** Public entry point to load a file by key (used by ConnectPanel URL flow) */
    fun loadFileByKey(fileKey: String) = loadFile(fileKey)

    private fun loadFile(fileKey: String) {
        currentFileKey = fileKey
        FigBridgeSettings.getInstance().state.lastFileKey = fileKey

        showTreeView()

        rootNode.removeAllChildren()
        rootNode.userObject = "Loading..."
        treeModel.reload()
        previewInfoLabel.text = "Loading file..."
        previewImageLabel.removeAll()
        previewImageLabel.layout = null
        previewImageLabel.text = null
        previewImageLabel.icon = AnimatedIcon.Default()

        scope.launch(Dispatchers.IO) {
            val cached = FigmaCache.getInstance().getCachedFile(fileKey)
            if (cached != null) {
                launch(Dispatchers.Swing) { populateTree(fileKey, cached) }
                return@launch
            }

            val result = FigmaClient.getInstance().getFile(fileKey)
            launch(Dispatchers.Swing) {
                when (result) {
                    is FigmaResult.Success -> {
                        FigmaCache.getInstance().cacheFile(fileKey, result.data)
                        populateTree(fileKey, result.data)
                    }
                    is FigmaResult.Error -> {
                        rootNode.removeAllChildren()
                        rootNode.userObject = "Error"
                        treeModel.reload()
                        previewInfoLabel.text = "Error: ${result.message}"
                        previewImageLabel.icon = null
                        previewImageLabel.text = null
                        // Show retry option
                        val retryButton = JButton("Retry").apply {
                            addActionListener { loadFile(fileKey) }
                        }
                        val errorPanel = JPanel().apply {
                            layout = BoxLayout(this, BoxLayout.Y_AXIS)
                            add(JBLabel("Could not load file: ${result.message}").apply {
                                alignmentX = CENTER_ALIGNMENT
                                foreground = java.awt.Color(0xE0, 0x40, 0x40)
                            })
                            add(Box.createVerticalStrut(8))
                            add(retryButton.apply { alignmentX = CENTER_ALIGNMENT })
                        }
                        previewImageLabel.layout = BorderLayout()
                        previewImageLabel.removeAll()
                        previewImageLabel.add(errorPanel, BorderLayout.CENTER)
                        previewImageLabel.revalidate()
                    }
                }
            }
        }
    }

    private fun populateTree(fileKey: String, file: FigmaFileResponse) {
        // Save styles for code generation
        currentFileStyles = file.styles
        // Save to recent files
        FigBridgeSettings.getInstance().addRecentFile(fileKey, file.name, file.thumbnailUrl)

        rootNode.removeAllChildren()
        rootNode.userObject = file.name

        for (page in file.document.children) {
            val pageNode = DefaultMutableTreeNode(page)
            for (frame in page.children) {
                pageNode.add(DefaultMutableTreeNode(frame))
            }
            rootNode.add(pageNode)
        }

        treeModel.reload()
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }

        previewInfoLabel.text = "${file.name} \u00b7 ${file.document.children.size} pages"
    }

    private fun loadThumbnail(fileKey: String, frame: FigmaNode) {
        val cached = FigmaCache.getInstance().getCachedThumbnail(frame.id)
        if (cached != null) {
            showThumbnail(cached, frame)
            return
        }

        previewImageLabel.icon = null
        val dims = frame.absoluteBoundingBox?.let { "${it.width.toInt()}\u00d7${it.height.toInt()}" } ?: ""
        previewInfoLabel.text = "Loading preview... $dims"

        scope.launch(Dispatchers.IO) {
            val result = FigmaClient.getInstance().getImage(fileKey, listOf(frame.id))
            when (result) {
                is FigmaResult.Success -> {
                    val imageUrl = result.data.images[frame.id]
                    if (imageUrl != null) {
                        try {
                            val image = ImageIO.read(URI(imageUrl).toURL())
                            if (image != null) {
                                FigmaCache.getInstance().cacheThumbnail(frame.id, image)
                                launch(Dispatchers.Swing) { showThumbnail(image, frame) }
                            }
                        } catch (_: Exception) {
                            launch(Dispatchers.Swing) { previewInfoLabel.text = "Preview unavailable" }
                        }
                    }
                }
                is FigmaResult.Error -> {
                    launch(Dispatchers.Swing) { previewInfoLabel.text = "Preview error: ${result.message}" }
                }
            }
        }
    }

    private fun generateCode() {
        val fileKey = currentFileKey ?: return
        val node = selectedNode ?: return
        val frameName = node.name
        val assetDir = FigBridgeSettings.getInstance().state.assetDir

        // Show animated spinner in preview area
        previewImageLabel.icon = AnimatedIcon.Default()
        previewImageLabel.text = "Generating code..."
        previewImageLabel.font = previewImageLabel.font.deriveFont(13f)
        previewImageLabel.foreground = JBUI.CurrentTheme.Label.disabledForeground()
        previewInfoLabel.text = frameName

        scope.launch(Dispatchers.IO) {
            // Fetch nodes + variables in parallel
            val nodesDeferred = async { FigmaClient.getInstance().getNodes(fileKey, listOf(node.id)) }
            val variablesDeferred = async {
                // Use cached variables if available
                val cached = FigmaCache.getInstance().getCachedVariables(fileKey)
                if (cached != null) return@async cached
                val varsResult = FigmaClient.getInstance().getVariables(fileKey)
                when (varsResult) {
                    is FigmaResult.Success -> {
                        FigmaCache.getInstance().cacheVariables(fileKey, varsResult.data.meta.variables)
                        varsResult.data.meta.variables
                    }
                    is FigmaResult.Error -> emptyMap()
                }
            }

            val result = nodesDeferred.await()
            val variables = variablesDeferred.await()

            when (result) {
                is FigmaResult.Success -> {
                    val fullNode = result.data.nodes[node.id]?.document
                    if (fullNode == null) {
                        launch(Dispatchers.Swing) { previewInfoLabel.text = "Error: Node data not found" }
                        return@launch
                    }

                    val mode = FigBridgeSettings.getInstance().codeMode
                    val generated = CodeGenerator.generate(fullNode, assetDir, variables, currentFileStyles, mode)

                    launch(Dispatchers.Swing) {
                        showCodeView(generated, fileKey, frameName)
                    }
                }
                is FigmaResult.Error -> {
                    launch(Dispatchers.Swing) { previewInfoLabel.text = "Error: ${result.message}" }
                }
            }
        }
    }

    private fun showCodeView(generated: GeneratedResult, fileKey: String, frameName: String) {
        codePanel?.dispose()
        val projectDir = project.basePath ?: return
        val panel = CodePanel(project, { showTreeView() }) {
            // "Get Assets" callback
            exportAssets(fileKey, generated.assets, java.io.File(projectDir), it)
        }
        panel.showCode(generated.html, generated.css, generated.fontsLink, generated.assets.size, frameName)
        codePanel = panel

        contentArea.removeAll()
        contentArea.add(panel, BorderLayout.CENTER)
        contentArea.revalidate()
        contentArea.repaint()
    }

    private fun exportAssets(fileKey: String, assets: List<AssetInfo>, projectDir: java.io.File, statusCallback: (String) -> Unit) {
        if (TrialManager.isExpired()) {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                project,
                "Your 14-day trial has expired.\nUpgrade to FigBridge Pro to export assets.",
                "Trial Expired",
            )
            return
        }
        statusCallback("Downloading ${assets.size} assets...")
        scope.launch(Dispatchers.IO) {
            val exportResult = AssetExporter.export(fileKey, assets, projectDir)
            val status = buildString {
                append("${exportResult.saved} saved")
                if (exportResult.skipped > 0) append(", ${exportResult.skipped} skipped")
                if (exportResult.errors.isNotEmpty()) append(", ${exportResult.errors.size} errors")
            }
            launch(Dispatchers.Swing) { statusCallback(status) }
        }
    }

    private fun showSettings() {
        contentArea.removeAll()
        contentArea.add(SettingsPanel { showTreeViewOrFilePicker() }, BorderLayout.CENTER)
        contentArea.revalidate()
        contentArea.repaint()
    }

    private fun showHelp() {
        contentArea.removeAll()
        contentArea.add(HelpPanel { showTreeViewOrFilePicker() }, BorderLayout.CENTER)
        contentArea.revalidate()
        contentArea.repaint()
    }

    private fun showTreeViewOrFilePicker() {
        if (currentFileKey != null) {
            showTreeView()
        } else {
            showFilePicker()
        }
    }

    private fun importTokensFromJson() {
        val fileChooser = javax.swing.JFileChooser().apply {
            dialogTitle = "Import Figma Variables JSON"
            fileFilter = javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json")
            currentDirectory = project.basePath?.let { java.io.File(it) }
        }

        if (fileChooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return

        try {
            val jsonString = fileChooser.selectedFile.readText()
            val result = TokenImporter.parseJson(jsonString)
            val css = TokenExporter.generateTokensCss(result.variables, result.collections)

            val tokenCount = result.variables.size
            val collectionCount = result.collections.size

            // Write tokens.css to project directory
            val projectDir = project.basePath
            if (projectDir != null) {
                val tokenFileName = FigBridgeSettings.getInstance().state.tokenFile
                val tokenFile = java.io.File(projectDir, tokenFileName)
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                        val vf = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                            .refreshAndFindFileByIoFile(tokenFile.parentFile)
                        if (vf != null) {
                            tokenFile.writeText(css)
                            vf.refresh(false, false)
                            com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                                .refreshAndFindFileByIoFile(tokenFile)
                        }
                    }
                    com.intellij.openapi.ui.Messages.showInfoMessage(
                        project,
                        "$tokenCount tokens from $collectionCount collections saved to $tokenFileName",
                        "Import Tokens",
                    )
                }
            }
        } catch (e: Exception) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                project,
                "Could not parse JSON: ${e.message}",
                "Import Tokens",
            )
        }
    }

    private fun exportTokens() {
        val fileKey = currentFileKey ?: return

        // Show loading feedback immediately
        previewImageLabel.icon = AnimatedIcon.Default()
        previewImageLabel.text = "Loading tokens..."
        previewImageLabel.font = previewImageLabel.font.deriveFont(13f)
        previewImageLabel.foreground = JBUI.CurrentTheme.Label.disabledForeground()
        previewInfoLabel.text = "Fetching Figma Variables..."

        scope.launch(Dispatchers.IO) {
            val varsResult = FigmaClient.getInstance().getVariables(fileKey)

            launch(Dispatchers.Swing) {
                when (varsResult) {
                    is FigmaResult.Success -> {
                        val meta = varsResult.data.meta
                        FigmaCache.getInstance().cacheVariables(fileKey, meta.variables)
                        val css = TokenExporter.generateTokensCss(meta.variables, meta.variableCollections)
                        showTokenCodeView(css)
                    }
                    is FigmaResult.Error -> {
                        previewImageLabel.icon = null
                        previewImageLabel.text = null
                        previewInfoLabel.text = "Token error: ${varsResult.message}"
                        com.intellij.openapi.ui.Messages.showErrorDialog(
                            project,
                            "Could not load variables: ${varsResult.message}\n\nVariables require a Professional or Enterprise Figma plan.",
                            "Export Tokens",
                        )
                    }
                }
            }
        }
    }

    private fun showTokenCodeView(css: String) {
        codePanel?.dispose()
        val panel = CodePanel(project, { showTreeView() })
        panel.showCode(css, "Design Tokens")
        codePanel = panel

        contentArea.removeAll()
        contentArea.add(panel, BorderLayout.CENTER)
        contentArea.revalidate()
        contentArea.repaint()
    }

    private fun showThumbnail(image: java.awt.image.BufferedImage, frame: FigmaNode) {
        val panelWidth = (this.width - 32).coerceAtLeast(200)
        val maxHeight = 250

        var scaledWidth = panelWidth
        var scaledHeight = (image.height.toFloat() / image.width * scaledWidth).toInt()

        if (scaledHeight > maxHeight) {
            scaledHeight = maxHeight
            scaledWidth = (image.width.toFloat() / image.height * scaledHeight).toInt()
        }

        val scaled = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)
        previewImageLabel.icon = ImageIcon(scaled)

        val dims = frame.absoluteBoundingBox?.let { "${it.width.toInt()}\u00d7${it.height.toInt()}" } ?: ""
        val layers = frame.children.size
        previewInfoLabel.text = "${frame.name} \u00b7 $dims \u00b7 $layers layers"
    }
}
