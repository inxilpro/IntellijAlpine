package com.github.inxilpro.intellijalpine.services

import com.intellij.openapi.project.Project
import com.github.inxilpro.intellijalpine.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
