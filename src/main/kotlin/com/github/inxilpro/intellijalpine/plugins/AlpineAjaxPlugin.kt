package com.github.inxilpro.intellijalpine.plugins

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.github.inxilpro.intellijalpine.core.CompletionProviderRegistration
import com.github.inxilpro.intellijalpine.attributes.AttributeInfo
import com.github.inxilpro.intellijalpine.completion.AutoCompleteSuggestions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import org.apache.commons.lang3.tuple.MutablePair

class AlpineAjaxPlugin : AlpinePlugin {

    val targetModifiers = arrayOf(
        "200",
        "301",
        "302",
        "303",
        "400",
        "401",
        "403",
        "404",
        "422",
        "500",
        "502",
        "503",
        "2xx",
        "3xx",
        "4xx",
        "5xx",
        "back",
        "away",
        "replace",
        "push",
        "error",
        "nofocus",
    )

    override fun getPluginName(): String = "alpine-ajax"

    override fun getPackageDisplayName(): String = "alpine-ajax"

    override fun getPackageNamesForDetection(): List<String> = listOf(
        "alpine-ajax",
        "@imacrayon/alpine-ajax"
    )

    override fun getTypeText(info: AttributeInfo): String? {
        if ("x-target:" == info.prefix) {
            return "DOM node to inject response into"
        }

        return when (info.attribute) {
            "x-target" -> "DOM node to inject response into"
            "x-headers" -> "Set AJAX request headers"
            "x-merge" -> "Merge response data with existing data"
            "x-autofocus" -> "Auto-focus on AJAX response"
            "x-sync" -> "Always sync on AJAX response"
            else -> null
        }
    }

    override fun injectJsContext(context: MutablePair<String, String>): MutablePair<String, String> {
        val magics = """
            /**
             * @param {string} action
             * @param {Object} options
             * @return {Promise<Response>}
             */
            function ${'$'}ajax(action, options = {}) {}
            
        """.trimIndent()

        return MutablePair(context.left + magics, context.right)
    }

    override fun directiveSupportJavaScript(directive: String): Boolean {
        return when (directive) {
            "x-target", "x-autofocus", "x-sync", "x-merge" -> false
            else -> true
        }
    }

    override fun injectAutoCompleteSuggestions(suggestions: AutoCompleteSuggestions) {
        suggestions.descriptors.add(AttributeInfo("x-target:dynamic"))
        suggestions.addModifiers("x-target", targetModifiers)
        suggestions.addModifiers("x-target:dynamic", targetModifiers)
    }

    override fun getCompletionProviders(): List<CompletionProviderRegistration> {
        return listOf(
            CompletionProviderRegistration(
                XmlPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
                    .withParent(XmlPatterns.xmlAttributeValue().withParent(XmlPatterns.xmlAttribute().withName("x-merge"))),
                AlpineMergeValueCompletionProvider(this)
            )
        )
    }

    override fun getDirectives(): List<String> = listOf(
        "x-target",
        "x-headers",
        "x-merge",
        "x-autofocus",
        "x-sync"
    )

    override fun getPrefixes(): List<String> = listOf(
        "x-target"
    )

    override fun performDetection(project: Project): Boolean {
        return hasAlpineAjaxInScriptTags(project) || hasAlpineAjaxCode(project)
    }

    private fun hasAlpineAjaxInScriptTags(project: Project): Boolean {
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
                            isAlpineAjaxScriptSrc(src)
                        } else {
                            hasAlpineAjaxPatterns(scriptTag.value.text)
                        }
                    }
                }
                virtualFile.name.endsWith(".blade.php") -> {
                    // Handle Blade files by checking their raw content
                    try {
                        val content = String(virtualFile.contentsToByteArray())
                        hasAlpineAjaxPatterns(content) || hasAlpineAjaxScriptTagsInContent(content)
                    } catch (_: Exception) {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun isAlpineAjaxScriptSrc(src: String): Boolean {
        return src.contains("alpine-ajax", ignoreCase = true)
    }

    private fun hasAlpineAjaxScriptTagsInContent(content: String): Boolean {
        // Use regex to find script tags with alpine-ajax references in raw HTML content
        val scriptTagRegex = Regex("<script[^>]*src=['\"]([^'\"]*)['\"][^>]*>", RegexOption.IGNORE_CASE)

        return scriptTagRegex.findAll(content).any { match ->
            val src = match.groupValues[1]
            isAlpineAjaxScriptSrc(src)
        }
    }

    private fun hasAlpineAjaxCode(project: Project): Boolean {
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
                hasAlpineAjaxPatterns(content)
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun hasAlpineAjaxPatterns(content: String): Boolean {
        val alpineAjaxPatterns = listOf(
            // Import statements
            "import.*alpine-ajax",
            "from.*alpine-ajax",
            "require.*alpine-ajax",

            // Alpine.js plugin registration
            "Alpine\\.plugin\\s*\\(.*ajax",
            "alpine\\.plugin\\s*\\(.*ajax",

            // Ajax-specific functions from the alpine-ajax source
            "AjaxInterceptor",
            "AjaxCommand",
            "ajaxCommand",
            "processAjaxResponse",
            "handleAjaxRequest",

            // Magic helper usage patterns
            "\\\$ajax\\s*\\(",
            "x-target",
            "x-swap",
            "x-headers",
            "x-replace"
        )

        return alpineAjaxPatterns.any { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(content)
        }
    }
}