package com.github.inxilpro.intellijalpine.settings

import com.github.inxilpro.intellijalpine.core.AlpinePluginAutoDetector
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AlpineProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        AlpinePluginAutoDetector.getInstance(project).start()
    }
}
