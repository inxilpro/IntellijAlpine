package com.github.inxilpro.intellijalpine

@Suppress("MemberVisibilityCanBePrivate")
class AttributeInfo(val attribute: String) {
    private val typeTexts = hashMapOf<String, String>(
        "x-data" to "New Alpine.js component scope",
        "x-init" to "Run on initialization",
        "x-show" to "Toggles 'display: none'",
        "x-model" to "Add two-way binding",
        "x-text" to "Bind to element's inner text",
        "x-html" to "Bind to element's inner HTML",
        "x-ref" to "Create a reference for later use",
        "x-if" to "Conditionally render template",
        "x-for" to "Map array to DOM nodes",
        "x-transition" to "Apply transition classes",
        "x-spread" to "Bind reusable directives",
        "x-cloak" to "Hide while Alpine is initializing",
    )

    val name: String

    val prefix: String

    val typeText: String

    init {
        prefix = extractPrefix()
        name = attribute.substring(prefix.length)
        typeText = buildTypeText()
    }

    fun isEvent(): Boolean {
        return "@" == prefix || "x-on:" == prefix
    }

    fun isBound(): Boolean {
        return ":" == prefix || "x-bind:" == prefix
    }

    fun isDirective(): Boolean {
        return AttributeUtil.directives.contains(name)
    }

    fun hasValue(): Boolean {
        return "x-cloak" != name
    }

    @Suppress("ReturnCount")
    private fun extractPrefix(): String {
        for (eventPrefix in AttributeUtil.eventPrefixes) {
            if (attribute.startsWith(eventPrefix)) {
                return eventPrefix
            }
        }

        for (bindPrefix in AttributeUtil.bindPrefixes) {
            if (attribute.startsWith(bindPrefix)) {
                return bindPrefix
            }
        }

        return ""
    }

    @Suppress("ReturnCount")
    private fun buildTypeText(): String {
        if (isEvent()) {
            return "'$name' listener"
        }

        if (isBound()) {
            return "Bind '$name' attribute"
        }

        return typeTexts.getOrDefault(name, "Alpine.js")
    }
}
