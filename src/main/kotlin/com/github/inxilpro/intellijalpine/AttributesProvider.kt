package com.github.inxilpro.intellijalpine

import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlAttributeDescriptorsProvider

class AttributesProvider : XmlAttributeDescriptorsProvider {
    override fun getAttributeDescriptors(xmlTag: XmlTag): Array<XmlAttributeDescriptor> {
        return emptyArray()
    }

    @Suppress("ReturnCount")
    override fun getAttributeDescriptor(name: String, xmlTag: XmlTag): XmlAttributeDescriptor? {
        val descriptor = xmlTag.descriptor as? HtmlElementDescriptorImpl ?: return null
        val info = AttributeInfo(name)

        if (info.isDirective() || info.isEvent() || info.isBound() || info.isTransition()) {
            return AlpineAttributeDescriptor(name, xmlTag)
        }

        /*
        if (info.isEvent()) {
            return descriptor.getAttributeDescriptor("on${info.name}", xmlTag)
        }

        if (info.isBound()) {
            return descriptor.getAttributeDescriptor(info.name, xmlTag)
        }

        if (info.isTransition()) {
            return descriptor.getAttributeDescriptor("class", xmlTag)
        }
        */

        return null
    }
}
