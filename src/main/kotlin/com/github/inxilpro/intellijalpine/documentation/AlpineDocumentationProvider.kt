package com.github.inxilpro.intellijalpine.documentation

import com.github.inxilpro.intellijalpine.attributes.AttributeUtil
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.xml.XmlTokenImpl
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTokenType

class AlpineDocumentationProvider : DocumentationProvider {

    private val directiveDocs = mapOf(
        "x-data" to DirectiveDoc(
            "x-data",
            "Declare a new Alpine component and its reactive data.",
            "<code>x-data=\"{ open: false }\"</code>",
            "Everything inside a tag with <code>x-data</code> becomes an Alpine component. " +
                    "Any data properties declared will be reactive — when they change, the DOM updates automatically."
        ),
        "x-init" to DirectiveDoc(
            "x-init",
            "Run an expression when a component is initialized.",
            "<code>x-init=\"date = new Date()\"</code>",
            "Runs the given expression once when the component is initialized. " +
                    "Can also be used to run async code using <code>await</code>."
        ),
        "x-show" to DirectiveDoc(
            "x-show",
            "Toggle the visibility of an element.",
            "<code>x-show=\"open\"</code>",
            "Sets <code>display: none</code> on the element when the expression evaluates to <code>false</code>. " +
                    "Works with <code>x-transition</code> for animated show/hide."
        ),
        "x-bind" to DirectiveDoc(
            "x-bind",
            "Dynamically set HTML attributes on an element.",
            "<code>x-bind:class=\"{ active: isActive }\"</code> or <code>:class=\"{ active: isActive }\"</code>",
            "Sets the value of an attribute to the result of a JavaScript expression. " +
                    "Shorthand: <code>:attribute</code>."
        ),
        "x-on" to DirectiveDoc(
            "x-on",
            "Listen for browser events on an element.",
            "<code>x-on:click=\"open = !open\"</code> or <code>@click=\"open = !open\"</code>",
            "Attaches an event listener to the element. Shorthand: <code>@event</code>. " +
                    "Supports modifiers like <code>.prevent</code>, <code>.stop</code>, <code>.window</code>, etc."
        ),
        "x-text" to DirectiveDoc(
            "x-text",
            "Set the text content of an element.",
            "<code>x-text=\"message\"</code>",
            "Sets the inner text of the element to the result of the expression. " +
                    "HTML is escaped automatically."
        ),
        "x-html" to DirectiveDoc(
            "x-html",
            "Set the inner HTML of an element.",
            "<code>x-html=\"content\"</code>",
            "Sets the inner HTML of the element. <strong>Warning:</strong> only use with trusted content " +
                    "to avoid XSS vulnerabilities."
        ),
        "x-model" to DirectiveDoc(
            "x-model",
            "Two-way bind a form input to Alpine data.",
            "<code>x-model=\"search\"</code>",
            "Keeps the value of an input element in sync with a data property. " +
                    "Supports modifiers: <code>.lazy</code>, <code>.number</code>, <code>.debounce</code>, <code>.throttle</code>."
        ),
        "x-modelable" to DirectiveDoc(
            "x-modelable",
            "Expose a property for external binding via x-model.",
            "<code>x-modelable=\"value\"</code>",
            "Allows a component property to be bound from outside using <code>x-model</code>."
        ),
        "x-for" to DirectiveDoc(
            "x-for",
            "Repeat a block of HTML based on a data set.",
            "<code>&lt;template x-for=\"item in items\"&gt;</code>",
            "Must be used on a <code>&lt;template&gt;</code> tag. Iterates over arrays or ranges."
        ),
        "x-transition" to DirectiveDoc(
            "x-transition",
            "Apply CSS transitions to show/hide elements.",
            "<code>x-transition</code> or <code>x-transition:enter=\"transition ease-out\"</code>",
            "Adds transition classes during enter/leave phases. Can specify classes for each phase: " +
                    "<code>:enter</code>, <code>:enter-start</code>, <code>:enter-end</code>, " +
                    "<code>:leave</code>, <code>:leave-start</code>, <code>:leave-end</code>."
        ),
        "x-effect" to DirectiveDoc(
            "x-effect",
            "Run a reactive side effect.",
            "<code>x-effect=\"console.log(count)\"</code>",
            "Re-runs the expression whenever any reactive data it references changes."
        ),
        "x-ref" to DirectiveDoc(
            "x-ref",
            "Reference an element directly via <code>\$refs</code>.",
            "<code>x-ref=\"input\"</code> then <code>\$refs.input.focus()</code>",
            "Creates a named reference to the DOM element accessible via <code>\$refs</code>."
        ),
        "x-if" to DirectiveDoc(
            "x-if",
            "Conditionally add/remove a block of HTML.",
            "<code>&lt;template x-if=\"open\"&gt;</code>",
            "Must be used on a <code>&lt;template&gt;</code> tag. Unlike <code>x-show</code>, " +
                    "this completely adds/removes the element from the DOM."
        ),
        "x-id" to DirectiveDoc(
            "x-id",
            "Scope generated IDs with <code>\$id()</code>.",
            "<code>x-id=\"['text-input']\"</code>",
            "Provides a scoping context for IDs generated via the <code>\$id()</code> magic."
        ),
        "x-cloak" to DirectiveDoc(
            "x-cloak",
            "Hide the element until Alpine initializes.",
            "<code>x-cloak</code>",
            "Hides the element until Alpine finishes initializing, preventing flash of unstyled content. " +
                    "Requires a CSS rule: <code>[x-cloak] { display: none !important; }</code>."
        ),
        "x-ignore" to DirectiveDoc(
            "x-ignore",
            "Prevent Alpine from initializing a section of HTML.",
            "<code>x-ignore</code>",
            "Tells Alpine to skip this element and all its children."
        ),
        "x-teleport" to DirectiveDoc(
            "x-teleport",
            "Move an element to another location in the DOM.",
            "<code>&lt;template x-teleport=\"body\"&gt;</code>",
            "Must be used on a <code>&lt;template&gt;</code> tag. Moves the template content " +
                    "to the target selector."
        ),
        "x-mask" to DirectiveDoc(
            "x-mask",
            "Apply an input mask to an element.",
            "<code>x-mask=\"(999) 999-9999\"</code>",
            "Constrains user input to match the specified pattern. <code>9</code> for digits, " +
                    "<code>a</code> for letters, <code>*</code> for both."
        ),
        "x-intersect" to DirectiveDoc(
            "x-intersect",
            "Trigger when element enters or leaves the viewport.",
            "<code>x-intersect=\"shown = true\"</code>",
            "Uses the Intersection Observer API to detect when an element is visible."
        ),
        "x-trap" to DirectiveDoc(
            "x-trap",
            "Trap keyboard focus within an element.",
            "<code>x-trap=\"open\"</code>",
            "When the expression is truthy, traps focus within the element (useful for modals/dialogs)."
        ),
        "x-collapse" to DirectiveDoc(
            "x-collapse",
            "Animate show/hide with a height transition.",
            "<code>x-collapse</code>",
            "Smoothly animates the element's height when toggling visibility with <code>x-show</code>."
        ),
        "x-spread" to DirectiveDoc(
            "x-spread",
            "<strong>Deprecated.</strong> Use <code>x-bind</code> with an object instead.",
            "<code>x-spread=\"directives\"</code>",
            "This directive is deprecated in Alpine v3. Use object syntax with <code>x-bind</code> instead."
        ),
    )

