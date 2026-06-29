package com.github.inxilpro.intellijalpine.plugins

import com.github.inxilpro.intellijalpine.attributes.AttributeInfo
import com.github.inxilpro.intellijalpine.completion.AutoCompleteSuggestions
import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.github.inxilpro.intellijalpine.core.JsContext

class TooltipPlugin : AlpinePlugin {

    override fun getPluginName(): String = "alpine-tooltip"

    override fun getPackageDisplayName(): String = "alpine-tooltip"

    override fun getPackageNamesForDetection(): List<String> = listOf(
        "alpine-tooltip",
        "@ryangjchandler/alpine-tooltip"
    )

    override fun injectAutoCompleteSuggestions(suggestions: AutoCompleteSuggestions) {
        val modifiers = arrayOf(
            "duration",
            "delay",
            "cursor",
            "on",
            "arrowless",
            "html",
            "interactive",
            "border",
            "debounce",
            "max-width",
            "theme",
            "placement",
            "animation",
            "no-flip",
        )

        suggestions.addModifiers("x-tooltip", modifiers)
    }

    override fun getTypeText(info: AttributeInfo): String? {
        return when (info.attribute) {
            "x-tooltip" -> "Tippy.js tooltip"
            else -> null
        }
    }

    override fun injectJsContext(context: JsContext): JsContext {
        val magics = """
            /**
             * @param {string} value
             * @param {Object} options
             * @return {Promise<Response>}
             */
            function ${'$'}tooltip(value, options = {}) {}

        """.trimIndent()

        return JsContext(context.prefix + magics, context.suffix)
    }

    override fun getDirectives(): List<String> = listOf(
        "x-tooltip",
    )
}