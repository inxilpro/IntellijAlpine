package com.github.inxilpro.intellijalpine.plugins

import com.github.inxilpro.intellijalpine.attributes.AttributeInfo
import com.github.inxilpro.intellijalpine.completion.AutoCompleteSuggestions
import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.apache.commons.lang3.tuple.MutablePair

class AlpineWizardPlugin : AlpinePlugin {

    override fun getPluginName(): String = "alpine-wizard"

    override fun getPackageDisplayName(): String = "alpine-wizard"

    override fun getPackageNamesForDetection(): List<String> = listOf(
        "alpine-wizard",
        "@glhd/alpine-wizard"
    )

    override fun getTypeText(info: AttributeInfo): String? {
        if ("x-wizard:" == info.prefix) {
            return when (info.name) {
                "step" -> "Define wizard step"
                "if" -> "Conditional wizard step"
                "title" -> "Set step title"
                else -> "Alpine Wizard directive"
            }
        }

        return when (info.attribute) {
            "x-wizard:step" -> "Define wizard step"
            "x-wizard:if" -> "Conditional wizard step"
            "x-wizard:title" -> "Set step title"
            else -> null
        }
    }

    override fun injectJsContext(context: MutablePair<String, String>): MutablePair<String, String> {
        val wizardMagics = """
            class AlpineWizardStep {
                /** @type {HTMLElement} */ el;
                /** @type {string} */ title;
                /** @type {boolean} */ is_applicable;
                /** @type {boolean} */ is_complete;
            }

            class AlpineWizardProgress {
                /** @type {number} */ current;
                /** @type {number} */ total;
                /** @type {number} */ complete;
                /** @type {number} */ incomplete;
                /** @type {string} */ percentage;
                /** @type {number} */ percentage_int;
                /** @type {number} */ percentage_float;
            }

            class AlpineWizardMagic {
                /** @returns {AlpineWizardStep} */ current() {}
                /** @returns {AlpineWizardStep|null} */ next() {}
                /** @returns {AlpineWizardStep|null} */ previous() {}
                /** @returns {AlpineWizardProgress} */ progress() {}
                /** @returns {boolean} */ isFirst() {}
                /** @returns {boolean} */ isNotFirst() {}
                /** @returns {boolean} */ isLast() {}
                /** @returns {boolean} */ isNotLast() {}
                /** @returns {boolean} */ isComplete() {}
                /** @returns {boolean} */ isNotComplete() {}
                /** @returns {boolean} */ isIncomplete() {}
                /** @returns {boolean} */ canGoForward() {}
                /** @returns {boolean} */ cannotGoForward() {}
                /** @returns {boolean} */ canGoBack() {}
                /** @returns {boolean} */ cannotGoBack() {}
                /** @returns {void} */ forward() {}
                /** @returns {void} */ back() {}
            }

            /** @type {AlpineWizardMagic} */
            let ${'$'}wizard;
            
        """.trimIndent()

        return MutablePair(context.left + wizardMagics, context.right)
    }

    override fun injectAutoCompleteSuggestions(suggestions: AutoCompleteSuggestions) {
        suggestions.descriptors.add(AttributeInfo("x-wizard:step"))
        suggestions.addModifiers("x-wizard:step", arrayOf("rules"))

        suggestions.descriptors.add(AttributeInfo("x-wizard:if"))
        suggestions.descriptors.add(AttributeInfo("x-wizard:title"))
    }

    override fun getDirectives(): List<String> = listOf(
        "x-wizard:step",
        "x-wizard:if",
        "x-wizard:title"
    )

    override fun getPrefixes(): List<String> = listOf(
        "x-wizard"
    )

    override fun performDetection(project: Project): Boolean {
        return hasAlpineWizardInScriptTags(project) || hasAlpineWizardCode(project)
    }

    private fun hasAlpineWizardInScriptTags(project: Project): Boolean {
        val htmlFiles = mutableListOf<VirtualFile>()

        // Get files by extension
        val extensions = listOf("html", "htm", "php", "twig")
        for (extension in extensions) {
            htmlFiles.addAll(
                FilenameIndex.getAllFilesByExt(
                    project,
                    extension,
                    GlobalSearchScope.projectScope(project)
                )
            )
        }

        return htmlFiles.any { virtualFile ->
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)

            when {
                psiFile is XmlFile -> {
                    // Handle HTML and XML files
                    val scriptTags = PsiTreeUtil.collectElementsOfType(psiFile, XmlTag::class.java)
                        .filter { it.name.equals("script", ignoreCase = true) }

                    scriptTags.any { scriptTag ->
                        val src = scriptTag.getAttributeValue("src")
                        if (src != null) {
                            isAlpineWizardScriptSrc(src)
                        } else {
                            hasAlpineWizardPatterns(scriptTag.value.text)
                        }
                    }
                }
                virtualFile.name.endsWith(".blade.php") -> {
                    // Handle Blade files by checking their raw content
                    try {
                        val content = String(virtualFile.contentsToByteArray())
                        hasAlpineWizardPatterns(content) || hasAlpineWizardScriptTagsInContent(content)
                    } catch (_: Exception) {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun isAlpineWizardScriptSrc(src: String): Boolean {
        return src.contains("alpine-wizard", ignoreCase = true)
    }

    private fun hasAlpineWizardScriptTagsInContent(content: String): Boolean {
        // Use regex to find script tags with alpine-wizard references in raw HTML content
        val scriptTagRegex = Regex("<script[^>]*src=['\"]([^'\"]*)['\"][^>]*>", RegexOption.IGNORE_CASE)

        return scriptTagRegex.findAll(content).any { match ->
            val src = match.groupValues[1]
            isAlpineWizardScriptSrc(src)
        }
    }

    private fun hasAlpineWizardCode(project: Project): Boolean {
        val jsExtensions = listOf("js", "ts", "mjs", "jsx", "tsx")
        val jsFiles = mutableListOf<VirtualFile>()

        for (extension in jsExtensions) {
            jsFiles.addAll(
                FilenameIndex.getAllFilesByExt(
                    project,
                    extension,
                    GlobalSearchScope.projectScope(project)
                )
            )
        }

        return jsFiles.any { virtualFile ->
            try {
                val content = String(virtualFile.contentsToByteArray())
                hasAlpineWizardPatterns(content)
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun hasAlpineWizardPatterns(content: String): Boolean {
        val alpineWizardPatterns = listOf(
            // Import statements
            "import.*alpine-wizard",
            "from.*alpine-wizard",
            "require.*alpine-wizard",

            // Alpine.js plugin registration
            "Alpine\\.plugin\\s*\\(.*wizard",
            "alpine\\.plugin\\s*\\(.*wizard",

            // Wizard-specific usage patterns
            "\\\$wizard\\s*\\.",
            "x-wizard:",
            "Alpine\\.wizard",
            "alpine\\.wizard"
        )

        return alpineWizardPatterns.any { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(content)
        }
    }
}