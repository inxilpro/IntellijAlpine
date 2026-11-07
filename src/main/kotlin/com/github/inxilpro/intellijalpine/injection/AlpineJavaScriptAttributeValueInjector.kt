package com.github.inxilpro.intellijalpine.injection

import com.github.inxilpro.intellijalpine.attributes.AttributeUtil
import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.github.inxilpro.intellijalpine.support.LanguageUtil
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
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

class AlpineJavaScriptAttributeValueInjector : MultiHostInjector {
    companion object {
        // {{ ... }}, <?php ... ?> / <?= ... ?>, and @directive(...) Blade/PHP constructs
        internal val phpMatcher = Regex("(?<!@)\\{\\{.+?}}|<\\?(?:=|php).+?\\?>|@[a-zA-Z]+\\(.*\\)(?:\\.defer)?")

        // Must parse as a JS expression in value position
        internal const val SERVER_VALUE_PLACEHOLDER = "__SERVER_VALUE()"

        // Alpine's "(item, index) in items" destructuring form
        internal val forParensMatcher =
            Regex("\\(\\s*[A-Za-z_$][\\w$]*(?:\\s*,\\s*[A-Za-z_$][\\w$]*)*\\s*\\)(?=\\s+(?:in|of)\\s)")

        internal val forVarsMatcher =
            Regex("^\\s*\\(?\\s*([A-Za-z_$][\\w$]*)(?:\\s*,\\s*([A-Za-z_$][\\w$]*))?(?:\\s*,\\s*([A-Za-z_$][\\w$]*))?\\s*\\)?\\s+(?:in|of)\\s")
    }

    /** A host-relative region of server-side template code and the JavaScript that stands in for it. */
    private data class Mask(val range: IntRange, val replacement: String)

    private val globalState =
        """
            /** @type {Object.<string, HTMLElement>} */
            let ${'$'}refs;
            
            /** @type {Object.<string, *>} */
            let ${'$'}store;
            
        """.trimIndent()

    private val globalMagics =
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

    private val coreMagics =
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

    private val eventMagics = "/** @type {eventType} */\nlet ${'$'}event;\n\n"

    private val resizeMagics =
        """
            /** @type number */
            let ${'$'}width;

            /** @type number */
            let ${'$'}height;

        """.trimIndent()

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

        // Fragments concatenate, so each masked server-side region between two
        // fragments is replaced by a placeholder in the following fragment's prefix
        var shreds = getJavaScriptShreds(host, content)

        if ("x-for" == attributeName) {
            shreds = rewriteForDestructuringParens(shreds, content)
        }

        // Empty fragments are dropped, carrying their gap prefix into the next fragment
        var carry = ""
        val places = mutableListOf<Pair<TextRange, String>>()
        for ((range, gapPrefix) in shreds) {
            if (range.isEmpty) {
                carry += gapPrefix
            } else {
                places.add(range to carry + gapPrefix)
                carry = ""
            }
        }

        // An empty value (x-show="") or one that is entirely template code (x-data="@json($config)")
        // still gets a single zero-length place, so completion works there and the stand-in parses
        if (places.isEmpty()) {
            places.add(TextRange.from(shreds.last().first.startOffset, 0) to carry)
            carry = ""
        }

        val (prefix, suffix) = getPrefixAndSuffix(attributeName, host)

        val jsLanguage = Language.findLanguageByID("JavaScript")
            ?: throw IllegalStateException("JavaScript language not found")

        registrar.startInjecting(jsLanguage)

        places.forEachIndexed { index, (range, gapPrefix) ->
            val placePrefix = (if (index == 0) prefix else "") + gapPrefix
            val placeSuffix = if (index == places.lastIndex) carry + suffix else ""
            registrar.addPlace(placePrefix, placeSuffix, host as PsiLanguageInjectionHost, range)
        }

