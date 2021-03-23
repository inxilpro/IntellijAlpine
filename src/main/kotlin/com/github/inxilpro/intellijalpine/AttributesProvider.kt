package com.github.inxilpro.intellijalpine

import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlAttributeDescriptorsProvider

class AttributesProvider : XmlAttributeDescriptorsProvider {
    override fun getAttributeDescriptors(xmlTag: XmlTag): Array<XmlAttributeDescriptor> {
        return emptyArray()
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
}
