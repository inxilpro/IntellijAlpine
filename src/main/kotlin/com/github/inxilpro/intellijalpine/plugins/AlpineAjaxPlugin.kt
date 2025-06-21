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
                    .withParent(
                        XmlPatterns.xmlAttributeValue().withParent(XmlPatterns.xmlAttribute().withName("x-merge"))
                    ),
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
}