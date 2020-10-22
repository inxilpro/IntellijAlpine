package com.github.inxilpro.intellijalpine

import com.intellij.psi.PsiElement
// import com.intellij.psi.meta.PsiPresentableMetaData
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ArrayUtil
import com.intellij.xml.XmlAttributeDescriptor
import com.intellij.xml.XmlAttributeDescriptorsProvider
import com.intellij.xml.impl.BasicXmlAttributeDescriptor

class AttributesProvider : XmlAttributeDescriptorsProvider {
    override fun getAttributeDescriptors(xmlTag: XmlTag): Array<XmlAttributeDescriptor> {
        return arrayOf(
                AttributeDescriptor("x-data"),
                AttributeDescriptor("x-init"),
                AttributeDescriptor("x-show"),
                AttributeDescriptor("x-model"),
                AttributeDescriptor("x-text"),
                AttributeDescriptor("x-html"),
                AttributeDescriptor("x-ref"),
                AttributeDescriptor("x-if"),
                AttributeDescriptor("x-for"),
                AttributeDescriptor("x-transition"),
                AttributeDescriptor("x-spread"),
                AttributeDescriptor("x-cloak")
        )
    }

    override fun getAttributeDescriptor(name: String, xmlTag: XmlTag): XmlAttributeDescriptor? {
        val prefixes = arrayOf("x-on:", "@")
        for (prefix in prefixes) {
            if (name.startsWith(prefix)) {
                val descriptor = xmlTag.descriptor
                if (descriptor != null) {
                    val attrName = name.substring(prefix.length)
                    val attributeDescriptor = descriptor.getAttributeDescriptor(attrName, xmlTag)
                    return attributeDescriptor ?: descriptor.getAttributeDescriptor("on$attrName", xmlTag)
                }
            }
        }
        // return if (Aurelia.REPEAT_FOR == name || Aurelia.VIRTUAL_REPEAT_FOR == name || Aurelia.AURELIA_APP == name) AttributeDescriptor(name) else null
        return null
    }

    private class AttributeDescriptor(private val name: String) : BasicXmlAttributeDescriptor() {
//        override fun getIcon() = Aurelia.ICON
//
//        override fun getTypeName(): String? = null

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
