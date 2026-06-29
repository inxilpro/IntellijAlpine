package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex

class PackageJsonDetector : DetectionStrategy {
    override fun detect(project: Project, plugins: List<AlpinePlugin>): Set<AlpinePlugin> {
        if (plugins.isEmpty() || DumbService.isDumb(project)) return emptySet()

        val packageJsonFiles = FilenameIndex.getVirtualFilesByName(
            "package.json",
            DetectionScopes.projectScopeExcludingNodeModules(project)
        )
        if (packageJsonFiles.isEmpty()) return emptySet()

        val psiManager = PsiManager.getInstance(project)
        val detected = mutableSetOf<AlpinePlugin>()

        for (virtualFile in packageJsonFiles) {
            val psiFile = psiManager.findFile(virtualFile) as? JsonFile ?: continue
            val rootObject = psiFile.topLevelValue as? JsonObject ?: continue
            val dependencies = rootObject.findProperty("dependencies")?.value as? JsonObject
            val devDependencies = rootObject.findProperty("devDependencies")?.value as? JsonObject

            for (plugin in plugins) {
                if (plugin in detected) continue
                if (hasPluginDependency(plugin, dependencies) || hasPluginDependency(plugin, devDependencies)) {
                    detected += plugin
                }
            }
        }

        return detected
    }

    private fun hasPluginDependency(plugin: AlpinePlugin, dependencies: JsonObject?): Boolean {
        return plugin.getPackageNamesForDetection().any { packageName ->
            dependencies?.findProperty(packageName) != null
        }
    }
}
