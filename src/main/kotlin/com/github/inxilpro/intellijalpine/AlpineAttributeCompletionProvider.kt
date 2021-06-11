package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.completion.HtmlCompletionContributor
import com.intellij.codeInsight.completion.XmlAttributeInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.xml.XmlAttribute
import com.intellij.util.ProcessingContext

class AlpineAttributeCompletionProvider(vararg items: String) :
    CompletionProvider<CompletionParameters?>() {

    @Suppress("ReturnCount")
    public override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        if (!HtmlCompletionContributor.hasHtmlAttributesCompletion(position)) {
            return
        }

        if (HTMLLanguage.INSTANCE !in position.containingFile.viewProvider.languages) {
            return
        }

        val attribute = position.parent as? XmlAttribute ?: return
        val xmlTag = attribute.parent as? HtmlTag ?: return

        // CompletionUtilCore.DUMMY_IDENTIFIER
        val attributeName = StringUtil.trimEnd(attribute.name, CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)

        for (prefix in AttributeUtil.xmlPrefixes) {
            if (prefix.startsWith(attributeName) && attributeName.length < prefix.length) {
                val info = AttributeInfo(prefix)
                val elementBuilder = LookupElementBuilder
                    .create(prefix)
                    .withCaseSensitivity(false)
                    .withIcon(Alpine.ICON)
                    .withTypeText(info.typeText)

                result.addElement(elementBuilder)
            }
        }

        for (info in AttributeUtil.getValidAttributesWithInfo(xmlTag)) {
            if (!prefixMatchesAttribute(attributeName, info.attribute)) {
                continue
            }
            var elementBuilder = LookupElementBuilder
                .create(info.attribute)
                .withCaseSensitivity(false)
                .withIcon(Alpine.ICON)
                .withTypeText(info.typeText)

            if (info.hasValue()) {
                elementBuilder = elementBuilder.withInsertHandler(XmlAttributeInsertHandler.INSTANCE)
            }

            result.addElement(elementBuilder)
        }
    }

    private fun prefixMatchesAttribute(prefix: String, attribute: String): Boolean {
        if (!attribute.contains(':')) {
            return true
        }

        if (!prefix.contains(':')) {
            return false
        }

        return attribute.startsWith(prefix, true)
    }
}
