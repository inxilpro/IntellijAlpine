package com.github.inxilpro.intellijalpine

import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor

object AttributeUtil {
    private val DIRECTIVES = arrayOf(
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

    private val EVENT_PREFIXES = arrayOf(
        "@",
        "x-on:"
    )

    private val BIND_PREFIXES = arrayOf(
        ":",
        "x-bind:"
    )

    fun getValidAttributes(xmlTag: XmlTag): Array<String>
    {
        val descriptors = mutableListOf<String>()

        for (directive in DIRECTIVES) {
            descriptors.add(directive)
        }

        for (descriptor in getDefaultHtmlAttributes(xmlTag)) {
            if (descriptor.name.startsWith("on")) {
                val event = descriptor.name.substring(2)
                for (prefix in EVENT_PREFIXES) {
                    descriptors.add(prefix + event)
                }
            } else {
                for (prefix in BIND_PREFIXES) {
                    descriptors.add(prefix + descriptor.name)
                }
            }
        }

        return descriptors.toTypedArray()
    }

    fun isDirective(attribute: String): Boolean
    {
        return DIRECTIVES.contains(attribute)
    }

    fun isEvent(attribute: String): Boolean
    {
        for (prefix in EVENT_PREFIXES) {
            if (attribute.startsWith(prefix)) {
                return true
            }
        }

        return false
    }

    fun isBound(attribute: String): Boolean
    {
        for (prefix in BIND_PREFIXES) {
            if (attribute.startsWith(prefix)) {
                return true
            }
        }

        return false
    }

    @Suppress("ReturnCount")
    fun stripPrefix(attribute: String): String
    {
        for (prefix in EVENT_PREFIXES) {
            if (attribute.startsWith(prefix)) {
                return attribute.substring(prefix.length)
            }
        }

        for (prefix in BIND_PREFIXES) {
            if (attribute.startsWith(prefix)) {
                return attribute.substring(prefix.length)
            }
        }

        return attribute
    }

    private fun getDefaultHtmlAttributes(xmlTag: XmlTag): Array<out XmlAttributeDescriptor> {
        return (xmlTag.descriptor as? HtmlElementDescriptorImpl
            ?: HtmlNSDescriptorImpl.guessTagForCommonAttributes(xmlTag) as? HtmlElementDescriptorImpl)
            ?.getDefaultAttributeDescriptors(xmlTag) ?: emptyArray()
    }
}