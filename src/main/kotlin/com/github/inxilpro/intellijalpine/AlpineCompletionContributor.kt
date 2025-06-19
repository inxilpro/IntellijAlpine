package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.XmlPatterns.xmlAttribute
import com.intellij.patterns.XmlPatterns.xmlAttributeValue
import com.intellij.psi.xml.XmlTokenType

class AlpineCompletionContributor : CompletionContributor() {
    init {
        // Attribute name completion
        extend(
            CompletionType.BASIC,
            psiElement(XmlTokenType.XML_NAME).withParent(xmlAttribute()),
            AlpineAttributeCompletionProvider()
        )
        
        // Attribute value completion for x-target
        extend(
            CompletionType.BASIC,
            psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
                .inside(xmlAttributeValue().withParent(xmlAttribute().withName("x-target"))),
            AlpineTargetAttributeValueCompletionProvider()
        )
    }
}
