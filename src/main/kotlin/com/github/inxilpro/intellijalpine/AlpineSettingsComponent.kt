package com.github.inxilpro.intellijalpine

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AlpineSettingsComponent {
    val panel: JPanel

    private val myShowGutterIconsStatus = JBCheckBox("Show Alpine gutter icons? ")

    val preferredFocusedComponent: JComponent
        get() = myShowGutterIconsStatus

    var showGutterIconsStatus: Boolean
        get() = myShowGutterIconsStatus.isSelected
        set(newStatus) {
            myShowGutterIconsStatus.isSelected = newStatus
        }

    init {
        panel = FormBuilder.createFormBuilder()
            .addComponent(myShowGutterIconsStatus, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}
