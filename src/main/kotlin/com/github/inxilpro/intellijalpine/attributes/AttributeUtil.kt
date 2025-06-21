package com.github.inxilpro.intellijalpine.attributes

import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.github.inxilpro.intellijalpine.support.LanguageUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.impl.source.html.dtd.HtmlAttributeDescriptorImpl
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue

object AttributeUtil {
    private val corePrefixes = listOf(
        "x-on",
        "x-bind",
        "x-transition",
    )

    val prefixes: List<String> by lazy {
        AlpinePluginRegistry.instance.getRegisteredPlugins()
            .flatMap { it.getPrefixes() }
            .union(corePrefixes)
            .toList()
    }

    private val coreDirectives = listOf(
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

    val directives: List<String> by lazy {
        AlpinePluginRegistry.instance.getRegisteredPlugins()
            .flatMap { it.getDirectives() }
            .union(coreDirectives)
            .toList()
    }

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

    fun getDirectivesForProject(project: Project): Array<String> {
        val pluginDirectives = AlpinePluginRegistry.instance.getAllDirectives(project)
        return (directives.toList() + pluginDirectives).toTypedArray()
    }

    fun getXmlPrefixesForProject(project: Project): Array<String> {
        val pluginPrefixes = AlpinePluginRegistry.instance.getAllPrefixes(project)
        return (prefixes.toList() + pluginPrefixes).toTypedArray()
    }

    fun isXmlPrefix(prefix: String): Boolean {
        return prefixes.contains(prefix)
    }

    fun isTemplateDirective(directive: String): Boolean {
        return templateDirectives.contains(directive)
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
        if (!LanguageUtil.supportsAlpineJs(host.containingFile)) {
            return false
        }

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
        if (!shouldInjectJavaScript(attributeName, host.containingFile.project)) {
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

    private fun shouldInjectJavaScript(name: String, project: Project): Boolean {
        // Never inject for these core attributes
        if (name.startsWith("x-transition:") || name == "x-mask" || name == "x-modelable") {
            return false
        }


        val enabledPlugins = AlpinePluginRegistry.instance.getEnabledPlugins(project)
        for (plugin in enabledPlugins) {
            val pluginDirectives = plugin.getDirectives()
            val pluginPrefixes = plugin.getPrefixes()

            // If this attribute belongs to this plugin, let the plugin decide
            if (pluginDirectives.contains(name) || pluginPrefixes.any { name.startsWith("$it:") }) {
                return plugin.directiveSupportJavaScript(name)
            }
        }

        // For core attributes and unknown attributes, default to true (inject JS)
        return true
    }
}