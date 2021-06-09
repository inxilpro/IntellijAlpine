package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
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
