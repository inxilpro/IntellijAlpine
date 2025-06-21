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

class TooltipPlugin : AlpinePlugin {

    override fun getPluginName(): String = "alpine-tooltip"

    override fun getPackageDisplayName(): String = "alpine-tooltip"

    override fun getPackageNamesForDetection(): List<String> = listOf(
        "alpine-tooltip",
        "@ryangjchandler/alpine-tooltip"
    )

    override fun injectAutoCompleteSuggestions(suggestions: AutoCompleteSuggestions) {
        val modifiers = arrayOf(
            "duration",
            "delay",
            "cursor",
            "on",
            "arrowless",
            "html",
            "interactive",
            "border",
            "debounce",
            "max-width",
            "theme",
            "placement",
            "animation",
            "no-flip",
        )

        suggestions.addModifiers("x-tooltip", modifiers)
    }

    override fun getTypeText(info: AttributeInfo): String? {
        return when (info.attribute) {
            "x-tooltip" -> "Tippy.js tooltip"
            else -> null
        }
    }

    override fun injectJsContext(context: MutablePair<String, String>): MutablePair<String, String> {
        val magics = """
            /**
             * @param {string} value
             * @param {Object} options
             * @return {Promise<Response>}
             */
            function ${'$'}tooltip(value, options = {}) {}
            
        """.trimIndent()

        return MutablePair(context.left + magics, context.right)
    }

    override fun getDirectives(): List<String> = listOf(
        "x-tooltip",
    )
}