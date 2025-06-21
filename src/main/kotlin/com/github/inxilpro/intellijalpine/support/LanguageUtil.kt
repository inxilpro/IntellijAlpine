package com.github.inxilpro.intellijalpine.support

import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.xml.XMLLanguage
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider

object LanguageUtil {

    private val HTML_LIKE_EXTENSIONS = setOf(
        "html", "htm", "xhtml", "xml",
        "php", "twig", "smarty", "tpl", "phtml",
        "erb", "jsp", "jsf", "ftl", "vm",
    )

    private val TEMPLATE_LANGUAGE_IDS = setOf(
        "Blade", "PHP", "Twig", "Smarty", "FreeMarker",
        "Velocity", "JSP", "ERB",
    )

    fun supportsAlpineJs(file: PsiFile): Boolean {
        return hasHtmlBasedLanguage(file) || hasTemplateLanguage(file) || hasHtmlLikeExtension(file) || isTemplateLanguageFile(
            file
        )
    }

    fun hasPhpLanguage(file: PsiFile): Boolean {
        return file.viewProvider.languages.any { lang ->
            lang.id == "PHP" || lang.id == "Blade"
        }
    }

    private fun hasHtmlBasedLanguage(file: PsiFile): Boolean {
        return file.viewProvider.languages.any { lang ->
            isHtmlBasedLanguage(lang)
        }
    }

    private fun isHtmlBasedLanguage(language: Language): Boolean {
        if (language.isKindOf(HTMLLanguage.INSTANCE)) {
            return true
        }

        if (language.isKindOf(XMLLanguage.INSTANCE)) {
            return TEMPLATE_LANGUAGE_IDS.contains(language.id)
        }

        return false
    }

    private fun hasTemplateLanguage(file: PsiFile): Boolean {
        return file.viewProvider.languages.any { lang ->
            TEMPLATE_LANGUAGE_IDS.contains(lang.id)
        }
    }

    private fun hasHtmlLikeExtension(file: PsiFile): Boolean {
        val fileName = file.name.lowercase()
        return HTML_LIKE_EXTENSIONS.any { ext ->
            fileName.endsWith(".$ext")
        }
    }

    private fun isTemplateLanguageFile(file: PsiFile): Boolean {
        return file.viewProvider is TemplateLanguageFileViewProvider
    }
}