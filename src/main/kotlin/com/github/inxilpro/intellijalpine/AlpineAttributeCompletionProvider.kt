package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.HtmlCompletionContributor
import com.intellij.codeInsight.completion.XmlAttributeInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.html.HTMLLanguage
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

        for (info in AttributeUtil.getValidAttributesWithInfo(xmlTag)) {
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
}
