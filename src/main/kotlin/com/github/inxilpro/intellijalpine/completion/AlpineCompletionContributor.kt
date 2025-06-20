package com.github.inxilpro.intellijalpine.completion

import com.github.inxilpro.intellijalpine.plugins.AlpineMergeValueCompletionProvider
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

        // Attribute value completion for x-merge
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
                .inside(XmlPatterns.xmlAttributeValue().withParent(XmlPatterns.xmlAttribute().withName("x-merge"))),
            AlpineMergeValueCompletionProvider()
        )
    }
}