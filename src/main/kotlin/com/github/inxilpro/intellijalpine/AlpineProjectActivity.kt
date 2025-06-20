package com.github.inxilpro.intellijalpine

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AlpineProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        readAction {
            AlpinePluginRegistry.getInstance().checkAndAutoEnablePlugins(project)
        }
    }
}
