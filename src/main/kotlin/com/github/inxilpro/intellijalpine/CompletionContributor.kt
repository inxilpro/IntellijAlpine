package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.XmlPatterns.xmlAttribute
import com.intellij.psi.xml.XmlTokenType

class CompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, psiElement(XmlTokenType.XML_NAME).withParent(xmlAttribute()),
            AttributeNameCompletionProvider())
//        extend(CompletionType.BASIC, psiElement(XmlTokenType.XML_DATA_CHARACTERS),
//            VueTagContentCompletionProvider())
//        extend(CompletionType.BASIC, psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN),
//            VueAttributeValueCompletionProvider())
    }
}
