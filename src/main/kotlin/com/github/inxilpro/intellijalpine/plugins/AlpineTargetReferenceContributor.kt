package com.github.inxilpro.intellijalpine.plugins

import com.github.inxilpro.intellijalpine.Alpine
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext

class AlpineTargetReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            XmlPatterns.xmlAttributeValue().withParent(
                XmlPatterns.xmlAttribute().withName("x-target")
            ),
            AlpineTargetReferenceProvider()
        )
    }
}

class AlpineTargetReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val attributeValue = element as? XmlAttributeValue ?: return PsiReference.EMPTY_ARRAY
        val value = attributeValue.value

        if (value.isBlank()) return PsiReference.EMPTY_ARRAY

        val references = mutableListOf<PsiReference>()
        val ids = value.split("\\s+".toRegex()).filter { it.isNotBlank() }
        var searchStart = 0

        for (id in ids) {
            val startIndex = value.indexOf(id, searchStart)
            if (startIndex >= 0) {
                // Range relative to the attribute value element (includes quotes)
                val range = TextRange(startIndex + 1, startIndex + id.length + 1)
                references.add(AlpineIdReference(attributeValue, range, id))
                searchStart = startIndex + id.length
            }
        }

        return references.toTypedArray()
    }
}

class AlpineIdReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val idValue: String
) : PsiReferenceBase<PsiElement>(element, rangeInElement) {

    override fun resolve(): PsiElement? {
        val xmlFile = element.containingFile as? XmlFile ?: return null
        return findElementWithId(xmlFile, idValue)
    }

    override fun getVariants(): Array<Any> {
        val xmlFile = element.containingFile as? XmlFile ?: return emptyArray()
        val allIds = collectElementIds(xmlFile)
        val currentValue = (element as XmlAttributeValue).value
        val usedIds = currentValue.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        val availableIds = allIds - usedIds

        return availableIds.map { id ->
            LookupElementBuilder.create(id)
                .withTypeText("Element ID")
                .withIcon(Alpine.ICON)
        }.toTypedArray()
    }

    override fun isSoft(): Boolean = false // Hard reference - should show error if unresolved

    private fun findElementWithId(xmlFile: XmlFile, id: String): PsiElement? {
        val allTags = PsiTreeUtil.findChildrenOfType(xmlFile, XmlTag::class.java)

        return allTags.firstOrNull { tag ->
            tag.getAttribute("id")?.value == id
        }?.getAttribute("id")?.valueElement
    }

    private fun collectElementIds(xmlFile: XmlFile): Set<String> {
        val allTags = PsiTreeUtil.findChildrenOfType(xmlFile, XmlTag::class.java)
        
        return allTags.mapNotNull { tag ->
            tag.getAttribute("id")?.value?.takeIf { it.isNotBlank() }
        }.toSet()
    }
}