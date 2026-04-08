package com.github.inxilpro.intellijalpine.injection

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AlpineJavaScriptInjectionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/injection"

    private fun assertInjectedInAttribute(html: String, attributeName: String) {
        val psiFile = myFixture.configureByText("test.html", html)
        val injectedManager = InjectedLanguageManager.getInstance(project)

        val attributes = PsiTreeUtil.findChildrenOfType(psiFile, XmlAttribute::class.java)
        val attribute = attributes.find { it.name == attributeName }
        assertNotNull("Should find $attributeName attribute", attribute)

        val attrValue = attribute!!.valueElement
        assertNotNull("Attribute should have a value element", attrValue)

        val injectedPsi = injectedManager.getInjectedPsiFiles(attrValue!!)
        assertNotNull("JavaScript should be injected into $attributeName", injectedPsi)
        assertFalse("Should have injected content", injectedPsi!!.isEmpty())
    }

    fun testJavaScriptInjectedInXShow() {
        assertInjectedInAttribute(
            """<div x-data="{ visible: true }"><span x-show="visible"></span></div>""",
            "x-show"
        )
    }

    fun testJavaScriptInjectedInXText() {
        assertInjectedInAttribute(
            """<div x-data="{ msg: 'hi' }"><span x-text="msg"></span></div>""",
            "x-text"
        )
    }

    fun testJavaScriptInjectedInEventHandler() {
        assertInjectedInAttribute(
            """<div x-data="{ count: 0 }"><button @click="count++">Click</button></div>""",
            "@click"
        )
    }

    fun testJavaScriptInjectedInXData() {
        assertInjectedInAttribute(
            """<div x-data="{ count: 0 }"></div>""",
            "x-data"
        )
    }

    fun testXDataScopeWrapping() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div x-data="{ items: [] }"><span x-text="items.length"></span></div>"""
        )
        val injectedManager = InjectedLanguageManager.getInstance(project)

        val attributes = PsiTreeUtil.findChildrenOfType(psiFile, XmlAttribute::class.java)
        val xTextAttr = attributes.find { it.name == "x-text" }
        assertNotNull("Should find x-text attribute", xTextAttr)

        val attrValue = xTextAttr!!.valueElement
        assertNotNull("Attribute should have a value element", attrValue)

        val injectedPsi = injectedManager.getInjectedPsiFiles(attrValue!!)
        assertNotNull("JavaScript should be injected", injectedPsi)
        assertFalse("Should have injected content", injectedPsi!!.isEmpty())

        val injectedText = injectedPsi[0].first.text
        assertTrue(
            "Injected JS should contain x-data scope wrapping with \$data",
            injectedText.contains("\$data")
        )
    }
}
