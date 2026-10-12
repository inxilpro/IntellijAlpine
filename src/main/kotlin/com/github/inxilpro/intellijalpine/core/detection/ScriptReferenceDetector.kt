package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

class ScriptReferenceDetector {
    companion object {
        private val MARKUP_EXTENSIONS = setOf("html", "htm", "php", "twig", "djhtml", "jinja", "astro")
        private val SCRIPT_EXTENSIONS = setOf("js", "ts", "mjs")

        private val SCRIPT_TAG_REGEX = Regex("<script[^>]*src=['\"]([^'\"]*)['\"][^>]*>", RegexOption.IGNORE_CASE)
    }

    suspend fun detect(project: Project, plugin: AlpinePlugin): Boolean {
        val packageNames = plugin.getPackageNamesForDetection()
        if (packageNames.isEmpty()) {
            return false
        }

        // Only the index lookup holds the read lock. Loading and matching file contents happens
        // outside of it so that a large project can't block pending write actions.
        val candidates = smartReadAction(project) { findCandidateFiles(project, packageNames) }
        if (candidates.isEmpty()) {
            return false
        }

        val importRegex = importRegexFor(packageNames)

        return withContext(Dispatchers.IO) {
            candidates.any { file ->
                currentCoroutineContext().ensureActive()
                val content = loadText(file) ?: return@any false
                when (file.extension?.lowercase()) {
                    in MARKUP_EXTENSIONS -> hasScriptTagReference(content, packageNames)
                    in SCRIPT_EXTENSIONS -> importRegex.containsMatchIn(content)
                    else -> false
                }
            }
        }
    }

    // The word index lets us skip every file that can't possibly mention the package, which is
    // nearly all of them, instead of reading the whole project from disk.
    private fun findCandidateFiles(project: Project, packageNames: List<String>): Set<VirtualFile> {
        val scope = GlobalSearchScope.projectScope(project)
        val helper = PsiSearchHelper.getInstance(project)
        val candidates = mutableSetOf<VirtualFile>()

        for (packageName in packageNames) {
            helper.processCandidateFilesForText(scope, UsageSearchContext.ANY, false, packageName) { file ->
                val extension = file.extension?.lowercase()
                if (extension in MARKUP_EXTENSIONS || extension in SCRIPT_EXTENSIONS) {
                    candidates.add(file)
                }
                true
            }
        }

        return candidates
    }

    private fun loadText(file: VirtualFile): CharSequence? {
        if (!file.isValid) {
            return null
        }

        return try {
            VfsUtilCore.loadText(file)
        } catch (_: IOException) {
            null
        }
    }

    private fun hasScriptTagReference(content: CharSequence, packageNames: List<String>): Boolean {
        return SCRIPT_TAG_REGEX.findAll(content).any { match ->
            val src = match.groupValues[1]
            packageNames.any { src.contains(it, ignoreCase = true) }
        }
    }

    private fun importRegexFor(packageNames: List<String>): Regex {
        val names = packageNames.joinToString("|") { Regex.escape(it) }

        return Regex("""\b(?:from|import|require)\s*\(?\s*['"](?:$names)['"]""", RegexOption.IGNORE_CASE)
    }
}
