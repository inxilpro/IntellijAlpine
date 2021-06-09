package com.github.inxilpro.intellijalpine

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.impl.source.html.dtd.HtmlAttributeDescriptorImpl
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl
import com.intellij.psi.impl.source.xml.XmlTextImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag

class Injector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, host: PsiElement) {
        val range = ElementManipulators.getValueTextRange(host)

        if (host is XmlAttributeValue) {
            val parent = host.getParent() as? XmlAttribute ?: return
            if (
                parent.descriptor is HtmlAttributeDescriptorImpl
                || parent.descriptor is AlpineAttributeDescriptor
            ) {
                if (isJavaScriptAlpineAttribute(parent) && isPossibleAlpineTag(parent.parent)) {
                    registrar.startInjecting(JavascriptLanguage.INSTANCE)
                        .addPlace(getPrefix(parent.name, host), getSuffix(parent.name), host as PsiLanguageInjectionHost, range)
                        .doneInjecting()
                }
            }
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(XmlTextImpl::class.java, XmlAttributeValueImpl::class.java)
    }

    private fun isPossibleAlpineTag(tag: XmlTag): Boolean {
        return !tag.name.startsWith("x-")
    }

    private fun isJavaScriptAlpineAttribute(attribute: XmlAttribute): Boolean {
        return isAlpineAttribute(attribute) && !attribute.name.startsWith("x-transition:")
    }

    private fun isAlpineAttribute(attribute: XmlAttribute): Boolean {
        if (attribute.parent is HtmlTag) {
            for (directive in AttributeUtil.getValidAttributes(attribute.parent as HtmlTag)) {
                if (directive == attribute.name) {
                    return true
                }
            }
        }

        return false
    }

    private fun getPrefix(directive: String, host: PsiElement): String {
        var prefix = ""

        // First we'll add the Alpine x-data context if we can
        val dataParent = PsiTreeUtil.findFirstParent(host) { it is HtmlTag && it.getAttribute("x-data") != null }
        if (dataParent is HtmlTag) {
            val xData = dataParent.getAttribute("x-data")?.value;
            if (null != xData) {
                prefix += "with (${xData}) { "
            }
        } else {
            prefix += "with ({}) { "
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
            prefix += " return "
        }

        return prefix
    }

    private fun getSuffix(directive: String): String {
        if ("x-for" == directive) {
            return ") {}; }"
        }

        if ("x-spread" == directive) {
            return "() }"
        }

        return " }"
    }
}
