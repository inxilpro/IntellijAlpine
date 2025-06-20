package com.github.inxilpro.intellijalpine

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class AlpineProjectListener : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        AlpineAjaxDetector.cleanup(project)
    }
}
