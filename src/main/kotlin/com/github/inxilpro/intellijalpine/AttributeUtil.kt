package com.github.inxilpro.intellijalpine

import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor

object AttributeUtil {
    val directives = arrayOf(
        "x-data",
        "x-init",
        "x-show",
        "x-model",
        "x-text",
        "x-html",
        "x-ref",
        "x-if",
        "x-for",
        "x-transition",
        "x-spread",
        "x-cloak",
    )

    val eventPrefixes = arrayOf(
        "@",
        "x-on:"
    )

    val bindPrefixes = arrayOf(
        ":",
        "x-bind:"
    )

    fun getValidAttributes(xmlTag: XmlTag): Array<String> {
        val descriptors = mutableListOf<String>()

        for (directive in directives) {
            if (xmlTag.name != "template" && (directive == "x-if" || directive == "x-for")) {
                continue
            }

            descriptors.add(directive)
        }

        for (descriptor in getDefaultHtmlAttributes(xmlTag)) {
            if (descriptor.name.startsWith("on")) {
                val event = descriptor.name.substring(2)
                for (prefix in eventPrefixes) {
                    descriptors.add(prefix + event)
                }
            } else {
                for (prefix in bindPrefixes) {
                    descriptors.add(prefix + descriptor.name)
                }
            }
        }

        return descriptors.toTypedArray()
    }

    fun getValidAttributesWithInfo(xmlTag: XmlTag): Array<AttributeInfo> {
        return getValidAttributes(xmlTag)
            .map { AttributeInfo(it) }
            .toTypedArray()
    }

    fun isEvent(attribute: String): Boolean {
        for (prefix in eventPrefixes) {
            if (attribute.startsWith(prefix)) {
                return true
            }
        }

        return false
    }

    private fun getDefaultHtmlAttributes(xmlTag: XmlTag): Array<out XmlAttributeDescriptor> {
        val tagDescriptor = xmlTag.descriptor as? HtmlElementDescriptorImpl
        val descriptor = tagDescriptor ?: HtmlNSDescriptorImpl.guessTagForCommonAttributes(xmlTag)

        return (descriptor as? HtmlElementDescriptorImpl)?.getDefaultAttributeDescriptors(xmlTag) ?: emptyArray()
    }
}
