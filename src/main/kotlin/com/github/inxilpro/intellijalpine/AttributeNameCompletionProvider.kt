package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
// import com.intellij.codeInsight.lookup.LookupElementBuilder
// import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import com.intellij.xml.XmlAttributeDescriptor
// import com.intellij.xml.util.HtmlUtil
// import javax.swing.Icon

class AttributeNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        private fun getDefaultHtmlAttributes(context: XmlTag?): Array<out XmlAttributeDescriptor> =
            (context?.descriptor as? HtmlElementDescriptorImpl
                ?: HtmlNSDescriptorImpl.guessTagForCommonAttributes(context) as? HtmlElementDescriptorImpl)
                ?.getDefaultAttributeDescriptors(context) ?: emptyArray()
    }

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
        val attr = parameters.position.parent as? XmlAttribute ?: return
        val argumentsInsertHandler = InsertHandler<LookupElement> { insertionContext, _ ->
            insertionContext.setLaterRunnable {
                CodeCompletionHandlerBase(
                    CompletionType.BASIC).invokeCompletion(parameters.originalFile.project, parameters.editor)
            }
        }


        /*
        val attrInfo = VueAttributeNameParser.parse(StringUtil.trimEnd(attr.name, CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED), attr.parent)

        val providedAttributes = attr.parent?.attributes?.asSequence()
            ?.filter { it != attr }
            ?.flatMap { getAliases(it) }
            ?.toMutableSet() ?: mutableSetOf()
        val collector = CompletionCollector(parameters, resultSet, providedAttributes)
        addTagDescriptorCompletions(attr, attrInfo, collector, argumentsInsertHandler)
        if (attrInfo is VueDirectiveInfo) {
            val directive: VueDirective? = when (attrInfo.directiveKind) {
                ON, BIND, SLOT -> findAttributeDescriptor(ATTR_DIRECTIVE_PREFIX + fromAsset(attrInfo.name), attrInfo.name, attr.parent)
                else -> (attr.descriptor as? VueAttributeDescriptor)
            }?.getSources()?.getOrNull(0) as? VueDirective

            if (attrInfo.modifiers.isNotEmpty()) {
                addModifierCompletions(resultSet, directive, attrInfo)
            }
            else {
                when (attrInfo.directiveKind) {
                    ON -> {
                        addEventCompletions(attr, collector)
                        return
                    }
                    BIND -> {
                        addBindCompletions(attr, collector)
                        return
                    }
                    SLOT -> {
                        addSlotCompletions(attr, collector)
                        return
                    }
                    else -> {
                        val argument = directive?.argument
                        val argumentPrefix = getPatternCompletablePrefix(argument?.pattern)
                        if (attrInfo.arguments != null && argumentPrefix.isNotBlank()) {
                            val prefix = collector.prefix
                            val newResult = if (prefix == ATTR_DIRECTIVE_PREFIX + attrInfo.name + ATTR_ARGUMENT_PREFIX) resultSet.withPrefixMatcher("")
                            else resultSet
                            newResult.addElement(lookupElement(argumentPrefix, argument,
                                typeText = argument!!.pattern?.toString(), insertHandler = null))
                        }
                    }
                }
            }
        }

        for (kind in listOf(ON, BIND, SLOT)) {
            val attrName = ATTR_DIRECTIVE_PREFIX + kind.directiveName + ATTR_ARGUMENT_PREFIX
            collector.contributeAttribute(lookupElement(attrName, null, priority = LOW, insertHandler = argumentsInsertHandler))
        }
        */
    }
}