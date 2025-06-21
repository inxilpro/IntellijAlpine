package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.openapi.project.Project

interface DetectionStrategy {
    fun detect(project: Project, plugin: AlpinePlugin): Boolean
}