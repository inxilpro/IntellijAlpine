package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.daemon.impl.analysis.XmlHighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.XmlHighlightingAwareElementDescriptor
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.XmlQuickFixFactory
import com.intellij.codeInspection.htmlInspections.RequiredAttributesInspection
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.html.impl.providers.HtmlAttributeValueProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.xml.XmlTag
import com.intellij.util.containers.JBIterable
import com.intellij.xml.XmlExtension
import com.intellij.xml.analysis.XmlAnalysisBundle
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor
import com.intellij.xml.util.HtmlUtil
import com.intellij.xml.util.XmlTagUtil
import com.intellij.xml.util.XmlUtil
import java.util.*

/**
 * Essentially a copy-paste from RequiredAttributesInspection,
 * but with the addition of a check for any bounded required attributes, like ":src" in <img/> tags.
 */
class AlpineRequiredAttributesInspection : RequiredAttributesInspection() {
    override fun checkTag(tag: XmlTag, holder: ProblemsHolder, isOnTheFly: Boolean) {
        val name = tag.name
        var elementDescriptor = XmlUtil.getDescriptorFromContext(tag)
        if (elementDescriptor is AnyXmlElementDescriptor || elementDescriptor == null) {
            elementDescriptor = tag.descriptor
        }

        if (elementDescriptor == null) return
        if (elementDescriptor is XmlHighlightingAwareElementDescriptor &&
                !(elementDescriptor as XmlHighlightingAwareElementDescriptor).shouldCheckRequiredAttributes()) {
            return
        }

        val attributeDescriptors = elementDescriptor.getAttributesDescriptors(tag)
        var requiredAttributes: MutableSet<String>? = null

        for (attribute in attributeDescriptors) {
            if (attribute != null && attribute.isRequired) {
                if (requiredAttributes == null) {
                    requiredAttributes = HashSet()
                }
                requiredAttributes.add(attribute.getName(tag))
            }
        }

        if (requiredAttributes != null) {
            for (attrName in requiredAttributes) {
                if (!hasAttribute(tag, attrName) && AttributeUtil.bindPrefixes.all { !hasAttribute(tag, it + attrName) } &&
                        !XmlExtension.getExtension(tag.containingFile).isRequiredAttributeImplicitlyPresent(tag, attrName)) {
                    val insertRequiredAttributeIntention: LocalQuickFix? = if (isOnTheFly) XmlQuickFixFactory.getInstance().insertRequiredAttributeFix(tag, attrName) else null
                    val localizedMessage = XmlAnalysisBundle.message("xml.inspections.element.doesnt.have.required.attribute", name, attrName)
                    reportOneTagProblem(
                            tag,
                            attrName,
                            localizedMessage,
                            insertRequiredAttributeIntention,
                            holder,
                            getIntentionAction(attrName),
                            isOnTheFly
                    )
                }
            }
        }
    }

    private fun hasAttribute(tag: XmlTag, attrName: String): Boolean {
        if (JBIterable.from(HtmlAttributeValueProvider.EP_NAME.extensionList)
                        .filterMap { it: HtmlAttributeValueProvider -> it.getCustomAttributeValue(tag, attrName) }.first() != null) {
            return true
        }
        val attribute = tag.getAttribute(attrName) ?: return false
        if (attribute.valueElement != null) return true
        if (tag !is HtmlTag) return false
        val descriptor = attribute.descriptor
        return descriptor != null && HtmlUtil.isBooleanAttribute(descriptor, tag)
    }

    private fun reportOneTagProblem(
            tag: XmlTag,
            name: String,
            localizedMessage: @InspectionMessage String,
            basicIntention: LocalQuickFix?,
            holder: ProblemsHolder,
            addAttributeFix: LocalQuickFix,
            isOnTheFly: Boolean,
    ) {
        var htmlTag = false
        if (tag is HtmlTag) {
            htmlTag = true
            if (isAdditionallyDeclared(additionalEntries, name)) return
        }
        val fixes: Array<LocalQuickFix>
        val highlightType: ProblemHighlightType
        if (htmlTag) {
            fixes = if (basicIntention == null) arrayOf(addAttributeFix) else arrayOf(addAttributeFix, basicIntention)
            highlightType = if (XmlHighlightVisitor.isInjectedWithoutValidation(tag)) ProblemHighlightType.INFORMATION else ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        } else {
            fixes = if (basicIntention == null) LocalQuickFix.EMPTY_ARRAY else arrayOf(basicIntention)
            highlightType = ProblemHighlightType.ERROR
        }
        if (isOnTheFly || highlightType != ProblemHighlightType.INFORMATION) {
            addElementsForTag(tag, localizedMessage, highlightType, holder, isOnTheFly, *fixes)
        }
    }

    private fun addElementsForTag(
            tag: XmlTag,
            message: String,
            error: ProblemHighlightType,
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            vararg fixes: LocalQuickFix,
    ) {
        val start = XmlTagUtil.getStartTagNameElement(tag) as PsiElement? ?: return
        holder.registerProblem(start, message, error, *fixes)

        if (isOnTheFly) {
            val end = XmlTagUtil.getEndTagNameElement(tag) as PsiElement? ?: return
            holder.registerProblem(end, message, error, *fixes)
        }
    }

    private fun isAdditionallyDeclared(additional: String, name: String): Boolean {
        val newName = StringUtil.toLowerCase(name)
        if (!additional.contains(newName)) return false
        val tokenizer = StringTokenizer(additional, ", ")
        while (tokenizer.hasMoreTokens()) {
            if (newName == tokenizer.nextToken()) {
                return true
            }
        }
        return false
    }
}