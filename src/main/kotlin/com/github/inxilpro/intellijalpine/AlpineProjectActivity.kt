package com.github.inxilpro.intellijalpine

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AlpineProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        AlpineAjaxDetector.checkAndAutoEnable(project)
    }
}
