package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project

class PluginDetector {
    private val packageJsonDetector = PackageJsonDetector()
    private val scriptReferenceDetector = ScriptReferenceDetector()

    suspend fun detect(project: Project, plugin: AlpinePlugin): Boolean {
        val detectedCheaply = smartReadAction(project) {
            packageJsonDetector.detect(project, plugin) || plugin.performDetection(project)
        }

        return detectedCheaply || scriptReferenceDetector.detect(project, plugin)
    }
}
