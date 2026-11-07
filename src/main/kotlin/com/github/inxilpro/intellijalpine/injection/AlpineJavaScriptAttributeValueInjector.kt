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
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import org.apache.commons.lang3.tuple.MutablePair
import org.apache.html.dom.HTMLDocumentImpl

/** Finds server-side template tokens in a piece of text. */
internal typealias Tokenizer = (String) -> List<IntRange>

class AlpineJavaScriptAttributeValueInjector : MultiHostInjector {
    companion object {
        // {{ ... }}, <?php ... ?> / <?= ... ?>, and @directive(...) Blade/PHP constructs
        internal val phpMatcher = Regex("(?<!@)\\{\\{.+?}}|<\\?(?:=|php).+?\\?>|@[a-zA-Z]+\\(.*\\)(?:\\.defer)?")

        // FreeMarker authors use ${'$'} to emit the dollar sign that starts a
        // JavaScript template-literal interpolation, e.g. `${'$'}{item.name}`.
        internal val freeMarkerDollarLiteralMatcher =
            Regex("\\${'$'}\\{\\s*['\"]\\${'$'}['\"]\\s*}")

        // Opens/closes of FTL branching blocks (#if, #list) and their #else/#elseif alternatives;
        // applied to single tags found by FreeMarkerScanner
        private val freeMarkerBranchTag = Regex("^[\\[<](/?)#(if|list|else|elseif)\\b")

        // Must parse as a JS expression in value position
        internal const val SERVER_VALUE_PLACEHOLDER = "__SERVER_VALUE()"

        // Alpine's "(item, index) in items" destructuring form
        internal val forParensMatcher =
            Regex("\\(\\s*[A-Za-z_$][\\w$]*(?:\\s*,\\s*[A-Za-z_$][\\w$]*)*\\s*\\)(?=\\s+(?:in|of)\\s)")

        internal val forVarsMatcher =
            Regex("^\\s*\\(?\\s*([A-Za-z_$][\\w$]*)(?:\\s*,\\s*([A-Za-z_$][\\w$]*))?(?:\\s*,\\s*([A-Za-z_$][\\w$]*))?\\s*\\)?\\s+(?:in|of)\\s")

        /**
         * What a masked server-side token stands for in the injected JavaScript. Interpolations and
         * self-closing macro calls produce a value at runtime and need an expression placeholder;
         * control-flow template tags (e.g. [#if], </#list>, {% if %}) contribute no value and must
         * vanish entirely, or they would add bogus members to surrounding object literals.
         */
        internal fun placeholderForToken(token: String): String {
            val trimmed = token.trim()
            return when {
                freeMarkerDollarLiteralMatcher.matches(trimmed) -> "\$"
                // @ covers Blade directives, which keep their historical placeholder
                // since value-producing ones like @json() are common in x-data
                trimmed.startsWith("\${") || trimmed.startsWith("{{") || trimmed.startsWith("<?") ||
                    trimmed.startsWith("<%=") || trimmed.startsWith("@") ->
                    SERVER_VALUE_PLACEHOLDER
                isSelfClosingUserDirective(trimmed) -> SERVER_VALUE_PLACEHOLDER
                else -> ""
            }
        }

        // Self-closing user-directive calls, e.g. [@json value=columns /], which templates use to emit
        // a JavaScript value where a macro is more readable than an interpolation
        private fun isSelfClosingUserDirective(token: String): Boolean =
            (token.startsWith("[@") && token.endsWith("/]")) || (token.startsWith("<@") && token.endsWith("/>"))

        /**
         * Replacement for a masked region, which may hold several adjacent template tokens (a template
         * plugin can report `[#if a]${x}` as one element). Each token is classified on its own so an
         * interpolation keeps its placeholder next to a vanishing control-flow tag.
         */
        internal fun replacementFor(maskedText: String, tokenizer: Tokenizer?): String {
            val tokens = tokenizer?.invoke(maskedText).orEmpty()
            if (tokens.isEmpty()) {
                return placeholderForToken(maskedText)
            }
            return tokens.joinToString("") { placeholderForToken(maskedText.substring(it)) }
        }

        /**
         * With only the tags masked, every arm of [#if]a[#else]b[/#if] would survive
         * as adjacent JS fragments ("a b"), which cannot parse. Only the first arm can
         * stand in for the block, so everything from the first alternative tag through
         * the matching close tag is masked along with it.
         */
        internal fun alternativeBranchIntervals(content: String): List<IntRange> {
            val intervals = mutableListOf<IntRange>()
            var branchStart = -1
            var depth = 0
            for (token in FreeMarkerScanner.tokens(content)) {
                val groups = freeMarkerBranchTag.find(content.substring(token))?.groupValues ?: continue
                val isClose = groups[1] == "/"
                val name = groups[2]
                when {
                    name == "else" || name == "elseif" -> {
                        if (branchStart < 0) {
                            branchStart = token.first
                            depth = 0
                        }
                    }
                    isClose -> {
                        if (branchStart >= 0) {
                            if (depth == 0) {
                                intervals.add(branchStart..token.last)
                                branchStart = -1
                            } else {
                                depth--
                            }
                        }
                    }
                    else -> {
                        if (branchStart >= 0) {
                            depth++
                        }
                    }
                }
            }
            return intervals
        }
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
        // fragments is replaced by its stand-in in the following fragment's prefix
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

    private fun getServerSideTokenizer(file: PsiFile): Tokenizer? = when {
        LanguageUtil.hasPhpLanguage(file) -> { text -> phpMatcher.findAll(text).map { it.range }.toList() }
        LanguageUtil.hasFreeMarkerLanguage(file) -> FreeMarkerScanner::tokens
        else -> null
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
     * Host-relative regions of server-side template code within the attribute value,
     * combining template fragments the FreeMarker plugin lexed (OuterLanguageElements) with
     * scanned tokens for setups where no template plugin is active. Overlapping regions
     * (an outer element and a regex match often coincide) are merged; touching ones are kept
     * apart so each keeps its own stand-in.
     */
    private fun getMasks(host: XmlAttributeValue, content: String, valueRange: TextRange): List<Mask> {
        val file = host.containingFile
        val tokenizer = getServerSideTokenizer(file)
        val isFreeMarker = LanguageUtil.hasFreeMarkerLanguage(file)
        val regions = mutableListOf<IntRange>()

        // Template code the FreeMarker plugin lexed; other template languages keep their existing
        // behavior, since their outer elements (such as Velocity references) can be valid JavaScript
        if (isFreeMarker) {
            val hostStart = host.textRange.startOffset
            PsiTreeUtil.findChildrenOfType(host, OuterLanguageElement::class.java).forEach {
                val start = it.textRange.startOffset - hostStart
                regions.add(start until start + it.textLength)
            }
        }

        tokenizer?.invoke(content)?.let(regions::addAll)

        val branches = if (isFreeMarker) alternativeBranchIntervals(content) else emptyList()
        regions.addAll(branches)

        val inValue = regions
            .map { maxOf(it.first, valueRange.startOffset)..minOf(it.last, valueRange.endOffset - 1) }
            .filter { !it.isEmpty() }
            .sortedBy { it.first }

        val merged = mutableListOf<IntRange>()
        for (region in inValue) {
            val last = merged.lastOrNull()
            if (last != null && region.first <= last.last) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, region.last)
            } else {
                merged.add(region)
            }
        }

        return merged.map { Mask(it, standInFor(content, it, branches, tokenizer)) }
    }

    /** Skipped #else arms contribute nothing; everything else in the region is classified token by token. */
    private fun standInFor(content: String, region: IntRange, branches: List<IntRange>, tokenizer: Tokenizer?): String {
        val result = StringBuilder()
        var position = region.first
        for (branch in branches.filter { it.first >= region.first && it.last <= region.last }.sortedBy { it.first }) {
            if (branch.first > position) {
                result.append(replacementFor(content.substring(position, branch.first), tokenizer))
            }
            position = maxOf(position, branch.last + 1)
        }
        if (position <= region.last) {
            result.append(replacementFor(content.substring(position, region.last + 1), tokenizer))
        }
        return result.toString()
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
