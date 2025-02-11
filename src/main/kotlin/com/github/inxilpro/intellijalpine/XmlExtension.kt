package com.github.inxilpro.intellijalpine

import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.impl.source.xml.SchemaPrefix
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.HtmlXmlExtension

class XmlExtension : HtmlXmlExtension() {
        override fun isAvailable(file: PsiFile?): Boolean {
        if (file == null) return false
        val languages = file.viewProvider.languages.map { it.id }
        return "HTML" in languages || "Blade" in languages || "PHP" in languages
    }

    override fun getPrefixDeclaration(context: XmlTag, namespacePrefix: String?): SchemaPrefix? {
        if (null != namespacePrefix && context is HtmlTag && hasAlpinePrefix(namespacePrefix)) {
            findAttributeSchema(context, namespacePrefix)
                ?.let { return it }
        }

        return super.getPrefixDeclaration(context, namespacePrefix)
    }

    private fun hasAlpinePrefix(namespacePrefix: String): Boolean {
        return AttributeUtil.isXmlPrefix(namespacePrefix)
    }

    private fun findAttributeSchema(context: XmlTag, namespacePrefix: String): SchemaPrefix? {
        return context.attributes
            .find { it.name.startsWith(namespacePrefix) }
            ?.let { SchemaPrefix(it, TextRange.create(0, namespacePrefix.length), "Alpine.js") }
    }
}
