package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

class ScriptReferenceDetector : DetectionStrategy {
    override fun detect(project: Project, plugin: AlpinePlugin): Boolean {
        return hasScriptTagReferences(project, plugin) || hasImportReferences(project, plugin)
    }

    private fun hasScriptTagReferences(project: Project, plugin: AlpinePlugin): Boolean {
        val htmlFiles = mutableListOf<VirtualFile>()
        val extensions = listOf("html", "htm", "php", "twig", "djhtml", "jinja", "astro")

        for (extension in extensions) {
            htmlFiles.addAll(
                FilenameIndex.getAllFilesByExt(project, extension, GlobalSearchScope.projectScope(project))
            )
        }

        return htmlFiles.any { virtualFile ->
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)

            when {
                // If the IDE knows it's XML, use structured data
                psiFile is XmlFile -> PsiTreeUtil.collectElementsOfType(psiFile, XmlTag::class.java)
                    .filter { it.name.equals("script", ignoreCase = true) }
                    .any { scriptTag ->
                        val src = scriptTag.getAttributeValue("src")
                        src != null && containsPackageReference(src, plugin)
                    }

                // Otherwise, just look at the contents of the file
                else -> {
                    try {
                        val content = String(virtualFile.contentsToByteArray())
                        hasScriptTagsInContent(content, plugin)
                    } catch (_: Exception) {
                        false
                    }
                }
            }
        }
    }

    private fun hasImportReferences(project: Project, plugin: AlpinePlugin): Boolean {
        val jsExtensions = listOf("js", "ts", "mjs")
        val jsFiles = mutableListOf<VirtualFile>()

        for (extension in jsExtensions) {
            jsFiles.addAll(
                FilenameIndex.getAllFilesByExt(project, extension, GlobalSearchScope.projectScope(project))
            )
        }

        return jsFiles.any { virtualFile ->
            try {
                val content = String(virtualFile.contentsToByteArray())
                hasImportStatements(content, plugin)
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun hasScriptTagsInContent(content: String, plugin: AlpinePlugin): Boolean {
        val scriptTagRegex = Regex("<script[^>]*src=['\"]([^'\"]*)['\"][^>]*>", RegexOption.IGNORE_CASE)
        return scriptTagRegex.findAll(content).any { match ->
            val src = match.groupValues[1]
            containsPackageReference(src, plugin)
        }
    }

    private fun hasImportStatements(content: String, plugin: AlpinePlugin): Boolean {
        val importPatterns = plugin.getPackageNamesForDetection().flatMap { packageName ->
            val escapedPackageName = Regex.escape(packageName)
            listOf(
                "import\\s+.*\\s+from\\s+['\"]$escapedPackageName['\"]",
                "import\\s+['\"]$escapedPackageName['\"]",
                "require\\s*\\(\\s*['\"]$escapedPackageName['\"]\\s*\\)",
                "from\\s+['\"]$escapedPackageName['\"]\\s+import"
            )
        }

        return importPatterns.any { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(content)
        }
    }

    private fun containsPackageReference(src: String, plugin: AlpinePlugin): Boolean {
        return plugin.getPackageNamesForDetection().any { packageName ->
            src.contains(packageName, ignoreCase = true)
        }
    }
}