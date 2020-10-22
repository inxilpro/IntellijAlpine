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

        for (directive in Alpine.allDirectives()) {
            descriptors.add(AttributeDescriptor(directive))
        }

//        for (prefix in Alpine.EVENT_PREFIXES) {
//            for (event in Alpine.COMMON_EVENTS) {
//                descriptors.add(AttributeDescriptor(prefix + event))
//            }
//        }

        return descriptors.toTypedArray()
    }

    override fun getAttributeDescriptor(name: String, xmlTag: XmlTag): XmlAttributeDescriptor? {
        for (prefix in Alpine.EVENT_PREFIXES) {
            if (name.startsWith(prefix)) {
                val descriptor = xmlTag.descriptor
                if (descriptor != null) {
                    val attrName = name.substring(prefix.length)
                    val attributeDescriptor = descriptor.getAttributeDescriptor(attrName, xmlTag)
                    return attributeDescriptor ?: descriptor.getAttributeDescriptor("on$attrName", xmlTag)
                }
            }
        }

        return null
    }

    private class AttributeDescriptor(private val name: String) : BasicXmlAttributeDescriptor(),
        PsiPresentableMetaData {
        override fun getIcon() = Alpine.ICON

        override fun getTypeName(): String? = "Alpine.js"

        override fun init(psiElement: PsiElement) {
        }

        override fun isRequired(): Boolean = false
        override fun hasIdType(): Boolean = false
        override fun hasIdRefType(): Boolean = false
        override fun isEnumerated(): Boolean = false
        override fun getDeclaration(): PsiElement? = null
        override fun getName(): String = name
        override fun getDependences(): Array<Any> = ArrayUtil.EMPTY_OBJECT_ARRAY
        override fun isFixed(): Boolean = false
        override fun getDefaultValue(): String? = null
        override fun getEnumeratedValues(): Array<String>? = ArrayUtil.EMPTY_STRING_ARRAY
    }
}
