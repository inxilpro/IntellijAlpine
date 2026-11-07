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
        "erb", "jsp", "jsf", "ftl", "ftlh", "vm",
    )

    private val FREEMARKER_EXTENSIONS = setOf("ftl", "ftlh")

    private val TEMPLATE_LANGUAGE_IDS = setOf(
        // FreeMarker's language id is "FTL"; "FreeMarker" is only its display name
        "Blade", "PHP", "Twig", "Smarty", "FTL", "FreeMarker",
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

    fun hasFreeMarkerLanguage(file: PsiFile): Boolean {
        // The FreeMarker plugin's language id is "FTL", with dialects such as "FTL>" and "FTL]" for
        // its angle- and square-bracket syntaxes; the extension fallback covers the plugin being absent
        return file.viewProvider.languages.any { language ->
            generateSequence(language) { it.baseLanguage }.any { it.id.startsWith("FTL") }
        } || FREEMARKER_EXTENSIONS.any { file.name.lowercase().endsWith(".$it") }
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