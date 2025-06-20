package com.github.inxilpro.intellijalpine

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.apache.commons.lang3.tuple.MutablePair

interface AlpinePlugin {
    companion object {
        val EP_NAME = ExtensionPointName.create<AlpinePlugin>("com.github.inxilpro.intellijalpine.alpinePlugin")
    }

    fun getPackageDisplayName(): String

    fun getPackageNamesForDetection(): List<String>

    fun getTypeText(info: AttributeInfo): String?

    fun injectJsContext(context: MutablePair<String, String>): MutablePair<String, String>

    fun getDirectives(): List<String>

    fun isEnabled(project: Project): Boolean

    fun enable(project: Project)

    fun disable(project: Project)

    fun performDetection(project: Project): Boolean
}