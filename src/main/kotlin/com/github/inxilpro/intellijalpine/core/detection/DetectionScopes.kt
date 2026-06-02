package com.github.inxilpro.intellijalpine.core.detection

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope

internal object DetectionScopes {
    fun projectScopeExcludingNodeModules(project: Project): GlobalSearchScope {
        return object : DelegatingGlobalSearchScope(GlobalSearchScope.projectScope(project)) {
            override fun contains(file: VirtualFile): Boolean {
                return super.contains(file) && !isInNodeModules(file)
            }
        }
    }

    private fun isInNodeModules(file: VirtualFile): Boolean {
        var current: VirtualFile? = file.parent
        while (current != null) {
            if (current.name == "node_modules") return true
            current = current.parent
        }
        return false
    }
}
