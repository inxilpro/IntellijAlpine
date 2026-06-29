package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext

class ScriptReferenceDetector : DetectionStrategy {
    private companion object {
        val HTML_EXTENSIONS = setOf("html", "htm", "php", "twig", "djhtml", "jinja", "astro")
        val JS_EXTENSIONS = setOf("js", "ts", "mjs")

        // "alpine" appears in nearly every file of an Alpine project, so it does nothing to
        // narrow the candidate set. The precise package name is still confirmed per file below.
        val GENERIC_TOKENS = setOf("alpine")

        const val MIN_TOKEN_LENGTH = 4
        const val MAX_FILE_SIZE = 1_000_000L

        private val NON_ALPHANUMERIC = Regex("[^a-zA-Z0-9]+")
        private val SCRIPT_TAG_REGEX = Regex("<script[^>]*src=['\"]([^'\"]*)['\"][^>]*>", RegexOption.IGNORE_CASE)
    }

    override fun detect(project: Project, plugins: List<AlpinePlugin>): Set<AlpinePlugin> {
        if (plugins.isEmpty() || DumbService.isDumb(project)) return emptySet()

        val tokens = plugins.flatMapTo(mutableSetOf()) { searchTokensFor(it) }
        if (tokens.isEmpty()) return emptySet()

        val candidates = findCandidateFiles(project, tokens)
        if (candidates.isEmpty()) return emptySet()

        val detected = mutableSetOf<AlpinePlugin>()
        val remaining = plugins.toMutableList()

        for (file in candidates) {
            if (remaining.isEmpty()) break

            val extension = file.extension?.lowercase()
            val isHtml = extension in HTML_EXTENSIONS
            val isJs = extension in JS_EXTENSIONS
            if (!isHtml && !isJs) continue
            if (file.length > MAX_FILE_SIZE || file.fileType.isBinary) continue

            val content = try {
                VfsUtilCore.loadText(file)
            } catch (_: Exception) {
                continue
            }

            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val plugin = iterator.next()
                val matched =
                    if (isHtml) hasScriptTagsInContent(content, plugin) else hasImportStatements(content, plugin)
                if (matched) {
                    detected += plugin
                    iterator.remove()
                }
            }
        }

        return detected
    }

    private fun findCandidateFiles(project: Project, tokens: Set<String>): Set<VirtualFile> {
        val helper = PsiSearchHelper.getInstance(project)
        val scope = DetectionScopes.projectScopeExcludingNodeModules(project)
        val candidates = mutableSetOf<VirtualFile>()

        for (token in tokens) {
            helper.processCandidateFilesForText(scope, UsageSearchContext.ANY, false, token) { file ->
                candidates.add(file)
                true
            }
        }

        return candidates
    }

    private fun searchTokensFor(plugin: AlpinePlugin): List<String> {
        return plugin.getPackageNamesForDetection().flatMap { packageName ->
            val tokens = packageName.split(NON_ALPHANUMERIC).filter { it.isNotEmpty() }
            val distinctive = tokens.filter { it.length >= MIN_TOKEN_LENGTH && it.lowercase() !in GENERIC_TOKENS }
            distinctive.ifEmpty { listOfNotNull(tokens.maxByOrNull { it.length }) }
        }
    }

    private fun hasScriptTagsInContent(content: String, plugin: AlpinePlugin): Boolean {
        return SCRIPT_TAG_REGEX.findAll(content).any { match ->
            containsPackageReference(match.groupValues[1], plugin)
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
