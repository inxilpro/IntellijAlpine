package com.github.inxilpro.intellijalpine

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.xml.XmlTokenImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import javax.swing.Icon

class AlpineLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun getIcon(): Icon {
        return Alpine.ICON
    }

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>
    ) {
        if (element is XmlAttribute && element.descriptor is AlpineAttributeDescriptor) {

            val token = PsiTreeUtil.getChildOfType(element, XmlTokenImpl::class.java) ?: return

            val builder = NavigationGutterIconBuilder.create(Alpine.ICON)
                .setTarget(token)
                .setTooltipText("Alpine.js directive")

            result.add(builder.createLineMarkerInfo(token))
        }
    }
}