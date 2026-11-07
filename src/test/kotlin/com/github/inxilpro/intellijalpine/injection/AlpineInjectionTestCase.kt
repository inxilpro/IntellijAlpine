package com.github.inxilpro.intellijalpine.injection

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSCommaExpression
import com.intellij.lang.javascript.psi.JSLabeledStatement
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Helpers for asserting on the JavaScript injected into Alpine attributes. */
abstract class AlpineInjectionTestCase : BasePlatformTestCase() {

    /** File name the markup is parsed as; its extension selects the template language. */
    protected open val fileName = "test.html"

    /** JavaScript injected into every [attributeName] attribute of [html], in document order. */
    protected fun injectedJs(html: String, attributeName: String, fileName: String = this.fileName): List<PsiFile> {
        val file = myFixture.configureByText(fileName, html)
        val hosts = file.viewProvider.allFiles
            .flatMap { root -> PsiTreeUtil.findChildrenOfType(root, XmlAttributeValue::class.java) }
            .filter { (it.parent as? XmlAttribute)?.name == attributeName }
            .distinct()
        assertTrue("No $attributeName attribute in:\n$html", hosts.isNotEmpty())

        val manager = InjectedLanguageManager.getInstance(project)
        val results = mutableListOf<PsiFile>()
        for (host in hosts) {
            manager.enumerate(host) { injected, _ -> results.add(injected) }
        }
        return results
    }

    /** The single JavaScript file injected into [attributeName], checked for parse errors. */
    protected fun cleanJs(html: String, attributeName: String, fileName: String = this.fileName): PsiFile {
        val injected = injectedJs(html, attributeName, fileName)
        assertEquals("Expected one JS injection into $attributeName", 1, injected.size)
        return injected.single().also(::assertNoJsErrors)
    }

    protected fun assertNoJsErrors(js: PsiFile) {
        val errors = PsiTreeUtil.findChildrenOfType(js, PsiErrorElement::class.java)
        assertTrue(
            "JS errors:\n" + errors.joinToString("\n") { "  ${it.errorDescription} at ${it.textOffset}" } +
                "\n--- injected text ---\n${js.text}\n---",
            errors.isEmpty(),
        )
    }

    /** Asserts that [keys] parse as properties of an object literal, not as labels or comma expressions. */
    protected fun assertObjectLiteralKeys(js: PsiFile, keys: Set<String>) {
        assertTrue(
            "Object literal keys should not parse as labels:\n${js.text}",
            PsiTreeUtil.findChildrenOfType(js, JSLabeledStatement::class.java).isEmpty(),
        )
        assertTrue(
            "Object literal content should not parse as comma expressions:\n${js.text}",
            PsiTreeUtil.findChildrenOfType(js, JSCommaExpression::class.java).isEmpty(),
        )
        val actual = PsiTreeUtil.findChildrenOfType(js, JSProperty::class.java).mapNotNull { it.name }.toSet()
        assertTrue("Expected properties $keys, found $actual:\n${js.text}", actual.containsAll(keys))
    }

    protected fun placeholderCount(js: PsiFile): Int =
        Regex(Regex.escape(AlpineJavaScriptAttributeValueInjector.SERVER_VALUE_PLACEHOLDER)).findAll(js.text).count()

    /** Server-side values are unknown to the IDE, so their placeholder must type as `any`. */
    protected fun assertPlaceholdersTypeAsAny(js: PsiFile) {
        val name = AlpineJavaScriptAttributeValueInjector.SERVER_VALUE_PLACEHOLDER.removeSuffix("()")
        val placeholders = PsiTreeUtil.findChildrenOfType(js, JSCallExpression::class.java)
            .filter { it.methodExpression?.text == name }
        assertTrue("No placeholders in:\n${js.text}", placeholders.isNotEmpty())
        for (placeholder in placeholders) {
            val type = JSResolveUtil.getExpressionJSType(placeholder)?.typeText
            assertTrue("Expected placeholder to type as any, got $type:\n${js.text}", type == "*" || type == "any")
        }
    }
}
