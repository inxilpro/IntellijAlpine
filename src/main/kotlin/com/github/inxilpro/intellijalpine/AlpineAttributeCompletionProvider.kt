package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.XmlAttributeInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.xml.XmlAttribute
import com.intellij.util.ProcessingContext

class AlpineAttributeCompletionProvider(vararg items: String) :
    CompletionProvider<CompletionParameters?>() {

    private val DIRECTIVE_TYPE_TEXTS = hashMapOf<String, String>(
        "x-data" to "New Alpine.js component scope",
        "x-init" to "Run on initialization",
        "x-show" to "Toggles 'display: none'",
        "x-model" to "Add two-way binding",
        "x-text" to "Bind to element's inner text",
        "x-html" to "Bind to element's inner HTML",
        "x-ref" to "Create a reference for later use",
        "x-if" to "Conditionally render template",
        "x-for" to "Map array to DOM nodes",
        "x-transition" to "Apply transition classes",
        "x-spread" to "Bind reusable directives",
        "x-cloak" to "Hide while Alpine is initializing",
    )

    public override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val attribute = parameters.position.parent as? XmlAttribute ?: return
        var tag = attribute.parent ?: return

        for (item in AttributeUtil.getValidAttributes(tag)) {
            var elementBuilder = LookupElementBuilder
                .create(item)
                .withCaseSensitivity(false)
                .withIcon(Alpine.ICON)

            if ("x-cloak" != item) {
                elementBuilder = elementBuilder.withInsertHandler(XmlAttributeInsertHandler.INSTANCE)
            }

            val typeText = getTypeText(item)
            if (typeText != null) {
                elementBuilder = elementBuilder.withTypeText(typeText)
            }

            result.addElement(elementBuilder)
        }
    }

    private fun getTypeText(attribute: String): String?
    {
        if (AttributeUtil.isEvent(attribute)) {
            val eventName = AttributeUtil.stripPrefix(attribute)
            return "'${eventName}' listener"
        }

        if (AttributeUtil.isBound(attribute)) {
            val sourceAttributeName = AttributeUtil.stripPrefix(attribute)
            return "Bind '${sourceAttributeName}' attribute"
        }

        return DIRECTIVE_TYPE_TEXTS.get(attribute)
    }
}