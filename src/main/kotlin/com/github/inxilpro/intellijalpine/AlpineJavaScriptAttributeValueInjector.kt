package com.github.inxilpro.intellijalpine

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import org.apache.commons.lang3.tuple.MutablePair
import org.apache.html.dom.HTMLDocumentImpl
import java.util.*

class AlpineJavaScriptAttributeValueInjector : MultiHostInjector {
    private companion object {
        val globalState =
            """
                /** @type {Object.<string, HTMLElement>} */
                let ${'$'}refs;
                
                /** @type {Object.<string, *>} */
                let ${'$'}store;
                
            """.trimIndent()

        val alpineWizardState =
            """
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
                /** @type {elType} */
                let ${'$'}el;
                
                /** @type {rootType} */
                let ${'$'}root;

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
                
                /**
                 * @param {string} scope
                 * @return string
                 */
                function ${'$'}id(scope) {}
                
            """.trimIndent()

        val eventMagics = "/** @type {eventType} */\nlet ${'$'}event;\n\n"
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, host: PsiElement) {
        if (host !is XmlAttributeValue) {
            return
        }
        if (!AttributeUtil.isValidInjectionTarget(host)) {
            return
        }

        val attribute = host.parent as? XmlAttribute ?: return
        val attributeName = attribute.name

        val content = host.text
        val ranges = getJavaScriptRanges(host, content)

        var (prefix, suffix) = getPrefixAndSuffix(attributeName, host)

        val jsLanguage = Language.findLanguageByID("JavaScript") 
            ?: throw IllegalStateException("JavaScript language not found")
        registrar.startInjecting(jsLanguage)

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

        if (!LanguageUtil.hasPhpLanguage(host.containingFile)) {
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

    private fun getPrefixAndSuffix(directive: String, host: XmlAttributeValue): Pair<String, String> {
        val globalContext = MutablePair(globalMagics, "");
        val context = AlpinePluginRegistry.getInstance().injectAllJsContext(host.project, globalContext)

        if ("x-data" != directive) {
            context.left = addTypingToCoreMagics(host) + context.left
        }

        if ("x-spread" == directive) {
            context.right += "()"
        }

        if (AttributeUtil.isEvent(directive)) {
            context.left += addTypingToEventMagics(directive, host)
        } else if ("x-for" == directive) {
            context.left += "for (let "
            context.right += ") {}"
        } else if ("x-ref" == directive) {
            context.left += "\$refs."
            context.right += "= \$el"
        } else if ("x-teleport" == directive) {
            context.left += "{ /** @var {HTMLElement} teleport */let teleport = "
            context.right += " }"
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
        val dataParent: HtmlTag?

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
                context.left = "$globalState\n$alpineWizardState\nlet ${'$'}data = $data;\nwith (${'$'}data) {\n\n$prefix"
                context.right = "$suffix\n\n}"
            }
        }
    }

    private fun addTypingToCoreMagics(host: XmlAttributeValue): String {
        var typedCoreMagics = coreMagics
        val attribute = host.parent as XmlAttribute
        val tag = attribute.parent

        fun jsElementNameFromXmlTag(tag: XmlTag): String {
            return try {
                HTMLDocumentImpl().createElement(tag.localName).javaClass.simpleName.removeSuffix("Impl")
            } catch (e: Exception) {
                "HTMLElement"
            }
        }

        // Determine type for $el
        run {
            val elType = jsElementNameFromXmlTag(tag)
            typedCoreMagics = typedCoreMagics.replace("{elType}", elType)
        }

        // Determine type for $root
        run {
            val elType = if (tag.getAttribute("x-data") != null) {
                jsElementNameFromXmlTag(tag)
            } else {
                PsiTreeUtil.findFirstParent(tag.parentTag)
                { it is HtmlTag && it.getAttribute("x-data") != null }
                    ?.let { jsElementNameFromXmlTag(it as XmlTag) }
                    ?: "HTMLElement"
            }
            typedCoreMagics = typedCoreMagics.replace("{rootType}", elType)
        }

        return typedCoreMagics
    }

    private fun addTypingToEventMagics(directive: String, host: XmlAttributeValue): String {
        val eventName = AttributeUtil.getEventNameFromDirective(directive)
        return eventMagics.replace("eventType", eventName)
    }
}
