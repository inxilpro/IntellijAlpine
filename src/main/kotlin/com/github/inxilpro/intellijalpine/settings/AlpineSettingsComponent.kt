package com.github.inxilpro.intellijalpine.settings

import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.intellij.openapi.project.Project
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AlpineSettingsComponent(private val project: Project?) {
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
            .addComponent(TitledSeparator("Application Settings"))
            .addComponent(myShowGutterIconsStatus, 1)

        // Only show project settings if we have a project context
        if (project != null) {
            builder.addComponent(TitledSeparator("Project Settings"))
            
            // Dynamically add checkboxes for each registered plugin
            AlpinePluginRegistry.instance.getRegisteredPlugins().forEach { plugin ->
                val checkBox = JBCheckBox("Enable ${plugin.getPackageDisplayName()} support for this project")
                pluginCheckBoxes[plugin.getPluginName()] = checkBox
                builder.addComponent(checkBox, 1)
            }
        }

        panel = builder.addComponentFillVertically(JPanel(), 0).panel
    }
}