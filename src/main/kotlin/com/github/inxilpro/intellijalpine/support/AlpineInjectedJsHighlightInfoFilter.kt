package com.github.inxilpro.intellijalpine.support

import com.github.inxilpro.intellijalpine.attributes.AttributeUtil
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.javascript.psi.JSAssignmentExpression
import com.intellij.lang.javascript.psi.JSDefinitionExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue

/**
 * Filters JavaScript diagnostics that cannot apply inside Alpine attribute expressions.
 *
 * Alpine evaluates attribute expressions against the component's data, so a bare identifier is
 * component state at runtime. IntelliJ's static resolution instead falls back to module members
 * ("Element is not exported") or to window globals (assigning to `open` resolves to `window.open`),
 * producing warnings the runtime scoping makes impossible. Only bare identifiers are affected:
 * qualified references keep their warnings, and an assignment warning is only dropped when the
 * name is a property of the component's `x-data` object literal.
 */
class AlpineInjectedJsHighlightInfoFilter : HighlightInfoFilter {
    override fun accept(info: HighlightInfo, file: PsiFile?): Boolean {
        if (file == null) return true
        val description = info.description ?: return true
        if (!isCandidate(description)) return true
        if (!isAlpineAttributeExpression(file)) return true

        return !shouldSuppress(
            description,
            isBareReference = { bareReferenceAt(file, info.startOffset) != null },
            assignsToComponentProperty = { assignsToComponentProperty(file, info.startOffset) },
        )
    }

    private fun isAlpineAttributeExpression(file: PsiFile): Boolean {
        val host = InjectedLanguageManager.getInstance(file.project).getInjectionHost(file) as? XmlAttributeValue
            ?: return false
        return AttributeUtil.isValidInjectionTarget(host)
    }

    private fun bareReferenceAt(file: PsiFile, offset: Int): JSReferenceExpression? {
        val element = file.findElementAt(offset) ?: return null
        val reference = PsiTreeUtil.getParentOfType(element, JSReferenceExpression::class.java, false) ?: return null
        return reference.takeIf { it.qualifier == null }
    }

    private fun assignsToComponentProperty(file: PsiFile, offset: Int): Boolean {
        val element = file.findElementAt(offset) ?: return false
        val assignment = PsiTreeUtil.getParentOfType(element, JSAssignmentExpression::class.java) ?: return false
        val target = (assignment.lOperand as? JSDefinitionExpression)?.expression as? JSReferenceExpression
            ?: assignment.lOperand as? JSReferenceExpression
            ?: return false
        if (target.qualifier != null) return false
        val name = target.referenceName ?: return false

        // A local variable or parameter of the expression shadows component state, so its warnings are real
        val resolved = target.resolve()
        if (resolved is JSVariable && resolved.containingFile == file && resolved.name != "\$data") return false

        // Alpine assigns a bare name that is an x-data property to component state, whatever the IDE resolved it to
        return name in componentDataProperties(file)
    }

    /** Property names of the `x-data` object literal the injector declares as `$data`, if it is a literal. */
    private fun componentDataProperties(file: PsiFile): Set<String> {
        val data = PsiTreeUtil.findChildrenOfType(file, JSVariable::class.java).firstOrNull { it.name == "\$data" }
        val literal = data?.initializer as? JSObjectLiteralExpression ?: return emptySet()
        return literal.properties.mapNotNull { it.name }.toSet()
    }

    companion object {
        private const val NOT_EXPORTED_WARNING = "Element is not exported"
        private const val ASSIGNMENT_MISMATCH_WARNING = "Assigned expression type "

        internal fun isCandidate(description: String): Boolean =
            description.startsWith(NOT_EXPORTED_WARNING) || description.startsWith(ASSIGNMENT_MISMATCH_WARNING)

        internal fun shouldSuppress(
            description: String,
            isBareReference: () -> Boolean,
            assignsToComponentProperty: () -> Boolean,
        ): Boolean =
            when {
                description.startsWith(NOT_EXPORTED_WARNING) -> isBareReference()
                description.startsWith(ASSIGNMENT_MISMATCH_WARNING) -> assignsToComponentProperty()
                else -> false
            }
    }
}
