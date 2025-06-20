package com.github.inxilpro.intellijalpine.core

import com.github.inxilpro.intellijalpine.attributes.AttributeInfo
import com.github.inxilpro.intellijalpine.completion.AutoCompleteSuggestions
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.apache.commons.lang3.tuple.MutablePair

interface AlpinePlugin {
    companion object {
        val EP_NAME = ExtensionPointName.Companion.create<AlpinePlugin>("com.github.inxilpro.intellijalpine.alpinePlugin")
    }

    fun getPluginName(): String

    fun getPackageDisplayName(): String

    fun getPackageNamesForDetection(): List<String>

    fun getTypeText(info: AttributeInfo): String? = null

    fun injectJsContext(context: MutablePair<String, String>): MutablePair<String, String> = context

    fun directiveSupportJavaScript(directive: String): Boolean = true

    fun injectAutoCompleteSuggestions(suggestions: AutoCompleteSuggestions) {}

    fun getDirectives(): List<String> = emptyList()

    fun getPrefixes(): List<String> = emptyList()

    fun performDetection(project: Project): Boolean = false
}