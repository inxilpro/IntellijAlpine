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
        val info = AttributeInfo(name)

        if (info.isDirective()) {
            return AlpineAttributeDescriptor(name, xmlTag)
        }

        if (info.isEvent()) {
            return descriptor.getAttributeDescriptor("on${info.name}", xmlTag)
        }

        if (info.isBound()) {
            return descriptor.getAttributeDescriptor(info.name, xmlTag)
        }

        return null
    }
}
