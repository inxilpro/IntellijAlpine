package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.openapi.project.Project

class PluginDetector: DetectionStrategy {
    private val strategies: List<DetectionStrategy> = listOf(
        PackageJsonDetector(),
        ScriptReferenceDetector()
    )
    
    override fun detect(project: Project, plugin: AlpinePlugin): Boolean {
        return strategies.any { it.detect(project, plugin) } || plugin.performDetection(project)
    }
}