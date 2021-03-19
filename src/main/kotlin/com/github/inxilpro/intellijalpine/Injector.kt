package com.github.inxilpro.intellijalpine

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.javascript.JavascriptLanguage
// import com.intellij.openapi.util.TextRange
// import com.jetbrains.php.lang.PhpLanguage;
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
// import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl
import com.intellij.psi.impl.source.xml.XmlTextImpl
// import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
// import com.intellij.psi.xml.XmlElementType
// import com.intellij.psi.xml.XmlTokenType
import java.util.Arrays

class Injector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, host: PsiElement) {
        val range = ElementManipulators.getValueTextRange(host)

        val prefix =
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

        var eventDeclaration = """
            /** @type Event */
            let ${'$'}event;
        """.trimIndent()

        var eventPrefix = prefix + eventDeclaration

        if (host is XmlAttributeValue) {
            val parent = host.getParent()
            if (parent is XmlAttribute) {
                val name = parent.name
                for (directive in Alpine.allDirectives()) {
                    if ("x-data" == name) {
                        registrar.startInjecting(JavascriptLanguage.INSTANCE)
                            .addPlace("let __data = ", ";", host as PsiLanguageInjectionHost, range)
                            .doneInjecting()
                        return
                    } else if (directive == name) {
                        var directivePrefix = prefix

                        if (directive.startsWith('@') || directive.startsWith("x-on:")) {
                            directivePrefix = eventPrefix
                        }

                        registrar.startInjecting(JavascriptLanguage.INSTANCE)
                            .addPlace(directivePrefix, ";", host as PsiLanguageInjectionHost, range)
                            .doneInjecting()
                        return
                    }
                }
            }
        }

//        val text = ElementManipulators.getValueText(host)
//        var start = text.indexOf("{{")
//        while (start >= 0) {
//            var end = text.indexOf("}}", start)
//
//            end = if (end >= 0) end else range.length
//
//            var injectionCandidate = host.findElementAt(start)
//            while (injectionCandidate is PsiWhiteSpace) {
//                injectionCandidate = injectionCandidate.getNextSibling()
//            }
//
//            if (
//                injectionCandidate != null
//                && injectionCandidate.startOffsetInParent <= end
//                && !XmlTokenType.COMMENTS.contains(injectionCandidate.node.elementType)
//                && injectionCandidate.node.elementType !== XmlElementType.XML_COMMENT
//                && injectionCandidate !is OuterLanguageElement
//            ) {
//
//                registrar.startInjecting(PhpLanguage.INSTANCE)
//                    .addPlace(null, null, host as PsiLanguageInjectionHost,
//                                TextRange(range.startOffset + start + 2, range.startOffset + end)
//                    )
//                    .doneInjecting()
//            }
//            start = text.indexOf("{{", end)
//        }

//        val text = ElementManipulators.getValueText(host)
//        var start = text.indexOf("\${")
//        while (start >= 0) {
//            var end = text.indexOf("}", start)
//            end = if (end >= 0) end else range.length
//            var injectionCandidate = host.findElementAt(start)
//            while (injectionCandidate is PsiWhiteSpace) injectionCandidate = injectionCandidate.getNextSibling()
//
//            if (injectionCandidate != null &&
//                    injectionCandidate.startOffsetInParent <= end &&
//                    !XmlTokenType.COMMENTS.contains(injectionCandidate.node.elementType) &&
//                    injectionCandidate.node.elementType !== XmlElementType.XML_COMMENT &&
//                    injectionCandidate !is OuterLanguageElement) {
//
//                registrar.startInjecting(JavascriptLanguage.INSTANCE)
//                        .addPlace(null, null, host as PsiLanguageInjectionHost,
//                                TextRange(range.startOffset + start + 2, range.startOffset + end))
//                        .doneInjecting()
//            }
//            start = text.indexOf("\${", end)
//        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return Arrays.asList(XmlTextImpl::class.java, XmlAttributeValueImpl::class.java)
    }
}
