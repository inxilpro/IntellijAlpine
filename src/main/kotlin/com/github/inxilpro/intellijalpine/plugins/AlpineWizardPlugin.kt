package com.github.inxilpro.intellijalpine.plugins

import com.github.inxilpro.intellijalpine.attributes.AttributeInfo
import com.github.inxilpro.intellijalpine.completion.AutoCompleteSuggestions
import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import org.apache.commons.lang3.tuple.MutablePair

class AlpineWizardPlugin : AlpinePlugin {

    override fun getPluginName(): String = "alpine-wizard"

    override fun getPackageDisplayName(): String = "alpine-wizard"

    override fun getPackageNamesForDetection(): List<String> = listOf(
        "alpine-wizard",
        "@glhd/alpine-wizard"
    )

    override fun getTypeText(info: AttributeInfo): String? {
        if ("x-wizard:" == info.prefix) {
            return when (info.name) {
                "step" -> "Define wizard step"
                "if" -> "Conditional wizard step"
                "title" -> "Set step title"
                else -> "Alpine Wizard directive"
            }
        }

        return when (info.attribute) {
            "x-wizard:step" -> "Define wizard step"
            "x-wizard:if" -> "Conditional wizard step"
            "x-wizard:title" -> "Set step title"
            else -> null
        }
    }

    override fun injectJsContext(context: MutablePair<String, String>): MutablePair<String, String> {
        val wizardMagics = """
            class AlpineWizardStep {
                /** @type {HTMLElement} */ el;
                /** @type {string} */ title;
                /** @type {boolean} */ is_applicable;
                /** @type {boolean} */ is_complete;
            }

            class AlpineWizardProgress {
                /** @type {number} */ current;
                /** @type {number} */ total;
                /** @type {number} */ complete;
                /** @type {number} */ incomplete;
                /** @type {string} */ percentage;
                /** @type {number} */ percentage_int;
                /** @type {number} */ percentage_float;
            }

            class AlpineWizardMagic {
                /** @returns {AlpineWizardStep} */ current() {}
                /** @returns {AlpineWizardStep|null} */ next() {}
                /** @returns {AlpineWizardStep|null} */ previous() {}
                /** @returns {AlpineWizardProgress} */ progress() {}
                /** @returns {boolean} */ isFirst() {}
                /** @returns {boolean} */ isNotFirst() {}
                /** @returns {boolean} */ isLast() {}
                /** @returns {boolean} */ isNotLast() {}
                /** @returns {boolean} */ isComplete() {}
                /** @returns {boolean} */ isNotComplete() {}
                /** @returns {boolean} */ isIncomplete() {}
                /** @returns {boolean} */ canGoForward() {}
                /** @returns {boolean} */ cannotGoForward() {}
                /** @returns {boolean} */ canGoBack() {}
                /** @returns {boolean} */ cannotGoBack() {}
                /** @returns {void} */ forward() {}
                /** @returns {void} */ back() {}
            }

            /** @type {AlpineWizardMagic} */
            let ${'$'}wizard;
            
        """.trimIndent()

        return MutablePair(context.left + wizardMagics, context.right)
    }

    override fun injectAutoCompleteSuggestions(suggestions: AutoCompleteSuggestions) {
        suggestions.descriptors.add(AttributeInfo("x-wizard:step"))
        suggestions.addModifiers("x-wizard:step", arrayOf("rules"))

        suggestions.descriptors.add(AttributeInfo("x-wizard:if"))
        suggestions.descriptors.add(AttributeInfo("x-wizard:title"))
    }

    override fun getDirectives(): List<String> = listOf(
        "x-wizard:step",
        "x-wizard:if",
        "x-wizard:title"
    )

    override fun getPrefixes(): List<String> = listOf(
        "x-wizard"
    )
}