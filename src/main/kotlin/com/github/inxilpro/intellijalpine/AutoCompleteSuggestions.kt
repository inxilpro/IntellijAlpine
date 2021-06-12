package com.github.inxilpro.intellijalpine

import com.intellij.psi.html.HtmlTag
import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor

class AutoCompleteSuggestions(val htmlTag: HtmlTag, val partialAttribute: String) {

    val descriptors: MutableList<AttributeInfo> = mutableListOf()

    private val tagName: String = htmlTag.name

    init {
        addDirectives()
        addPrefixes()
        addDerivedAttributes()
    }

    private fun addDirectives() {
        for (directive in AttributeUtil.directives) {
            if (tagName != "template" && (directive == "x-if" || directive == "x-for")) {
                continue
            }

            if (!directive.contains(partialAttribute)) {
                continue
            }

            descriptors.add(AttributeInfo(directive))
        }
    }

    private fun addPrefixes() {
        for (prefix in AttributeUtil.xmlPrefixes) {
            if (!prefix.contains(partialAttribute)) {
                continue
            }

            descriptors.add(AttributeInfo(prefix))
        }
    }

    private fun addDerivedAttributes() {
        if (!AttributeUtil.isEvent(partialAttribute) && !AttributeUtil.isBound(partialAttribute)) {
            return
        }

        for (descriptor in getDefaultHtmlAttributes(htmlTag)) {
            if (!partialMatches(descriptor.name)) {
                continue
            }

            if (descriptor.name.startsWith("on")) {
                addEvent(descriptor)
            } else {
                addBoundAttribute(descriptor)
            }
        }
    }

    private fun addEvent(descriptor: XmlAttributeDescriptor) {
        val event = descriptor.name.substring(2)
        for (prefix in AttributeUtil.eventPrefixes) {
            if (!prefix.contains(partialAttribute) && !partialAttribute.contains(prefix)) {
                continue
            }

            descriptors.add(AttributeInfo(prefix + event))

//            if (prefixMatchesAttributeSegment(prefix, '.')) {
//                for (modifier in AttributeUtil.eventModifiers) {
//                    println("$partialAttribute -> '$prefix$event.$modifier'")
//                    descriptors.add(AttributeInfo("${prefix}${event}.${modifier}"))
//                }
//            }
        }
    }

    private fun addBoundAttribute(descriptor: XmlAttributeDescriptor) {
        for (prefix in AttributeUtil.bindPrefixes) {
            if (!prefix.contains(partialAttribute) && !partialAttribute.contains(prefix)) {
                continue
            }

            descriptors.add(AttributeInfo(prefix + descriptor.name))
        }
    }

    private fun getDefaultHtmlAttributes(htmlTag: XmlTag): Array<out XmlAttributeDescriptor> {
        val tagDescriptor = htmlTag.descriptor as? HtmlElementDescriptorImpl
        val descriptor = tagDescriptor ?: HtmlNSDescriptorImpl.guessTagForCommonAttributes(htmlTag)

        return (descriptor as? HtmlElementDescriptorImpl)?.getDefaultAttributeDescriptors(htmlTag) ?: emptyArray()
    }

    private fun partialMatches(target: String): Boolean {
        val withoutModifier = partialAttribute.substringBefore(".")
        // val prefix = withoutModifier.substringBefore(":")
        val afterPrefix = withoutModifier.substringAfter(":")

        if (afterPrefix.isEmpty()) {
            return false
        }

        return target.contains(afterPrefix) || afterPrefix.contains(target)
    }

    private fun prefixMatchesAttributeSegment(attribute: String, segmentSeparator: Char): Boolean {
        if (!attribute.contains(segmentSeparator)) {
            return true
        }

        if (!partialAttribute.contains(segmentSeparator)) {
            return false
        }

        return attribute.startsWith(partialAttribute, true)
    }
}
