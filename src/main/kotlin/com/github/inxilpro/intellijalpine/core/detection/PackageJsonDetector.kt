package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

private val LOG = logger<PackageJsonDetector>()

class PackageJsonDetector : DetectionStrategy {
    override fun detect(project: Project, plugin: AlpinePlugin): Boolean {
        if (DumbService.isDumb(project)) return false
        val packageJsonFiles = FilenameIndex.getVirtualFilesByName(
            "package.json",
            GlobalSearchScope.projectScope(project)
        )

        return packageJsonFiles.any { virtualFile ->
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile is JsonFile) {
                val rootObject = psiFile.topLevelValue as? JsonObject
                val dependencies = rootObject?.findProperty("dependencies")?.value as? JsonObject
                val devDependencies = rootObject?.findProperty("devDependencies")?.value as? JsonObject

                hasPluginDependency(plugin, dependencies) || hasPluginDependency(plugin, devDependencies)
            } else {
                false
            }
        }
    }

    private fun hasPluginDependency(plugin: AlpinePlugin, dependencies: JsonObject?): Boolean {
        return plugin.getPackageNamesForDetection().any { packageName ->
            dependencies?.findProperty(packageName) != null
        }
    }
}