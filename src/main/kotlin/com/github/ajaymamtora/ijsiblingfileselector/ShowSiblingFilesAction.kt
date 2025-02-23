package com.github.ajaymamtora.ijsiblingfileselector

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
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
            .filter { !it.isDirectory }
            .sortedBy { it.name }

        val listModel = DefaultListModel<VirtualFile>().apply {
            siblingFiles.forEach { addElement(it) }
        }

        val fileList = JBList(listModel).apply {
            cellRenderer = ListCellRenderer { list, value, index, isSelected, _ ->
                JLabel(value.name, value.fileType.icon, JLabel.LEFT).apply {
                    isOpaque = true
                    background = if (isSelected) list.selectionBackground else list.background
                    foreground = if (isSelected) list.selectionForeground else list.foreground
                }
            }
        }

        val searchField = JBTextField().apply {
            emptyText.text = "Search files..."
            addKeyListener(object : KeyAdapter() {
                override fun keyReleased(e: KeyEvent) {
                    val query = text.lowercase()
                    listModel.clear()
                    siblingFiles.filter { it.name.lowercase().contains(query) }
                        .forEach { listModel.addElement(it) }
                }
            })
        }

        val popup: JBPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(
                JPanel(BorderLayout()).apply {
                    add(searchField, BorderLayout.NORTH)
                    add(JBScrollPane(fileList), BorderLayout.CENTER)
                },
                searchField
            )
            .setTitle("Select Sibling File")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()

        fileList.addListSelectionListener {
            if (!fileList.isSelectionEmpty) {
                val selectedFile = fileList.selectedValue
                openFile(project, selectedFile)
                popup.closeOk(null)
            }
        }

        popup.showCenteredInCurrentWindow(project)
    }

    private fun openFile(project: Project, file: VirtualFile) {
        FileEditorManager.getInstance(project).openFile(file, true)
    }
}
