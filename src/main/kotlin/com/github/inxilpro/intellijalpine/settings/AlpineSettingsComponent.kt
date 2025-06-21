package com.github.inxilpro.intellijalpine.settings

import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.intellij.openapi.project.Project
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.JPanel

class AlpineSettingsComponent(project: Project?) {
    val panel: JPanel

    private val myShowGutterIconsStatus = JBCheckBox("Show Alpine gutter icons")
    private val pluginCheckBoxes = mutableMapOf<String, JBCheckBox>()

    val preferredFocusedComponent: JComponent
        get() = myShowGutterIconsStatus

    var showGutterIconsStatus: Boolean
        get() = myShowGutterIconsStatus.isSelected
        set(newStatus) {
            myShowGutterIconsStatus.isSelected = newStatus
        }

    fun getPluginStatus(pluginName: String): Boolean {
        return pluginCheckBoxes[pluginName]?.isSelected ?: false
    }

    fun setPluginStatus(pluginName: String, enabled: Boolean) {
        pluginCheckBoxes[pluginName]?.isSelected = enabled
    }

    init {
        val builder = FormBuilder.createFormBuilder()
            .addComponent(TitledSeparator("Plugin Settings"))
            .addComponent(myShowGutterIconsStatus, 1)

        // Only show project settings if we have a project context
        if (project != null) {
            builder.addVerticalGap(10)  // Add spacing between sections
                .addComponent(TitledSeparator("Project Settings for “${project.name}”"))

            val projectLabel = JBLabel("These settings apply only to the current project")
            projectLabel.foreground = UIUtil.getContextHelpForeground()
            projectLabel.font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL)
            builder.addComponent(projectLabel, 1)
                .addVerticalGap(5)

            // Dynamically add checkboxes for each registered plugin
            AlpinePluginRegistry.instance.getRegisteredPlugins().forEach { plugin ->
                val checkBox = JBCheckBox("Enable “${plugin.getPackageDisplayName()}” support for this project")
                pluginCheckBoxes[plugin.getPluginName()] = checkBox
                builder.addComponent(checkBox, 1)
            }
        }

        panel = builder.addComponentFillVertically(JPanel(), 0).panel
    }
}