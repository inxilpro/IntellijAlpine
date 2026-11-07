package com.github.inxilpro.intellijalpine.support

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag

/**
 * The HTML "Missing associated label" inspection accepts a static `aria-label`, but not one
 * Alpine binds at runtime (`:aria-label="'Select ' + name"`), so bound labels are treated
 * as equivalent.
 */
class AlpineBoundLabelHighlightInfoFilter : HighlightInfoFilter {
    override fun accept(info: HighlightInfo, file: PsiFile?): Boolean {
        if (file == null) return true
        if (info.description?.startsWith(MISSING_LABEL_WARNING) != true) return true
        if (!LanguageUtil.supportsAlpineJs(file)) return true

        return !hasBoundLabel(file, info.startOffset)
    }

    private fun hasBoundLabel(file: PsiFile, offset: Int): Boolean =
        file.viewProvider.allFiles.any { root ->
            val tag = root.findElementAt(offset)?.let { PsiTreeUtil.getParentOfType(it, XmlTag::class.java, false) }
            tag != null && tag.attributes.any { it.name in BOUND_LABEL_ATTRIBUTES }
        }

    companion object {
        private const val MISSING_LABEL_WARNING = "Missing associated label"

        internal val BOUND_LABEL_ATTRIBUTES = setOf(
            ":aria-label",
            "x-bind:aria-label",
            ":aria-labelledby",
            "x-bind:aria-labelledby",
        )
    }
}
