package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.util.ProcessingContext

class AlpineMergeValueCompletionProvider : CompletionProvider<CompletionParameters>() {
    
    private val mergeStrategies = arrayOf(
        "before" to "Insert content before target",
        "replace" to "Replace target element (default)",
        "update" to "Update target's innerHTML",
        "prepend" to "Prepend content to target",
        "append" to "Append content to target", 
        "after" to "Insert content after target",
        "morph" to "Morph content preserving state"
    )
    
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java) ?: return
        
        if (attribute.name != "x-merge") {
            return
        }
        
        for ((strategy, description) in mergeStrategies) {
            val lookupElement = LookupElementBuilder
                .create(strategy)
                .withTypeText(description)
                .withIcon(Alpine.ICON)
                
            result.addElement(lookupElement)
        }
    }
}