    private val magicDocs = mapOf(
        "\$refs" to "Access DOM elements marked with <code>x-ref</code>.",
        "\$store" to "Access global Alpine stores registered with <code>Alpine.store()</code>.",
        "\$el" to "Reference to the current DOM element.",
        "\$root" to "Reference to the root element of the current Alpine component.",
        "\$dispatch" to "Dispatch a custom browser event from the current element.",
        "\$nextTick" to "Execute a callback after Alpine has finished updating the DOM.",
        "\$watch" to "Watch a reactive property and run a callback when it changes.",
        "\$id" to "Generate a scoped unique ID (requires <code>x-id</code> on an ancestor).",
        "\$persist" to "Persist a data property in localStorage.",
        "\$queryString" to "Bind a data property to the URL query string.",
        "\$data" to "Access the current component's reactive data object.",
        "\$event" to "Access the native browser event inside an event handler.",
    )

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val attributeName = resolveAttributeName(element, originalElement) ?: return null
        return generateDirectiveDoc(attributeName)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val attributeName = resolveAttributeName(element, originalElement) ?: return null
        val baseName = attributeName.substringBefore('.')
        val doc = directiveDocs[baseName] ?: return null
        return doc.summary
    }

    private fun resolveAttributeName(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = element ?: originalElement ?: return null

        if (target is XmlTokenImpl && target.tokenType == XmlTokenType.XML_NAME) {
            val attr = target.parent as? XmlAttribute ?: return null
            val name = attr.name
            if (name.startsWith("x-") || name.startsWith("@") || name.startsWith(":")) {
                return name
            }
        }

        if (target is XmlAttribute) {
            val name = target.name
            if (name.startsWith("x-") || name.startsWith("@") || name.startsWith(":")) {
                return name
            }
        }

        return null
    }

    private fun generateDirectiveDoc(attributeName: String): String? {
        val baseName = when {
            attributeName.startsWith("@") -> "x-on"
            attributeName.startsWith(":") -> "x-bind"
            else -> attributeName.substringBefore('.').substringBefore(':').let { base ->
                if (directiveDocs.containsKey(attributeName.substringBefore('.'))) {
                    attributeName.substringBefore('.')
                } else {
                    base
                }
            }
        }

        val doc = directiveDocs[baseName] ?: return null

        return buildString {
            append("<div class='definition'><pre>")
            append(doc.name)
            append("</pre></div>")
            append("<div class='content'>")
            append("<p>${doc.summary}</p>")
            append("<p><b>Example:</b> ${doc.example}</p>")
            append("<p>${doc.details}</p>")
            append("</div>")
        }
    }

    private data class DirectiveDoc(
        val name: String,
        val summary: String,
        val example: String,
        val details: String,
    )
}
