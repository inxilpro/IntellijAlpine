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
import org.apache.commons.lang3.tuple.MutablePair

class AlpineJavaScriptAttributeValueInjector : MultiHostInjector {
    private companion object {
        val globalMagics =
            """
                /**
                 * @param {*<ValueToPersist>} value
                 * @return {ValueToPersist}
                 * @template ValueToPersist
                 */
                function ${'$'}persist(value) {}
                
                /**
                 * @param {*<ValueForQueryString>} value
                 * @return {ValueForQueryString}
                 * @template ValueForQueryString
                 */
                function ${'$'}queryString(value) {}
                       
            """.trimIndent()

        val coreMagics =
            """
                /** @type {HTMLElement} */
                let ${'$'}el;

                /** @type {Object} */
                let ${'$'}refs;
                
                /** @type {Object} */
                let ${'$'}store;

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

        val eventMagics = "/** @type {Event} */\nlet ${'$'}event;\n\n"
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, host: PsiElement) {
        if (host !is XmlAttributeValue) {
            return
        }
        if (!isValidInjectionTarget(host)) {
            return
        }

        val attribute = host.parent as? XmlAttribute ?: return
        val attributeName = attribute.name

        val content = host.text
        val ranges = getJavaScriptRanges(host, content)

        var (prefix, suffix) = getPrefixAndSuffix(attributeName, host)

        registrar.startInjecting(JavascriptLanguage.INSTANCE)

        ranges.forEachIndexed { index, range ->
            if (index == ranges.lastIndex) {
                registrar.addPlace(prefix, suffix, host as PsiLanguageInjectionHost, range)
            } else {
                registrar.addPlace(prefix, "", host as PsiLanguageInjectionHost, range)
            }

            if (ranges.lastIndex != index) {
                prefix += range.substring(content)
                prefix += "__PHP_CALL()"
            }
        }

        registrar.doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(XmlAttributeValue::class.java)
    }

    private fun getJavaScriptRanges(host: XmlAttributeValue, content: String): List<TextRange> {
        val valueRange = ElementManipulators.getValueTextRange(host)

        if (host.containingFile.viewProvider.languages.filter { "PHP" == it.id || "Blade" == it.id }.isEmpty()) {
            return listOf(valueRange)
        }

        val phpMatcher = Regex("(?:(?<!@)\\{\\{.+?}}|<\\?(?:=|php).+?\\?>|@[a-zA-Z]+\\(.*\\)(?:\\.defer)?)")
        val ranges = mutableListOf<TextRange>()

        var offset = valueRange.startOffset
        phpMatcher.findAll(content).forEach {
            ranges.add(TextRange(offset, it.range.first))
            offset = it.range.last + 1
        }

        ranges.add(TextRange(offset, valueRange.endOffset))

        return ranges.toList()
    }

    private fun isValidInjectionTarget(host: XmlAttributeValue): Boolean {
        // Make sure that we have an XML attribute as a parent
        val attribute = host.parent as? XmlAttribute ?: return false

        // Make sure we have an HTML tag (and not a Blade <x- tag)
        val tag = attribute.parent as? HtmlTag ?: return false
        val attributeName = attribute.name

        if (isBladeComponentBindAttribute(tag, attributeName)) {
            return false
        }

        // Make sure we have an attribute that looks like it's Alpine
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

    private fun isValidAttribute(attribute: XmlAttribute): Boolean {
        return attribute.descriptor is HtmlAttributeDescriptorImpl || attribute.descriptor is AlpineAttributeDescriptor
    }

    private fun isBladeComponentBindAttribute(tag: HtmlTag, name: String): Boolean {
        return AttributeUtil.isBladeComponentTag(tag)
                && name.startsWith(":")
                && !name.startsWith("::")
    }

    private fun isAlpineAttributeName(name: String): Boolean {
        return name.startsWith("x-") || name.startsWith("@") || name.startsWith(':')
    }

    private fun shouldInjectJavaScript(name: String): Boolean {
        return !name.startsWith("x-transition:") && "x-ref" != name
    }

    private fun getPrefixAndSuffix(directive: String, host: XmlAttributeValue): Pair<String, String> {
        val context = MutablePair(globalMagics, "")

        if ("x-data" != directive) {
            context.left = coreMagics + context.left
        }

        if ("x-spread" == directive) {
            context.right += "()"
        }

        if (AttributeUtil.isEvent(directive)) {
            context.left += eventMagics
        } else if ("x-for" == directive) {
            context.left += "for (let "
            context.right += ") {}\n"
        } else if ("x-init" == directive) {
            // We want x-init to skip the directive wrapping
        } else {
            context.left += "__ALPINE_DIRECTIVE(\n"
            context.right += "\n)"
        }

        addWithData(host, directive, context)

        return context.toPair()
    }

    private fun addWithData(host: XmlAttributeValue, directive: String, context: MutablePair<String, String>) {
        var dataParent: HtmlTag?

        if ("x-data" == directive) {
            val parentTag = PsiTreeUtil.findFirstParent(host) { it is HtmlTag } ?: return
            dataParent = PsiTreeUtil.findFirstParent(parentTag) {
                it != parentTag && it is HtmlTag && it.getAttribute("x-data") != null
            } as HtmlTag?
        } else {
            dataParent = PsiTreeUtil.findFirstParent(host) {
                it is HtmlTag && it.getAttribute("x-data") != null
            } as HtmlTag?
        }

        if (dataParent is HtmlTag) {
            val data = dataParent.getAttribute("x-data")?.value
            if (null != data) {
                val (prefix, suffix) = context
                context.left = "with ($data) {\n$prefix"
                context.right = "$suffix\n}"
            }
        }
    }
}
