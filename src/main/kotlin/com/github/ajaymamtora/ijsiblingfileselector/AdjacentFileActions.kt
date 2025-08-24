package com.github.ajaymamtora.ijsiblingfileselector

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileEditorManager

class SelectNextFileAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val adjacentFile = findAdjacentFile(project, currentFile, next = true)
        adjacentFile?.let { openFile(project, it) }
    }
}

class SelectPreviousFileAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val adjacentFile = findAdjacentFile(project, currentFile, next = false)
        adjacentFile?.let { openFile(project, it) }
    }
}

private fun findAdjacentFile(project: Project, currentFile: VirtualFile, next: Boolean): VirtualFile? {
    val directory = currentFile.parent ?: return null
    val files = directory.children
        .filter { !it.isDirectory }
        .sortedBy { it.name }

    val currentIndex = files.indexOf(currentFile)
    if (currentIndex == -1) return null

    return if (next) {
        files.getOrNull(currentIndex + 1) ?: files.firstOrNull()
    } else {
        files.getOrNull(currentIndex - 1) ?: files.lastOrNull()
    }
}

private fun openFile(project: Project, file: VirtualFile) {
    FileEditorManager.getInstance(project).openFile(file, true)
}
