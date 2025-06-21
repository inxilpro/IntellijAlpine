package com.github.inxilpro.intellijalpine.completion

import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.xml.XmlTokenType

class AlpineCompletionContributor : CompletionContributor() {
    init {
        // Attribute name completion
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(XmlTokenType.XML_NAME).withParent(XmlPatterns.xmlAttribute()),
            AlpineAttributeCompletionProvider()
        )

        // Plugin completions
        AlpinePluginRegistry.instance.getRegisteredPlugins().forEach { plugin ->
            plugin.getCompletionProviders().forEach { registration ->
                extend(
                    registration.type,
                    registration.pattern,
                    registration.provider
                )
            }
        }
    }
}