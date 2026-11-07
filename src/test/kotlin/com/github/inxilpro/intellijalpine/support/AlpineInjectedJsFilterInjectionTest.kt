package com.github.inxilpro.intellijalpine.support

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AlpineInjectedJsFilterInjectionTest : BasePlatformTestCase() {
    private val filter = AlpineInjectedJsHighlightInfoFilter()

    fun testSuppressesNotExportedOnBareReferenceInsideAlpineAttribute() {
        val injected = injectedAlpineJs("""<div x-data="app.components.select()"><div x-text="label()"></div></div>""", "x-text")

        assertFalse(
            "Not-exported warnings on bare Alpine references must be filtered",
            filter.accept(warningAt(injected, "label()", NOT_EXPORTED), injected),
        )
    }

    fun testKeepsNotExportedOnQualifiedReference() {
        val injected = injectedAlpineJs("""<div x-data="{}"><div x-text="helpers.label()"></div></div>""", "x-text")

        assertTrue(
            "Qualified references are not component state, so their warnings stay",
            filter.accept(warningAt(injected, "label()", NOT_EXPORTED), injected),
        )
    }

    fun testKeepsNotExportedInPlainTypeScriptFile() {
        val file = myFixture.configureByText("component.ts", "const text = label();")

        assertTrue(
            "Not-exported warnings outside Alpine attributes must stay visible",
            filter.accept(warningAt(file, "label()", NOT_EXPORTED), file),
        )
    }

    fun testSuppressesAssignmentToComponentProperty() {
        val injected = injectedAlpineJs("""<div x-data="{ open: false }"><button @click="open = true"></button></div>""", "@click")

        assertFalse(
            "Assigning an x-data property must not warn, even when the name matches a global like window.open",
            filter.accept(warningAt(injected, "open = true", ASSIGNMENT_MISMATCH), injected),
        )
    }

    fun testKeepsAssignmentWarningWhenNoComponentPropertyExists() {
        val injected = injectedAlpineJs("""<div x-data="{ count: 0 }"><button @click="open = true"></button></div>""", "@click")

        assertTrue(
            "Without a matching x-data property the assignment really targets the global",
            filter.accept(warningAt(injected, "open = true", ASSIGNMENT_MISMATCH), injected),
        )
    }

    fun testKeepsAssignmentWarningForLocalVariableShadowingAProperty() {
        val injected = injectedAlpineJs(
            """<div x-data="{ count: 0 }"><button @click="let count = 1; count = 'many'"></button></div>""",
            "@click",
        )

        assertTrue(
            "A local variable shadows the component property, so its assignment warning is real",
            filter.accept(warningAt(injected, "count = 'many'", ASSIGNMENT_MISMATCH), injected),
        )
    }

    private fun injectedAlpineJs(html: String, attributeName: String): PsiFile {
        val file = myFixture.configureByText("test.html", html)
        val manager = InjectedLanguageManager.getInstance(project)
        val host = PsiTreeUtil.findChildrenOfType(file, XmlAttributeValue::class.java)
            .first { (it.parent as? XmlAttribute)?.name == attributeName }

        var injected: PsiFile? = null
        manager.enumerate(host) { injectedPsi, _ -> injected = injectedPsi }
        return assertInstanceOf(injected, PsiFile::class.java)
    }

    private fun warningAt(file: PsiFile, snippet: String, description: String): HighlightInfo {
        val offset = file.text.lastIndexOf(snippet)
        assertTrue("'$snippet' not found in:\n${file.text}", offset >= 0)
        return HighlightInfo.newHighlightInfo(HighlightInfoType.WARNING)
            .range(offset, offset + 1)
            .description(description)
            .createUnconditionally()
    }

    private companion object {
        const val NOT_EXPORTED = "Element is not exported"
        const val ASSIGNMENT_MISMATCH =
            "Assigned expression type boolean is not assignable to type " +
                "(url?: string | URL, target?: string, features?: string) => WindowProxy | null"
    }
}
