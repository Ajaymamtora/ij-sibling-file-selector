package com.github.ajaymamtora.ijsiblingfileselector

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import javax.swing.BorderFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class ShowSiblingFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val directory = currentFile.parent ?: return

        val siblingFiles = directory.children
            .filter { !it.isDirectory && it != currentFile } // Exclude the current file
            .sortedBy { it.name }

        val listModel = DefaultListModel<VirtualFile>().apply {
            siblingFiles.forEach { addElement(it) }
        }

        val fileList = JBList(listModel).apply {
            cellRenderer = ListCellRenderer { list, value, _, isSelected, _ ->
                JLabel(value.name, value.fileType.icon, JLabel.LEFT).apply {
                    isOpaque = true
                    background = if (isSelected) list.selectionBackground else list.background
                    foreground = if (isSelected) list.selectionForeground else list.foreground
                }
            }
            selectedIndex = 0
        }

        var popup: JBPopup? = null
        var isCtrlWPressed = false

        val searchField = JBTextField().apply {
            emptyText.text = "Search files..."
            addKeyListener(object : KeyAdapter() {
                override fun keyReleased(e: KeyEvent) {
                    // Reset CTRL+W state if any other key is pressed or if the operation is done
                    if (e.keyCode != KeyEvent.VK_CONTROL && e.keyCode != KeyEvent.VK_W) {
                        if (!(isCtrlWPressed && (e.keyCode == KeyEvent.VK_S || e.keyCode == KeyEvent.VK_V))) {
                            isCtrlWPressed = false
                        }
                    }

                    // Only update search results if it's not a navigation key
                    if (e.keyCode !in arrayOf(
                            KeyEvent.VK_UP,
                            KeyEvent.VK_DOWN,
                            KeyEvent.VK_ENTER,
                            KeyEvent.VK_CONTROL,
                            KeyEvent.VK_W,
                            KeyEvent.VK_S,
                            KeyEvent.VK_V
                        )) {
                        val query = text.lowercase()
                        listModel.clear()
                        siblingFiles.filter { it.name.lowercase().contains(query) }
                            .forEach { listModel.addElement(it) }
                        if (listModel.size() > 0) {
                            fileList.selectedIndex = 0
                        }
                    }
                }

                override fun keyPressed(e: KeyEvent) {
                    // Track CTRL+W combination
                    if (e.isControlDown && e.keyCode == KeyEvent.VK_W) {
                        isCtrlWPressed = true
                        e.consume()
                        return
                    }

                    // Handle split commands after CTRL+W is pressed
                    if (isCtrlWPressed) {
                        when (e.keyCode) {
                            KeyEvent.VK_S -> {
                                // S key after CTRL+W (without requiring CTRL to be held)
                                e.consume()
                                isCtrlWPressed = false
                                fileList.selectedValue?.let { selectedFile ->
                                    openFileInSplit(project, selectedFile, false) // false for vertical split
                                    popup?.dispose()
                                }
                                return
                            }
                            KeyEvent.VK_V -> {
                                // V key after CTRL+W (without requiring CTRL to be held)
                                e.consume()
                                isCtrlWPressed = false
                                fileList.selectedValue?.let { selectedFile ->
                                    openFileInSplit(project, selectedFile, true) // true for horizontal split
                                    popup?.dispose()
                                }
                                return
                            }
                        }
                    }

                    // Regular navigation keys
                    when (e.keyCode) {
                        KeyEvent.VK_DOWN -> {
                            e.consume() // Prevent the search field from handling the event
                            if (listModel.size() > 0) {
                                // Wrap around to the top when reaching the bottom
                                val nextIndex = (fileList.selectedIndex + 1) % listModel.size()
                                fileList.selectedIndex = nextIndex
                                fileList.ensureIndexIsVisible(nextIndex)
                            }
                        }
                        KeyEvent.VK_UP -> {
                            e.consume() // Prevent the search field from handling the event
                            if (listModel.size() > 0) {
                                // Wrap around to the bottom when reaching the top
                                val prevIndex = (fileList.selectedIndex - 1 + listModel.size()) % listModel.size()
                                fileList.selectedIndex = prevIndex
                                fileList.ensureIndexIsVisible(prevIndex)
                            }
                        }
                        KeyEvent.VK_ENTER -> {
                            e.consume() // Prevent the search field from handling the event
                            fileList.selectedValue?.let { selectedFile ->
                                openFile(project, selectedFile)
                                popup?.dispose() // Close the popup after file selection
                            }
                        }
                    }
                }
            })
        }

        // Calculate the width needed for the longest filename
        val fontMetrics = fileList.getFontMetrics(fileList.font)
        val longestFilename = siblingFiles.maxByOrNull { it.name.length }?.name ?: ""

        // Add some extra width for the icon and padding
        val fileNameWidth = fontMetrics.stringWidth(longestFilename) + 50

        // Minimum width to prevent tiny popups for short filenames
        val minWidth = 300
        val preferredWidth = maxOf(fileNameWidth, minWidth)

        // Create the panel with calculated dimensions
        val contentPanel = JPanel(BorderLayout()).apply {
            // Add padding around components to match JetBrains UI style
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            add(searchField, BorderLayout.NORTH)

            // Add a small gap between the search field and the list
            val listScrollPane = JBScrollPane(fileList)
            add(listScrollPane, BorderLayout.CENTER)

            preferredSize = Dimension(preferredWidth, 400)
        }

        // Style the search field to match IDE look
        searchField.border = BorderFactory.createCompoundBorder(
            searchField.border,
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )

        // Create the popup and store the reference
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, searchField)
            .setTitle("Select Sibling File")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()

        popup.showCenteredInCurrentWindow(project)
    }

    private fun openFile(project: Project, file: VirtualFile) {
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    private fun openFileInSplit(project: Project, file: VirtualFile, isHorizontal: Boolean) {
        val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
        val currentWindow = fileEditorManager.currentWindow

        // Split the window based on the direction
        val newWindow = if (isHorizontal) {
            // SPLIT_HORIZONTAL = 1
            currentWindow?.split(1, true, file, true)
        } else {
            // SPLIT_VERTICAL = 0
            currentWindow?.split(0, true, file, true)
        }

        // Ensure the file is opened in the new split
        if (newWindow != null) {
            newWindow.setAsCurrentWindow(true)
            fileEditorManager.openFile(file, focusEditor = true, searchForOpen = true)
        } else {
            // Fallback if split fails for some reason
            openFile(project, file)
        }
    }
}
