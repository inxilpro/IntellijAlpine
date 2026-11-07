package com.github.inxilpro.intellijalpine.attributes

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DirectiveAttributesTest : BasePlatformTestCase() {

    fun testIntersectEnterAndLeaveAreRecognizedAndInjectJavaScript() {
        for (attribute in listOf("x-intersect:enter", "x-intersect:leave")) {
            assertAttributeRecognized(attribute)

            val injected = injectedJs("""<div x-data="{ shown: false }" $attribute="shown = true"></div>""", attribute)
            assertEquals("JS should be injected into $attribute", 1, injected.size)
            val errors = PsiTreeUtil.findChildrenOfType(injected.single(), PsiErrorElement::class.java)
            assertTrue("JS errors in $attribute:\n${injected.single().text}", errors.isEmpty())
        }
    }

    fun testTailwindUiStateAttributesAreRecognizedButNotInjected() {
        for (attribute in listOf("x-description", "x-state:on", "x-state:off")) {
            assertAttributeRecognized(attribute)
            assertTrue(
                "$attribute holds prose, not JavaScript",
                injectedJs("""<div $attribute="Open"></div>""", attribute).isEmpty(),
            )
        }
    }

    fun testNewAttributesAreOfferedInCompletion() {
        myFixture.configureByText("test.html", """<div x-data x-<caret>></div>""")
        myFixture.completeBasic()
        val offered = myFixture.lookupElementStrings.orEmpty()

        for (attribute in listOf("x-intersect:enter", "x-intersect:leave", "x-description", "x-state:on", "x-state:off")) {
            assertTrue("$attribute should be offered, got $offered", attribute in offered)
        }
        assertFalse("A bare x-state is not an attribute", "x-state" in offered)
    }

    private fun assertAttributeRecognized(attributeName: String) {
        val file = myFixture.configureByText("test.html", """<div $attributeName="value"></div>""")
        val tag = PsiTreeUtil.findChildOfType(file, XmlTag::class.java)!!
        assertNotNull(
            "$attributeName should be recognized as an Alpine attribute",
            AttributesProvider().getAttributeDescriptor(attributeName, tag),
        )
    }

    private fun injectedJs(html: String, attributeName: String): List<com.intellij.psi.PsiFile> {
        val file = myFixture.configureByText("test.html", html)
        val host = PsiTreeUtil.findChildrenOfType(file, XmlAttributeValue::class.java)
            .first { (it.parent as? XmlAttribute)?.name == attributeName }
        val results = mutableListOf<com.intellij.psi.PsiFile>()
        InjectedLanguageManager.getInstance(project).enumerate(host) { injected, _ -> results.add(injected) }
        return results
    }
}