        registrar.doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(XmlAttributeValue::class.java)
    }

    /**
     * Alpine's "(item, index) in items" is not valid JS inside "for (let ...)";
     * drop the paren characters from the injected ranges and substitute array
     * destructuring brackets so the parser sees "for (let [item, index] in items)".
     */
    private fun rewriteForDestructuringParens(
        shreds: List<Pair<TextRange, String>>,
        content: String,
    ): List<Pair<TextRange, String>> {
        val match = forParensMatcher.find(content) ?: return shreds
        var result = dropCharAt(shreds, match.range.first, "[")
        result = dropCharAt(result, match.range.last, "]")
        return result
    }

    private fun dropCharAt(
        shreds: List<Pair<TextRange, String>>,
        offset: Int,
        replacement: String,
    ): List<Pair<TextRange, String>> {
        return shreds.flatMap { (range, gapPrefix) ->
            if (offset >= range.startOffset && offset < range.endOffset) {
                listOf(
                    TextRange(range.startOffset, offset) to gapPrefix,
                    TextRange(offset + 1, range.endOffset) to replacement,
                )
            } else {
                listOf(range to gapPrefix)
            }
        }
    }


    /**
     * Splits the attribute value into JS fragments around masked server-side
     * regions; each fragment carries the gap prefix standing in for the masked
     * region before it.
     */
    private fun getJavaScriptShreds(host: XmlAttributeValue, content: String): List<Pair<TextRange, String>> {
        val valueRange = ElementManipulators.getValueTextRange(host)
        val masks = getMasks(host, content, valueRange)

        if (masks.isEmpty()) {
            return listOf(valueRange to "")
        }

        val shreds = mutableListOf<Pair<TextRange, String>>()
        var offset = valueRange.startOffset
        var gapPrefix = ""
        for (mask in masks) {
            if (mask.range.first > offset) {
                shreds.add(TextRange(offset, mask.range.first) to gapPrefix)
                gapPrefix = ""
            }
            gapPrefix += mask.replacement
            offset = maxOf(offset, mask.range.last + 1)
        }
        shreds.add(TextRange(offset, maxOf(offset, valueRange.endOffset)) to gapPrefix)

        return shreds
    }

    /** The attribute's value text with server-side template code replaced by its stand-in. */
    private fun sanitizeServerSide(value: XmlAttributeValue): String {
        val content = value.text
        val valueRange = ElementManipulators.getValueTextRange(value)

        val result = StringBuilder()
        var offset = valueRange.startOffset
        for (mask in getMasks(value, content, valueRange)) {
            if (mask.range.first > offset) {
                result.append(content, offset, mask.range.first)
            }
            result.append(mask.replacement)
            offset = maxOf(offset, mask.range.last + 1)
        }
        if (valueRange.endOffset > offset) {
            result.append(content, offset, valueRange.endOffset)
        }
        return result.toString()
    }

    /**
     * Host-relative regions of server-side template code within the attribute value, each
     * standing in for a value in the injected JavaScript.
     */
    private fun getMasks(host: XmlAttributeValue, content: String, valueRange: TextRange): List<Mask> {
        if (!LanguageUtil.hasPhpLanguage(host.containingFile)) {
            return emptyList()
        }

        return phpMatcher.findAll(content)
            .map { it.range }
            .filter { it.first >= valueRange.startOffset && it.last < valueRange.endOffset }
            .map { Mask(it, SERVER_VALUE_PLACEHOLDER) }
            .toList()
    }

    private fun getPrefixAndSuffix(directive: String, host: XmlAttributeValue): Pair<String, String> {
        val globalContext = MutablePair(globalMagics, "")
        val context = AlpinePluginRegistry.instance.injectAllJsContext(host.project, globalContext)

        if ("x-data" != directive) {
            context.left = addTypingToCoreMagics(host) + context.left
        }

        // Loop-variable declarations must precede any expression wrapper below
        addForScope(host, directive, context)

        if ("x-spread" == directive) {
            context.right += "()"
        }

        if (AttributeUtil.isEvent(directive)) {
            context.left += addTypingToEventMagics(directive, host)
        } else if ("x-for" == directive) {
            context.left += "for (let "
            val variableReads = getForVariables(host).joinToString("\n") { "void $it;" }
            context.right += ") {\n$variableReads\n}"
        } else if ("x-ref" == directive) {
            context.left += "\$refs[\""
            context.right += "\"] = \$el"
        } else if ("x-teleport" == directive) {
            context.left += "{ /** @var {HTMLElement} teleport */let teleport = "
            context.right += " }"
        } else if ("x-resize" == directive) {
            context.left += resizeMagics
        } else if ("x-init" == directive || "x-effect" == directive) {
            // x-init and x-effect run scripts (statements), so they skip the
            // expression-only directive wrapping
        } else {
            context.left += "__ALPINE_DIRECTIVE(\n"
            context.right += "\n)"
        }

        addWithData(host, directive, context)

        return context.toPair()
    }

    private fun getForVariables(host: XmlAttributeValue): List<String> {
        val value = (host.parent as? XmlAttribute)?.value ?: return emptyList()
        return forVarsMatcher.find(value)?.groupValues
            ?.drop(1)
            ?.filter(String::isNotEmpty)
            ?: emptyList()
    }

    /**
     * Declares x-for loop variables from enclosing (or same-tag) x-for directives
     * so they resolve in sibling and child directives. Nested loops may reuse a
     * name; each name is declared once.
     */
    private fun addForScope(host: XmlAttributeValue, directive: String, context: MutablePair<String, String>) {
        val hostTag = PsiTreeUtil.findFirstParent(host) { it is HtmlTag } as? HtmlTag ?: return
        val declared = linkedSetOf<String>()

        var tag: HtmlTag? = hostTag
        while (tag != null) {
            val forValue = tag.getAttribute("x-for")?.value
            if (forValue != null && !("x-for" == directive && tag == hostTag)) {
                forVarsMatcher.find(forValue)?.groupValues?.drop(1)?.filter { it.isNotEmpty() }?.let(declared::addAll)
            }
            tag = PsiTreeUtil.findFirstParent(tag, true) { it is HtmlTag } as? HtmlTag
        }

        if (declared.isNotEmpty()) {
            context.left += declared.joinToString("") { "let $it;\n" } + "\n"
        }
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
            val data = dataParent.getAttribute("x-data")?.valueElement
                ?.let { sanitizeServerSide(it) }
                ?.ifBlank { "{}" }
            if (null != data) {
                // Alpine auto-invokes x-data expressions that evaluate to a function;
                // the typeof guard lets the IDE infer the invoked component's shape
                val dataExpr = if (data.trimStart().startsWith("{")) {
                    data
                } else {
                    "(typeof ($data) === 'function' ? ($data)() : ($data))"
                }
                val (prefix, suffix) = context
                context.left = "$globalState\nlet ${'$'}data = $dataExpr;\nwith (${'$'}data) {\n\n$prefix"
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
