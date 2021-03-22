package com.github.inxilpro.intellijalpine

import com.intellij.openapi.util.IconLoader

object Alpine {
    val ICON = IconLoader.getIcon("/META-INF/pluginIcon.svg", Alpine::class.java)

    val DIRECTIVES = arrayOf(
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

    val EVENT_PREFIXES = arrayOf(
        "@",
        "x-on:"
    )

    val BIND_PREFIXES = arrayOf(
        ":",
        "x-bind:"
    )

    fun allDirectives(name: String): Array<String> {
        val descriptors = mutableListOf<String>()

        for (directive in DIRECTIVES) {
            descriptors.add(directive)
        }

        /*
        if (BindingsMap.containsKey(name)) {
            for (prefix in BIND_PREFIXES) {
                for (directive in BindingsMap[name]!!) {
                    descriptors.add(prefix + directive)
                }
            }
        }

        if (EventsMap.containsKey(name)) {
            for (prefix in EVENT_PREFIXES) {
                for (event in EventsMap[name]!!) {
                    descriptors.add(prefix + event)
                }
            }
        }
        */

        return descriptors.toTypedArray()
    }
}
