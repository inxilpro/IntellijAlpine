package com.github.inxilpro.intellijalpine.plugins

import com.github.inxilpro.intellijalpine.Alpine
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
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
        val idMap = getIdMap(xmlFile)
        return idMap[idValue]
    }

    override fun getVariants(): Array<Any> {
        val xmlFile = element.containingFile as? XmlFile ?: return emptyArray()
        val idMap = getIdMap(xmlFile)
        val currentValue = (element as XmlAttributeValue).value
        val usedIds = currentValue.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        val availableIds = idMap.keys - usedIds

        return availableIds.map { id ->
            LookupElementBuilder.create(id)
                .withTypeText("Element ID")
                .withIcon(Alpine.ICON)
        }.toTypedArray()
    }

    override fun isSoft(): Boolean = false

    private fun getIdMap(xmlFile: XmlFile): Map<String, PsiElement> {
        return CachedValuesManager.getCachedValue(xmlFile) {
            val allTags = PsiTreeUtil.findChildrenOfType(xmlFile, XmlTag::class.java)
            val map = mutableMapOf<String, PsiElement>()

            for (tag in allTags) {
                val idAttr = tag.getAttribute("id")
                val idVal = idAttr?.value
                if (!idVal.isNullOrBlank() && idAttr.valueElement != null) {
                    map[idVal] = idAttr.valueElement!!
                }
            }

            CachedValueProvider.Result.create(map as Map<String, PsiElement>, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }
}
