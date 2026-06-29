package com.github.inxilpro.intellijalpine.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlAttribute

class DeprecatedXSpreadInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : XmlElementVisitor() {
            override fun visitXmlAttribute(attribute: XmlAttribute) {
                if (attribute.name == "x-spread") {
                    holder.registerProblem(
                        attribute.nameElement,
                        "The 'x-spread' directive is deprecated in Alpine.js v3. Use 'x-bind' with an object instead."
                    )
                }
            }
        }
    }
}
