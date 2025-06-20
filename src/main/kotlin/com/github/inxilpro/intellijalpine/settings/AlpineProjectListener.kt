package com.github.inxilpro.intellijalpine.settings

import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class AlpineProjectListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        AlpinePluginRegistry.Companion.getInstance().cleanup(project)
    }
}