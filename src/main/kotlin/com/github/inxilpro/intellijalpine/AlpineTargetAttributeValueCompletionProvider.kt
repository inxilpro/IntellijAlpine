package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext

class AlpineTargetAttributeValueCompletionProvider : CompletionProvider<CompletionParameters>() {
    
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java) ?: return
        
        if (attribute.name != "x-target") {
            return
        }
        
        val xmlFile = element.containingFile as? XmlFile ?: return
        val currentValue = attribute.value ?: ""
        val allIds = collectElementIds(xmlFile)
        val usedIds = currentValue.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        val availableIds = allIds - usedIds
        
        // Get the prefix for filtering
        val caretOffset = parameters.offset
        val valueStart = attribute.valueElement?.textRange?.startOffset ?: return
        val prefix = currentValue.substring(0, (caretOffset - valueStart - 1).coerceAtLeast(0))
            .split("\\s+".toRegex()).lastOrNull() ?: ""
        
        val filteredResult = result.withPrefixMatcher(prefix)
        
        for (id in availableIds) {
            if (id.startsWith(prefix)) {
                val lookupElement = LookupElementBuilder
                    .create(id)
                    .withTypeText("Element ID")
                    .withIcon(Alpine.ICON)
                    .withInsertHandler(XTargetInsertHandler())
                
                filteredResult.addElement(lookupElement)
            }
        }
    }
    
    private fun collectElementIds(xmlFile: XmlFile): Set<String> {
        val ids = mutableSetOf<String>()
        val rootTag = xmlFile.rootTag ?: return ids
        
        fun collectIds(tag: XmlTag) {
            tag.getAttribute("id")?.value?.let { id ->
                if (id.isNotBlank()) {
                    ids.add(id)
                }
            }
            
            for (subTag in tag.subTags) {
                collectIds(subTag)
            }
        }
        
        collectIds(rootTag)
        return ids
    }
}

class XTargetInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val element = context.file.findElementAt(context.startOffset) ?: return
        val attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java) ?: return
        val currentValue = attribute.value ?: ""
        
        // If there are already IDs and we're not at the end, add a space
        if (currentValue.isNotBlank() && !currentValue.endsWith(" ")) {
            context.document.insertString(context.tailOffset, " ")
            context.editor.caretModel.moveToOffset(context.tailOffset + 1)
        }
    }
}