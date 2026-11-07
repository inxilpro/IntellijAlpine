package com.github.inxilpro.intellijalpine.injection

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.UnknownFileType

/**
 * FreeMarker masking via the regex fallback: .ftl files are parsed as plain HTML here, as they are
 * when the FreeMarker plugin is absent, so only the file extension marks them as FreeMarker.
 */
class FreeMarkerInjectionTest : AlpineInjectionTestCase() {
    override val fileName = "test.ftl"

    private val restoreFtl = mutableListOf<() -> Unit>()

    override fun setUp() {
        super.setUp()
        restoreFtl += parseAsHtml("ftl")
    }

    override fun tearDown() {
        try {
            restoreFtl.forEach { it() }
        } finally {
            super.tearDown()
        }
    }

    /** Parses files with [extension] as plain HTML; returns a function restoring the previous association. */
    private fun parseAsHtml(extension: String): () -> Unit {
        val manager = FileTypeManager.getInstance()
        var previous: FileType? = null
        runWriteAction {
            previous = manager.getFileTypeByExtension(extension).takeUnless { it is UnknownFileType }
            previous?.let { manager.removeAssociatedExtension(it, extension) }
            manager.associateExtension(HtmlFileType.INSTANCE, extension)
        }
        return {
            runWriteAction {
                manager.removeAssociatedExtension(HtmlFileType.INSTANCE, extension)
                previous?.let { manager.associateExtension(it, extension) }
            }
        }
    }

    fun testInterpolationsBecomeTypedPlaceholders() {
        val js = cleanJs("""<div x-data="{ count: ${'$'}{cart.items?size}, total: ${'$'}{cart.total?c} }"></div>""", "x-data")
        assertObjectLiteralKeys(js, setOf("count", "total"))
        assertEquals(2, placeholderCount(js))
        assertPlaceholdersTypeAsAny(js)
    }

    fun testInterpolationInsideExpression() {
        cleanJs("""<div x-data="{ id: 0 }"><span x-show="id === ${'$'}{product.id?c}"></span></div>""", "x-show")
    }

    fun testMultipleInterpolationsInStatements() {
        cleanJs(
            """
            <div x-data="{ a: 0, b: 0 }">
                <div x-effect="if (a === ${'$'}{first?c}) { b = ${'$'}{second?c}; }"></div>
            </div>
            """.trimIndent(),
            "x-effect",
        )
    }

    fun testExistsOperatorInsideInterpolation() {
        val js = cleanJs("""<div x-show="isOpen && ${'$'}{(user?? && user.admin)?c}"></div>""", "x-show")
        assertFalse("FreeMarker code should be masked:\n${js.text}", js.text.contains("user??"))
    }

    fun testControlFlowTagsVanishWhileInterpolationsStay() {
        val js = cleanJs(
            """
            <div x-init="setup({
                name: ${'$'}{name},
                [#if email??]
                    email: ${'$'}{email},
                [/#if]
            })"></div>
            """.trimIndent(),
            "x-init",
        )
        assertTrue(js.text.contains("name: __SERVER_VALUE()") && js.text.contains("email: __SERVER_VALUE()"))
        assertEquals("Control-flow tags must not add placeholders:\n${js.text}", 2, placeholderCount(js))
    }

    fun testOnlyFirstIfArmIsKept() {
        val js = cleanJs(
            """<div x-data x-init="setTheme([#if dark??]'dark'[#else]'light'[/#if])"></div>""",
            "x-init",
        )
        assertTrue(js.text.contains("setTheme('dark')"))
        assertFalse("The #else arm should be masked:\n${js.text}", js.text.contains("'light'"))
    }

    fun testInterpolationNextToATagKeepsItsPlaceholder() {
        val js = cleanJs("""<div x-data="{ value: [#if ok]${'$'}{amount}[#else]0[/#if] }"></div>""", "x-data")
        assertTrue("The first arm's value must survive:\n${js.text}", js.text.contains("value: __SERVER_VALUE()"))
        assertObjectLiteralKeys(js, setOf("value"))
    }

    fun testDollarEscapeKeepsJavaScriptTemplateLiteral() {
        val escapedDollar = "\${'$'}"
        val js = cleanJs("""<div x-data="{ label: user => `${escapedDollar}{user.name}` }"></div>""", "x-data")
        assertTrue(js.text.contains("`\${user.name}`"))
        assertFalse(js.text.contains("__SERVER_VALUE(){user.name}"))
    }

    fun testSelfClosingMacroCallsStandInForValues() {
        val js = cleanJs(
            """<div x-init="setup([@json value=rows /], [@json value=columns[0] /], <@json value=options />)"></div>""",
            "x-init",
        )
        assertTrue(js.text.contains("setup(__SERVER_VALUE(), __SERVER_VALUE(), __SERVER_VALUE())"))
    }

    fun testComparisonInsideAngleBracketTagIsMasked() {
        val js = cleanJs("""<div x-data x-init="start(<#if (count > 1)>'many'<#else>'one'</#if>)"></div>""", "x-init")
        assertTrue(js.text.contains("start('many')"))
    }

    fun testNestedCallInsideAngleBracketTagIsMasked() {
        val js = cleanJs("""<div x-data x-init="start(<#if (size(items) > 1)>'many'<#else>'one'</#if>)"></div>""", "x-init")
        assertTrue(js.text.contains("start('many')"))
    }

    fun testTailwindArbitraryValuesAreNotTemplateCode() {
        val js = cleanJs("""<div x-data="{ on: true }" :class="on ? 'bg-[#343434]' : 'bg-[#000000]'"></div>""", ":class")
        assertTrue("Bracketed class names are plain text:\n${js.text}", js.text.contains("'bg-[#343434]'"))
        assertEquals(0, placeholderCount(js))
    }

    fun testXForSourceWithInterpolationIsMasked() {
        cleanJs("""<template x-for="item in ${'$'}{hasItems?c} ? items : []"><i x-text="item"></i></template>""", "x-for")
    }

    fun testAttributeThatIsAllTemplateCodeStillInjects() {
        val js = cleanJs("""<div x-data="${'$'}{componentJson}"></div>""", "x-data")
        assertEquals(1, placeholderCount(js))
    }

    fun testFtlhFilesAreFreeMarker() {
        restoreFtl += parseAsHtml("ftlh")
        val js = cleanJs("""<div x-show="${'$'}{visible?c}"></div>""", "x-show", fileName = "page.ftlh")
        assertEquals(1, placeholderCount(js))
    }
}
