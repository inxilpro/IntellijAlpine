package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.html.HtmlTag

class AutoPopupHandler : TypedHandlerDelegate() {
    @Suppress("ReturnCount")
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (LookupManager.getActiveLookup(editor) != null) {
            return Result.CONTINUE
        }

        val element = file.findElementAt(editor.caretModel.offset)
        if (element?.parent !is HtmlTag) {
            return Result.CONTINUE
        }

        if (charTyped == '@' || charTyped == ':') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
            return Result.STOP
        }

        return Result.CONTINUE
    }
}
