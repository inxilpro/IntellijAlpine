package com.github.inxilpro.intellijalpine.documentation

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AlpineDocumentationProviderTest : BasePlatformTestCase() {
    private val provider = AlpineDocumentationProvider()

    fun testDocumentationForXData() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div x-da<caret>ta="{ open: false }"></div>"""
        )
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val doc = provider.generateDoc(element, element)
        assertNotNull("Should generate documentation for x-data", doc)
        assertTrue("Doc should mention component scope", doc!!.contains("component"))
    }

    fun testDocumentationForXShow() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><span x-sh<caret>ow="open"></span></div>"""
        )
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val doc = provider.generateDoc(element, element)
        assertNotNull("Should generate documentation for x-show", doc)
        assertTrue("Doc should mention visibility", doc!!.contains("visibility"))
    }

    fun testDocumentationForEventShorthand() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><button @cl<caret>ick="handler">Click</button></div>"""
        )
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val doc = provider.generateDoc(element, element)
        assertNotNull("Should generate documentation for @click (maps to x-on)", doc)
        assertTrue("Doc should describe event listener", doc!!.contains("event"))
    }

    fun testDocumentationForBindShorthand() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><div :cl<caret>ass="{ active: true }"></div></div>"""
        )
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val doc = provider.generateDoc(element, element)
        assertNotNull("Should generate documentation for :class (maps to x-bind)", doc)
        assertTrue("Doc should describe attribute binding", doc!!.contains("attribute"))
    }

    fun testNoDocumentationForNonAlpineAttribute() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div cl<caret>ass="test"></div>"""
        )
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val doc = provider.generateDoc(element, element)
        assertNull("Should not generate documentation for non-Alpine attribute", doc)
    }

    fun testQuickNavigateInfoForXModel() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><input x-mo<caret>del="name"></div>"""
        )
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val info = provider.getQuickNavigateInfo(element, element)
        assertNotNull("Should return quick navigate info for x-model", info)
        assertTrue("Info should mention two-way binding", info!!.contains("Two-way"))
    }

    fun testDocumentationForDeprecatedXSpread() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><div x-spr<caret>ead="directives"></div></div>"""
        )
        val element = psiFile.findElementAt(myFixture.caretOffset)
        val doc = provider.generateDoc(element, element)
        assertNotNull("Should generate documentation for x-spread", doc)
        assertTrue("Doc should mention deprecated", doc!!.contains("Deprecated") || doc.contains("deprecated"))
    }

    fun testDocumentationForXTransition() {
        val psiFile = myFixture.configureByText(
            "test.html",
            """<div x-data="{}" x-show="open" x-tran<caret>sition></div>"""
        )
        val attributes = PsiTreeUtil.findChildrenOfType(psiFile, XmlAttribute::class.java)
        val xTransition = attributes.find { it.name == "x-transition" }
        assertNotNull("Should find x-transition attribute", xTransition)

        val doc = provider.generateDoc(xTransition!!.nameElement, xTransition.nameElement)
        assertNotNull("Should generate documentation for x-transition", doc)
        assertTrue("Doc should mention transition", doc!!.contains("transition"))
    }
}
