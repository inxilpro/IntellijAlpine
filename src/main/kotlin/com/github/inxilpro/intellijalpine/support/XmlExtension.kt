package com.github.inxilpro.intellijalpine.support

import com.github.inxilpro.intellijalpine.attributes.AttributeUtil
import com.github.inxilpro.intellijalpine.support.LanguageUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.impl.source.xml.SchemaPrefix
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.HtmlXmlExtension

class XmlExtension : HtmlXmlExtension() {
    override fun isAvailable(file: PsiFile?): Boolean {
        if (file == null) return false
        return LanguageUtil.supportsAlpineJs(file)
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