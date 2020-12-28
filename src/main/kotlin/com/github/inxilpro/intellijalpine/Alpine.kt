package com.github.inxilpro.intellijalpine

import com.intellij.openapi.util.IconLoader

object Alpine {
    val ICON = IconLoader.getIcon("/META-INF/pluginIcon.svg", Alpine::class.java)

    val DIRECTIVES = arrayOf(
        "x-data",
        "x-init",
        "x-show",
        "x-bind",
        "x-on",
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

    val EVENT_PREFIXES = arrayOf(
        "@",
        "x-on:"
    )

    val COMMON_EVENTS = arrayOf(
        "error",
        "load",
        "beforeunload",
        "unload",
        "focus",
        "blur",
        "focusin",
        "focusout",
        "reset",
        "submit",
        "resize",
        "scroll",
        "keydown",
        "keypress",
        "keyup",
        "click",
        "mousedown",
        "mouseenter",
        "mouseleave",
        "mousemove",
        "mouseout",
        "mouseup",
        "select",
        "input",
        "readystatechange",
    )

    val MAGIC_PROPERTIES = arrayOf(
        "el",
        "refs",
        "event",
        "dispatch",
        "nextTick",
        "watch",
    )

    fun allDirectives(): Array<String> {
        val descriptors = mutableListOf<String>()

        for (directive in DIRECTIVES) {
            descriptors.add(directive)
        }

        for (prefix in EVENT_PREFIXES) {
            for (event in COMMON_EVENTS) {
                descriptors.add(prefix + event)
            }
        }

        return descriptors.toTypedArray()
    }
}
