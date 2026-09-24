package com.github.inxilpro.intellijalpine.settings

import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AlpineProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Detection relies on indexes, which are not ready while the project is still being opened.
        smartReadAction(project) {
            AlpinePluginRegistry.instance.checkAndAutoEnablePlugins(project)
        }
    }
}