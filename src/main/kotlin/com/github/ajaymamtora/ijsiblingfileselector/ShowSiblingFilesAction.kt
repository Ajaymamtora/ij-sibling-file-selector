package com.github.ajaymamtora.ijsiblingfileselector

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.ui.components.JBList
import javax.swing.DefaultListModel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileEditorManager
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer

class ShowSiblingFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val directory = currentFile.parent ?: return

        // Get all sibling files (excluding directories)
        val siblingFiles = directory.children
            .filter { !it.isDirectory }
            .sortedBy { it.name }
            .toList()

        // Create list model and populate it
        val listModel = DefaultListModel<VirtualFile>()
        siblingFiles.forEach { listModel.addElement(it) }

        // Create JBList with custom renderer
        val fileList = JBList(listModel).apply {
            cellRenderer = object : ListCellRenderer<VirtualFile> {
                override fun getListCellRendererComponent(
                    list: JList<out VirtualFile>?,
                    value: VirtualFile?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    return com.intellij.ui.components.JBLabel().apply {
                        text = value?.name ?: ""
                        icon = value?.fileType?.icon
                        background = if (isSelected) list?.selectionBackground else list?.background
                        foreground = if (isSelected) list?.selectionForeground else list?.foreground
                        isOpaque = isSelected
                    }
                }
            }
        }

        // Show popup with search
        PopupChooserBuilder(fileList)
            .setTitle("Select Sibling File")
            .setItemChosenCallback(Runnable {
                val selectedFile = fileList.selectedValue
                if (selectedFile != null) {
                    openFile(project, selectedFile)
                }
            })
            .setMovable(true)
            .createPopup()
            .showCenteredInCurrentWindow(project)
    }

    private fun openFile(project: Project, file: VirtualFile) {
        FileEditorManager.getInstance(project).openFile(file, true)
    }
}
