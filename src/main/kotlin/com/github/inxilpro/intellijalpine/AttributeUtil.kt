package com.github.inxilpro.intellijalpine

import com.intellij.psi.html.HtmlTag
import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor

object AttributeUtil {
    private val validAttributes = mutableMapOf<String, Array<AttributeInfo>>()

    val xmlPrefixes = arrayOf(
        "x-on",
        "x-bind",
        "x-transition",
        "x-wizard", // glhd/alpine-wizard pacakge
    )

    val directives = arrayOf(
        "x-data",
        "x-init",
        "x-show",
        "x-bind",
        "x-text",
        "x-html",
        "x-model",
        "x-modelable",
        "x-for",
        "x-transition",
        "x-effect",
        "x-ignore",
        "x-ref",
        "x-cloak",
        "x-teleport",
        "x-if",
        "x-id",
        "x-mask",
        "x-intersect",
        "x-trap",
        "x-collapse",
        "x-spread", // deprecated
    )

    val templateDirectives = arrayOf(
        "x-if",
        "x-for",
        "x-teleport",
    )

    val eventPrefixes = arrayOf(
        "@",
        "x-on:"
    )

    val eventModifiers = arrayOf(
        "prevent",
        "stop",
        "outside",
        "window",
        "document",
        "once",
        "debounce",
        "throttle",
        "self",
        "camel",
        "passive"
    )

    val bindPrefixes = arrayOf(
        ":",
        "x-bind:"
    )

    val modelModifiers = arrayOf(
        "lazy",
        "number",
        "debounce",
        "throttle"
    )

    val timeUnitModifiers = arrayOf(
        "debounce",
        "throttle",
        "duration",
        "delay",
    )

    val numericModifiers = arrayOf(
        "scale",
    )

    val transitionModifiers = arrayOf(
        "duration",
        "delay",
        "opacity",
        "scale",
        "origin",
    )

    val keypressModifiers = arrayOf(
        "shift",
        "enter",
        "space",
        "ctrl",
        "cmd",
        "meta",
        "alt",
        "up",
        "down",
        "left",
        "right",
        "esc",
        "tab",
        "caps-lock",
    )

    val intersectModifiers = arrayOf(
        "once"
    )

    fun isXmlPrefix(prefix: String): Boolean {
        return xmlPrefixes.contains(prefix)
    }

    fun isTemplateDirective(directive: String): Boolean {
        return templateDirectives.contains(directive)
    }

    fun getValidAttributesWithInfo(xmlTag: HtmlTag): Array<AttributeInfo> {
        return validAttributes.getOrPut(xmlTag.name, { buildValidAttributes(xmlTag) })
    }

    fun isEvent(attribute: String): Boolean {
        for (prefix in eventPrefixes) {
            if (attribute.startsWith(prefix)) {
                return true
            }
        }

        return false
    }

    fun isBound(attribute: String): Boolean {
        for (prefix in bindPrefixes) {
            if (attribute.startsWith(prefix)) {
                return true
            }
        }

        return false
    }

    private fun buildValidAttributes(htmlTag: HtmlTag): Array<AttributeInfo> {
        val descriptors = mutableListOf<AttributeInfo>()

        for (directive in directives) {
            if (htmlTag.name != "template" && isTemplateDirective(directive)) {
                continue
            }

            descriptors.add(AttributeInfo(directive))
        }

        for (descriptor in getDefaultHtmlAttributes(htmlTag)) {
            if (descriptor.name.startsWith("on")) {
                val event = descriptor.name.substring(2)
                for (prefix in eventPrefixes) {
                    descriptors.add(AttributeInfo(prefix + event))
                }
            } else {
                for (prefix in bindPrefixes) {
                    descriptors.add(AttributeInfo(prefix + descriptor.name))
                }
            }
        }

        return descriptors.toTypedArray()
    }

    private fun getDefaultHtmlAttributes(xmlTag: XmlTag): Array<out XmlAttributeDescriptor> {
        val tagDescriptor = xmlTag.descriptor as? HtmlElementDescriptorImpl
        val descriptor = tagDescriptor ?: HtmlNSDescriptorImpl.guessTagForCommonAttributes(xmlTag)

        return (descriptor as? HtmlElementDescriptorImpl)?.getDefaultAttributeDescriptors(xmlTag) ?: emptyArray()
    }
}
