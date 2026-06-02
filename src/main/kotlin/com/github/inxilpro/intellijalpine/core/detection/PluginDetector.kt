package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.openapi.project.Project

class PluginDetector : DetectionStrategy {
    private val strategies: List<DetectionStrategy> = listOf(
        PackageJsonDetector(),
        ScriptReferenceDetector()
    )

    override fun detect(project: Project, plugins: List<AlpinePlugin>): Set<AlpinePlugin> {
        if (plugins.isEmpty()) return emptySet()

        val detected = mutableSetOf<AlpinePlugin>()

        for (strategy in strategies) {
            val remaining = plugins.filter { it !in detected }
            if (remaining.isEmpty()) break
            detected += strategy.detect(project, remaining)
        }

        for (plugin in plugins) {
            if (plugin !in detected && plugin.performDetection(project)) {
                detected += plugin
            }
        }

        return detected
    }
}
