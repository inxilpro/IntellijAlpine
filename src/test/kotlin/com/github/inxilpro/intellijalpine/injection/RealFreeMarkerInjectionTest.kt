package com.github.inxilpro.intellijalpine.injection

import com.intellij.freemarker.psi.files.FtlFileType
import com.intellij.lang.javascript.inspections.JSUnusedLocalSymbolsInspection
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.lang.javascript.psi.JSEmptyStatement
import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue

/**
 * FreeMarker masking with the FreeMarker plugin active, where template code reaches the injector as
 * OuterLanguageElements. Built only when testing against IntelliJ IDEA (see build.gradle.kts).
 */
class RealFreeMarkerInjectionTest : AlpineInjectionTestCase() {
    override val fileName = "test.ftl"

    private fun realFtlJs(html: String, attributeName: String, fileName: String = this.fileName) =
        cleanJs(html, attributeName, fileName).also {
        val file = myFixture.file
        assertEquals("The FreeMarker file type should be active", FtlFileType.INSTANCE, file.fileType)
        val host = file.viewProvider.allFiles
            .flatMap { root -> PsiTreeUtil.findChildrenOfType(root, XmlAttributeValue::class.java) }
            .first { (it.parent as? XmlAttribute)?.name == attributeName }
        assertTrue(
            "Expected FreeMarker PSI to expose template code as OuterLanguageElements:\n${host.text}",
            PsiTreeUtil.findChildrenOfType(host, OuterLanguageElement::class.java).isNotEmpty(),
        )
    }

    fun testOnlyFirstIfArmIsKept() {
        val js = realFtlJs(
            """
            <div x-data x-init="
                setTheme([#if dark??]'dark'[#else]'light'[/#if]);
                setLocale([#if locale??]'${'$'}{locale}'[#else]'en'[/#if]);
            "></div>
            """.trimIndent(),
            "x-init",
        )
        assertFalse("The #else arms should be masked:\n${js.text}", js.text.contains("'light'") || js.text.contains("'en'"))
        assertTrue(
            "Statement terminators should not parse as empty statements:\n${js.text}",
            PsiTreeUtil.findChildrenOfType(js, JSEmptyStatement::class.java).isEmpty(),
        )
    }

    fun testInterpolationNextToATagKeepsItsPlaceholder() {
        val js = realFtlJs("""<div x-data="{ value: [#if ok]${'$'}{amount}[#else]0[/#if] }"></div>""", "x-data")
        assertTrue("The first arm's value must survive:\n${js.text}", js.text.contains("value: __SERVER_VALUE()"))
        assertObjectLiteralKeys(js, setOf("value"))
    }

    fun testObjectLiteralWithInterpolationsAndOptionalMembers() {
        val js = realFtlJs(
            """
            <div x-data x-init="setup({
                    id: ${'$'}{product.id?c},
                    title: '${'$'}{product.title?js_string}',
                    inStock: ${'$'}{product.inStock?c},
                    [#if product.sale??]
                    salePrice: ${'$'}{product.sale.price?c},
                    [/#if]
                    tags: ${'$'}{tagsJson}
                })"></div>
            """.trimIndent(),
            "x-init",
        )
        assertObjectLiteralKeys(js, setOf("id", "title", "inStock", "salePrice", "tags"))
        assertPlaceholdersTypeAsAny(js)
    }

    fun testDollarEscapeKeepsJavaScriptTemplateLiteral() {
        val escapedDollar = "\${'$'}"
        val js = realFtlJs("""<div x-data="{ label: user => `${escapedDollar}{user.name}` }"></div>""", "x-data")
        assertTrue(js.text.contains("`\${user.name}`"))
    }

    fun testSelfClosingMacroCallsStandInForValues() {
        val js = realFtlJs("""<div x-init="setup([@json value=rows /], [@json value=columns[0] /])"></div>""", "x-init")
        assertTrue(js.text.contains("setup(__SERVER_VALUE(), __SERVER_VALUE())"))
    }

    fun testXForVariablesAreNotReportedUnused() {
        val js = realFtlJs(
            """
            <div x-data="{ errors: [] }">
                <template x-for="( error, index ) in [#if errors??]errors[#else][][/#if]" :key="index">
                    <p x-text="error.message"></p>
                </template>
            </div>
            """.trimIndent(),
            "x-for",
        )
        assertTrue(js.text.contains("void error;") && js.text.contains("void index;"))
        myFixture.enableInspections(JSUnusedLocalSymbolsInspection())
        val unused = myFixture.doHighlighting().filter { it.description?.contains("Unused") == true }
        assertTrue("x-for variables used by child directives were reported unused: $unused", unused.isEmpty())
    }

    fun testFreeMarkerFilesWithOtherExtensionsAreRecognizedByLanguage() {
        runWriteAction { FileTypeManager.getInstance().associateExtension(FtlFileType.INSTANCE, "tmpl") }
        try {
            val js = realFtlJs("""<div x-data="{ value: [#if ok]${'$'}{amount}[#else]0[/#if] }"></div>""", "x-data", "page.tmpl")
            assertTrue(js.text.contains("value: __SERVER_VALUE()"))
        } finally {
            runWriteAction { FileTypeManager.getInstance().removeAssociatedExtension(FtlFileType.INSTANCE, "tmpl") }
        }
    }
}
