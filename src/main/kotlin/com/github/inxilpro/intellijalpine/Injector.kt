package com.github.inxilpro.intellijalpine

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl
import com.intellij.psi.impl.source.xml.XmlTextImpl
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import java.util.Arrays

class Injector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, host: PsiElement) {
        val range = ElementManipulators.getValueTextRange(host)

        if (host is XmlAttributeValue) {
            val parent = host.getParent()
            if (parent is XmlAttribute && parent.parent is XmlTag) {
                for (directive in Alpine.allDirectives(parent.parent.name)) {
                    if (directive == parent.name) {
                        val directivePrefix = getPrefix(directive)

                        registrar.startInjecting(JavascriptLanguage.INSTANCE)
                            .addPlace(directivePrefix, ";", host as PsiLanguageInjectionHost, range)
                            .doneInjecting()

                        return
                    }
                }
            }
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return Arrays.asList(XmlTextImpl::class.java, XmlAttributeValueImpl::class.java)
    }

    fun getPrefix(directive: String): String {
        var prefix = ""

        val generalPrefix =
            """
                /** @type HTMLElement */
                let ${'$'}el;

                /** @type Object */
                let ${'$'}refs;

                /**
                 * @param {Event|string} event
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

        val eventDeclaration =
            """
                /** @type Event */
                let ${'$'}event;
            """.trimIndent()

        if ("x-data" == directive) {
            prefix = "let __data = "
        } else {
            prefix = generalPrefix
        }

        if (directive.startsWith('@') || directive.startsWith("x-on:")) {
            prefix += eventDeclaration
        }

        return prefix
    }
}
