package com.github.inxilpro.intellijalpine

import com.intellij.psi.PsiElement
import com.intellij.psi.meta.PsiPresentableMetaData
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ArrayUtil
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlAttributeDescriptorsProvider
import com.intellij.xml.impl.BasicXmlAttributeDescriptor

class AttributesProvider : XmlAttributeDescriptorsProvider {
    override fun getAttributeDescriptors(xmlTag: XmlTag): Array<XmlAttributeDescriptor> {
        val descriptors = mutableListOf<AttributeDescriptor>()

        for (directive in AttributeUtil.getValidAttributes(xmlTag)) {
            descriptors.add(AttributeDescriptor(directive))
        }

        return descriptors.toTypedArray()
    }

    @Suppress("ReturnCount")
    override fun getAttributeDescriptor(name: String, xmlTag: XmlTag): XmlAttributeDescriptor? {
        val descriptor = xmlTag.descriptor ?: return null

        if (AttributeUtil.isDirective(name)) {
            val attrName = "data-$name"
            val attributeDescriptor = descriptor.getAttributeDescriptor(attrName, xmlTag)
            return attributeDescriptor ?: descriptor.getAttributeDescriptor(attrName, xmlTag)
        }

        if (AttributeUtil.isEvent(name)) {
            val attrName = AttributeUtil.stripPrefix(name)
            val attributeDescriptor = descriptor.getAttributeDescriptor(attrName, xmlTag)
            return attributeDescriptor ?: descriptor.getAttributeDescriptor("on$attrName", xmlTag)
        }

        if (AttributeUtil.isBound(name)) {
            val attrName = AttributeUtil.stripPrefix(name)
            val attributeDescriptor = descriptor.getAttributeDescriptor(attrName, xmlTag)
            return attributeDescriptor ?: descriptor.getAttributeDescriptor(attrName, xmlTag)
        }

        return null
    }

    private class AttributeDescriptor(private val name: String) : BasicXmlAttributeDescriptor(),
        PsiPresentableMetaData {
        override fun getIcon() = Alpine.ICON
        override fun getTypeName() = "Alpine.js"
        override fun init(psiElement: PsiElement) {}
        override fun isRequired(): Boolean = false
        override fun hasIdType(): Boolean {
            return name == "id"
        }

        override fun hasIdRefType(): Boolean = false
        override fun isEnumerated(): Boolean = false
        override fun getDeclaration(): PsiElement? = null
        override fun getName(): String = name
        override fun getDependencies(): Array<Any> = ArrayUtil.EMPTY_OBJECT_ARRAY
        override fun isFixed(): Boolean = false
        override fun getDefaultValue(): String? = null
        override fun getEnumeratedValues(): Array<String>? = ArrayUtil.EMPTY_STRING_ARRAY
    }
}
