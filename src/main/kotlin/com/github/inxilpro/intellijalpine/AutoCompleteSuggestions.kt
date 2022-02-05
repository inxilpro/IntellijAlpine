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
        addTransitions()
    }

    private fun addDirectives() {
        for (directive in AttributeUtil.directives) {
            if (tagName != "template" && (directive == "x-if" || directive == "x-for")) {
                continue
            }

            descriptors.add(AttributeInfo(directive))

            if ("x-model" == directive) {
                addModifiers(directive, AttributeUtil.modelModifiers)
            }

            if ("x-intersect" == directive) {
                addModifiers(directive, AttributeUtil.intersectModifiers)
            }
        }
    }

    private fun addPrefixes() {
        for (prefix in AttributeUtil.xmlPrefixes) {
            descriptors.add(AttributeInfo(prefix))
        }
    }

    private fun addDerivedAttributes() {
        if (!AttributeUtil.isEvent(partialAttribute) && !AttributeUtil.isBound(partialAttribute, htmlTag)) {
            return
        }

        for (descriptor in getDefaultHtmlAttributes(htmlTag)) {
            if (descriptor.name.startsWith("on")) {
                addEvent(descriptor)
            } else {
                addBoundAttribute(descriptor)
            }
        }
    }

    private fun addTransitions() {
        val stages: Array<String> = arrayOf(
            "enter",
            "enter-start",
            "enter-end",
            "leave",
            "leave-start",
            "leave-end",
        )

        addModifiers("x-transition", AttributeUtil.transitionModifiers)

        for (stage in stages) {
            descriptors.add(AttributeInfo("x-transition:$stage"))
            addModifiers("x-transition:$stage", AttributeUtil.transitionModifiers)
        }
    }

    private fun addEvent(descriptor: XmlAttributeDescriptor) {
        val event = descriptor.name.substring(2)
        for (prefix in AttributeUtil.eventPrefixes) {
            descriptors.add(AttributeInfo(prefix + event))

            addModifiers("$prefix$event", AttributeUtil.eventModifiers)

            if (event.toLowerCase() == "keydown" || event.toLowerCase() == "keyup") {
                addModifiers("$prefix$event", AttributeUtil.keypressModifiers)
            }
        }
    }

    private fun addBoundAttribute(descriptor: XmlAttributeDescriptor) {
        for (prefix in AttributeUtil.getBindPrefixes(htmlTag)) {
            descriptors.add(AttributeInfo(prefix + descriptor.name))
        }
    }

    private fun addModifiers(modifiableDirective: String, modifiers: Array<String>) {
        if (!partialAttribute.startsWith(modifiableDirective)) {
            return
        }

        var withExistingModifiers = partialAttribute

        if (partialAttribute.contains('.')) {
            withExistingModifiers = partialAttribute.substringBeforeLast('.')
        }

        for (modifier in modifiers) {
            if (!partialAttribute.contains(".$modifier")) {
                descriptors.add(AttributeInfo("$withExistingModifiers.$modifier"))
            }
        }

        val timeLimits = arrayOf(
            "75ms",
            "100ms",
            "150ms",
            "200ms",
            "300ms",
            "500ms",
            "700ms",
            "1000ms",
        )
        for (timeUnitModifier in AttributeUtil.timeUnitModifiers) {
            if (withExistingModifiers.endsWith(".$timeUnitModifier")) {
                for (timeLimit in timeLimits) {
                    descriptors.add(AttributeInfo("$withExistingModifiers.$timeLimit"))
                }
            }
        }

        val numbers = arrayOf("10", "20", "30", "40", "50", "60", "70", "80", "90")
        if (withExistingModifiers.endsWith(".scale")) {
            for (number in numbers) {
                descriptors.add(AttributeInfo("$withExistingModifiers.$number"))
            }
        }

        val origins = arrayOf("top", "bottom", "left", "right")
        if (withExistingModifiers.endsWith(".origin")) {
            for (origin in origins) {
                descriptors.add(AttributeInfo("$withExistingModifiers.$origin"))
            }
        }
    }

    private fun getDefaultHtmlAttributes(htmlTag: XmlTag): Array<out XmlAttributeDescriptor> {
        val tagDescriptor = htmlTag.descriptor as? HtmlElementDescriptorImpl
        val descriptor = tagDescriptor ?: HtmlNSDescriptorImpl.guessTagForCommonAttributes(htmlTag)

        return (descriptor as? HtmlElementDescriptorImpl)?.getDefaultAttributeDescriptors(htmlTag) ?: emptyArray()
    }
}
