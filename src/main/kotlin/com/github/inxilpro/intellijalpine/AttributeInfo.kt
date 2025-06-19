package com.github.inxilpro.intellijalpine

@Suppress("MemberVisibilityCanBePrivate")
class AttributeInfo(val attribute: String) {
    private val typeTexts = hashMapOf<String, String>(
        "x-data" to "New Alpine.js component scope",
        "x-init" to "Run on initialization",
        "x-show" to "Toggles 'display: none'",
        "x-model" to "Add two-way binding",
        "x-modelable" to "Expose x-model target",
        "x-text" to "Bind to element's inner text",
        "x-html" to "Bind to element's inner HTML",
        "x-ref" to "Create a reference for later use",
        "x-if" to "Conditionally render template",
        "x-id" to "Register \$id() scope",
        "x-for" to "Map array to DOM nodes",
        "x-transition" to "Add transition classes",
        "x-transition:enter" to "Transition classes used during the entire entering phase",
        "x-transition:enter-start" to "Transition classes for start of entering phase",
        "x-transition:enter-end" to "Transition classes end of entering phase",
        "x-transition:leave" to "Transition classes used during the entire leaving phase",
        "x-transition:leave-start" to "Transition classes for start of leaving phase",
        "x-transition:leave-end" to "Transition classes for end of leaving phase",
        "x-effect" to "Add reactive effect",
        "x-ignore" to "Ignore DOM node in Alpine.js",
        "x-spread" to "Bind reusable directives",
        "x-cloak" to "Hide while Alpine is initializing",
        "x-teleport" to "Teleport template to another DOM node",
        "x-on" to "Add listener",
        "x-bind" to "Bind an attribute",
        "x-mask" to "Set input mask",
        "x-mask:dynamic" to "Set dynamic input mask",
        "x-intersect" to "Bind an intersection observer",
        "x-trap" to "Add focus trap",
        "x-collapse" to "Collapse element when hidden",
        "x-wizard:step" to "Add wizard step",
        "x-wizard:if" to "Add wizard condition",
        "x-wizard:title" to "Add title to wizard step",
    )

    val name: String

    val prefix: String

    val typeText: String

    init {
        prefix = extractPrefix()
        name = attribute.substring(prefix.length).substringBefore('.')
        typeText = buildTypeText()
    }

    @Suppress("ComplexCondition")
    fun isAlpine(): Boolean {
        return this.isEvent() || this.isBound() || this.isTransition() || this.isDirective() || this.isWizard()
    }

    fun isEvent(): Boolean {
        return "@" == prefix || "x-on:" == prefix
    }

    fun isBound(): Boolean {
        return ":" == prefix || "x-bind:" == prefix
    }

    fun isTransition(): Boolean {
        return "x-transition:" == prefix
    }

    fun isDirective(): Boolean {
        return AttributeUtil.directives.contains(name)
    }

    fun isWizard(): Boolean {
        return "x-wizard:" == prefix
    }

    fun hasValue(): Boolean {
        return "x-cloak" != name && "x-ignore" != name
    }

    fun canBePrefix(): Boolean {
        return "x-bind" == name || "x-transition" == name || "x-on" == name || "x-wizard" == name
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

        if (attribute.startsWith("x-transition:")) {
            return "x-transition:"
        }

        if (attribute.startsWith("x-wizard:")) {
            return "x-wizard:"
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

        if (isTransition()) {
            return "CSS classes for '$name' transition phase"
        }

        return typeTexts.getOrDefault(attribute, "Alpine.js")
    }
}
