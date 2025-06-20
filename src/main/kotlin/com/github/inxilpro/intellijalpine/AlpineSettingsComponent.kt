package com.github.inxilpro.intellijalpine

import com.intellij.openapi.project.Project
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AlpineSettingsComponent(private val project: Project?) {
    val panel: JPanel

    private val myShowGutterIconsStatus = JBCheckBox("Show Alpine gutter icons")
    private val myEnableAlpineAjaxStatus = JBCheckBox("Enable alpine-ajax support for this project")

    val preferredFocusedComponent: JComponent
        get() = myShowGutterIconsStatus

    var showGutterIconsStatus: Boolean
        get() = myShowGutterIconsStatus.isSelected
        set(newStatus) {
            myShowGutterIconsStatus.isSelected = newStatus
        }

    var enableAlpineAjaxStatus: Boolean
        get() = myEnableAlpineAjaxStatus.isSelected
        set(newStatus) {
            myEnableAlpineAjaxStatus.isSelected = newStatus
        }

    init {
        val builder = FormBuilder.createFormBuilder()
            .addComponent(TitledSeparator("Application Settings"))
            .addComponent(myShowGutterIconsStatus, 1)

        // Only show project settings if we have a project context
        if (project != null) {
            builder.addComponent(TitledSeparator("Project Settings"))
                .addComponent(myEnableAlpineAjaxStatus, 1)
        }

        panel = builder.addComponentFillVertically(JPanel(), 0).panel
    }
}
