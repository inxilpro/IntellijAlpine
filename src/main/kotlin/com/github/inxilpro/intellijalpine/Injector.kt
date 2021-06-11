package com.github.inxilpro.intellijalpine

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.impl.source.html.dtd.HtmlAttributeDescriptorImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue

class Injector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, host: PsiElement) {
        if (host !is XmlAttributeValue) {
            return
        }

        // Make sure that we have an XML attribute as a parent
        val attribute = host.parent as? XmlAttribute ?: return

        // Make sure we have an HTML tag (and not a Blade <x- tag)
        val tag = attribute.parent as? HtmlTag ?: return
        if (!isValidHtmlTag(tag)) {
            return
        }

        // Make sure we have an attribute that looks like it's Alpine
        val attributeName = attribute.name
        if (!isAlpineAttributeName(attributeName)) {
            return
        }

        // Make sure it's a valid Attribute to operate on
        if (!isValidAttribute(attribute)) {
            return
        }

        // Make sure it's an attribute that is parsed as JavaScript
        if (!shouldInjectJavaScript(attributeName)) {
            return
        }

        // Alright. Now that we have an Alpine attribute that needs
        // language injection, let's set it up

        var prefix = getPrefix(attributeName, host)
        var suffix = ""

        val content = host.text
        val ranges = getJavaScriptRange(host, content)

        ranges.forEachIndexed { index, range ->
            if (index == ranges.lastIndex) {
                suffix = getSuffix(attributeName)
            }

            registrar.startInjecting(JavascriptLanguage.INSTANCE)
                .addPlace(prefix, suffix, host as PsiLanguageInjectionHost, range)
                .doneInjecting()

            if (ranges.lastIndex != index) {
                prefix += range.substring(content)
                prefix += "__PHP_CALL()"
            }
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(XmlAttributeValue::class.java)
    }

    private fun getJavaScriptRange(host: PsiElement, content: String): List<TextRange> {
        val valueRange = ElementManipulators.getValueTextRange(host)

        if (host.containingFile.viewProvider.languages.filter { "PHP" == it.id || "Blade" == it.id }.isEmpty()) {
            return listOf(valueRange)
        }

        val phpMatcher = Regex("(?:(?<!@)\\{\\{.+?}}|<\\?(?:=|php).+?\\?>)")
        val ranges = mutableListOf<TextRange>()

        var offset = valueRange.startOffset
        phpMatcher.findAll(content).forEach {
            ranges.add(TextRange(offset, it.range.first))
            offset = it.range.last + 1
        }

        ranges.add(TextRange(offset, valueRange.endOffset))

        return ranges.toList()
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
        return !name.startsWith("x-transition:")
    }

    private fun getPrefix(directive: String, host: PsiElement): String {
        var prefix = ""

        // First we'll add the Alpine x-data context if we can
        val dataParent = PsiTreeUtil.findFirstParent(host) { it is HtmlTag && it.getAttribute("x-data") != null }
        if (dataParent is HtmlTag) {
            val xData = dataParent.getAttribute("x-data")?.value
            if (null != xData) {
                prefix += "with ($xData) { \n"
            }
        } else {
            prefix += "with ({}) { \n"
        }

        // Next we'll set up the available Alpine magic properties/etc if we're
        // inside of an existing Alpine context
        if ("x-data" != directive) {
            prefix += """
                /** @type HTMLElement */
                let ${'$'}el;

                /** @type Object */
                let ${'$'}refs;

                /**
                 * @param {string} event
                 * @param {Object} detail
                 * @return boolean
                 */
                function ${'$'}dispatch(event, detail = {}) {}

                /**
                 * @param {Function} callback
                 * @return void
                 */
                function ${'$'}nextTick(callback) {}

                /**
                 * @param {string} property
                 * @param {Function} callback
                 * @return void
                 */
                function ${'$'}watch(property, callback) {}
            """.trimIndent()
        }

        prefix += "\n"

        // Handle a few different edge-cases in terms of how the attribute
        // should be parsed (i.e. are we returning something, or executing
        // code for an event callback, or in a loop, etc).
        if (AttributeUtil.isEvent(directive)) {
            prefix +=
                """
                    /** @type Event */
                    let ${'$'}event;
                """.trimIndent()
        } else if ("x-for" == directive) {
            prefix += "for (let "
        } else {
            prefix += " __ALPINE_DIRECTIVE("
        }

        return prefix
    }

    private fun getSuffix(directive: String): String {
        if ("x-for" == directive) {
            return ") {}; }"
        }

        if ("x-spread" == directive) {
            return "()) }"
        }

        return ") }"
    }
}
