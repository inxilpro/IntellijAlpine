package com.github.inxilpro.intellijalpine

import com.intellij.psi.html.HtmlTag
import com.intellij.psi.impl.source.html.dtd.HtmlAttributeDescriptorImpl
import com.intellij.psi.impl.source.html.dtd.HtmlElementDescriptorImpl
import com.intellij.psi.impl.source.html.dtd.HtmlNSDescriptorImpl
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.XmlAttributeDescriptor
import java.util.Arrays
import java.util.Collections

object AttributeUtil {
    private val validAttributes = mutableMapOf<String, Array<AttributeInfo>>()

    val xmlPrefixes = arrayOf(
        "x-on",
        "x-bind",
        "x-transition",
        "x-wizard", // glhd/alpine-wizard pacakge
        "x-target", // alpine-ajax
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

        // Alpine AJAX directives
        "x-target",
        "x-headers",
        "x-merge",
        "x-autofocus",
        "x-sync",
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
    
    val targetModifiers = arrayOf(
        "200",
        "301",
        "302",
        "303",
        "400",
        "401",
        "403",
        "404",
        "422",
        "500",
        "502",
        "503",
        "2xx",
        "3xx",
        "4xx",
        "5xx",
        "back",
        "away",
        "replace",
        "push",
        "error",
        "nofocus",
    )

    // Taken from https://developer.mozilla.org/en-US/docs
    val nameToInterfaceEventMap: Map<String, String> = mapOf(
        Pair("afterscriptexecute", "Event"),
        Pair("animationcancel", "AnimationEvent"),
        Pair("animationend", "AnimationEvent"),
        Pair("animationiteration", "AnimationEvent"),
        Pair("animationstart", "AnimationEvent"),
        Pair("auxclick", "PointerEvent"),
        Pair("beforematch", "Event"),
        Pair("beforescriptexecute", "Event"),
        Pair("beforexrselect", "XRSessionEvent"),
        Pair("blur", "FocusEvent"),
        Pair("click", "PointerEvent"),
        Pair("compositionend", "CompositionEvent"),
        Pair("compositionstart", "CompositionEvent"),
        Pair("compositionupdate", "CompositionEvent"),
        Pair("contentvisibilityautostatechange", "ContentVisibilityAutoStateChangeEvent"),
        Pair("contextmenu", "PointerEvent"),
        Pair("copy", "ClipboardEvent"),
        Pair("cut", "ClipboardEvent"),
        Pair("dblclick", "MouseEvent"),
        Pair("DOMActivate", "MouseEvent"),
        Pair("DOMMouseScroll", "WheelEvent"),
        Pair("focus", "FocusEvent"),
        Pair("focusin", "FocusEvent"),
        Pair("focusout", "FocusEvent"),
        Pair("fullscreenchange", "Event"),
        Pair("fullscreenerror", "Event"),
        Pair("gesturechange", "GestureEvent"),
        Pair("gestureend", "GestureEvent"),
        Pair("gesturestart", "GestureEvent"),
        Pair("gotpointercapture", "PointerEvent"),
        Pair("keydown", "KeyboardEvent"),
        Pair("keypress", "KeyboardEvent"),
        Pair("keyup", "KeyboardEvent"),
        Pair("lostpointercapture", "PointerEvent"),
        Pair("mousedown", "MouseEvent"),
        Pair("mouseenter", "MouseEvent"),
        Pair("mouseleave", "MouseEvent"),
        Pair("mousemove", "MouseEvent"),
        Pair("mouseout", "MouseEvent"),
        Pair("mouseover", "MouseEvent"),
        Pair("mouseup", "MouseEvent"),
        Pair("mousewheel", "WheelEvent"),
        Pair("MozMousePixelScroll", "WheelEvent"),
        Pair("paste", "ClipboardEvent"),
        Pair("pointercancel", "PointerEvent"),
        Pair("pointerdown", "PointerEvent"),
        Pair("pointerenter", "PointerEvent"),
        Pair("pointerleave", "PointerEvent"),
        Pair("pointermove", "PointerEvent"),
        Pair("pointerout", "PointerEvent"),
        Pair("pointerover", "PointerEvent"),
        Pair("pointerrawupdate", "PointerEvent"),
        Pair("pointerup", "PointerEvent"),
        Pair("scroll", "Event"),
        Pair("scrollend", "Event"),
        Pair("securitypolicyviolation", "SecurityPolicyViolationEvent"),
        Pair("touchcancel", "TouchEvent"),
        Pair("touchend", "TouchEvent"),
        Pair("touchmove", "TouchEvent"),
        Pair("touchstart", "TouchEvent"),
        Pair("transitioncancel", "TransitionEvent"),
        Pair("transitionend", "TransitionEvent"),
        Pair("transitionrun", "TransitionEvent"),
        Pair("transitionstart", "TransitionEvent"),
        Pair("webkitmouseforcechanged", "MouseEvent"),
        Pair("webkitmouseforcedown", "MouseEvent"),
        Pair("webkitmouseforceup", "MouseEvent"),
        Pair("webkitmouseforcewillbegin", "MouseEvent"),
        Pair("wheel", "WheelEvent"),
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

    fun isValidInjectionTarget(host: XmlAttributeValue): Boolean {
        // Make sure that we have an XML attribute as a parent
        val attribute = host.parent as? XmlAttribute ?: return false

        // Make sure we have an HTML tag (and not a Blade <x- tag)
        val tag = attribute.parent as? HtmlTag ?: return false
        if (!isValidHtmlTag(tag)) {
            return false
        }

        // Make sure we have an attribute that looks like it's Alpine
        val attributeName = attribute.name
        if (!isAlpineAttributeName(attributeName)) {
            return false
        }

        // Make sure it's a valid Attribute to operate on
        if (!isValidAttribute(attribute)) {
            return false
        }

        // Make sure it's an attribute that is parsed as JavaScript
        if (!shouldInjectJavaScript(attributeName)) {
            return false
        }

        return true
    }

    fun getEventNameFromDirective(directive: String): String {
        return nameToInterfaceEventMap[eventPrefixes.fold(directive) { acc, s -> acc.removePrefix(s) }.split(".")
            .first()]
            ?: "Event"
    }

    private fun isValidAttribute(attribute: XmlAttribute): Boolean {
        return attribute.descriptor is HtmlAttributeDescriptorImpl || attribute.descriptor is AlpineAttributeDescriptor
    }

    private fun isValidHtmlTag(tag: HtmlTag): Boolean {
        return !tag.name.startsWith("x-")
    }

    private fun isAlpineAttributeName(name: String): Boolean {
        return name.startsWith("x-") || name.startsWith("@") || name.startsWith(':')
    }

    private fun shouldInjectJavaScript(name: String): Boolean {
        // x-target:dynamic should still inject JavaScript, but plain x-target should not
        if (name == "x-target") return false
        
        return !name.startsWith("x-transition:") && "x-mask" != name && "x-modelable" != name && "x-autofocus" != name && "x-sync" != name
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
