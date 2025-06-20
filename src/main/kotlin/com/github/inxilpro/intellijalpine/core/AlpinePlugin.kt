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

    fun getPackageDisplayName(): String

    fun getPackageNamesForDetection(): List<String>

    fun getTypeText(info: AttributeInfo): String?

    fun injectJsContext(context: MutablePair<String, String>): MutablePair<String, String>

    fun injectAutoCompleteSuggestions(suggestions: AutoCompleteSuggestions)

    fun getDirectives(): List<String>

    fun getPrefixes(): List<String>

    fun isEnabled(project: Project): Boolean

    fun enable(project: Project)

    fun disable(project: Project)

    fun performDetection(project: Project): Boolean
}