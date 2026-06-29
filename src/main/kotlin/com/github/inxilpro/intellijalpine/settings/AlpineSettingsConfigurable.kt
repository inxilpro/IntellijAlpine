package com.github.inxilpro.intellijalpine.settings

import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class AlpineSettingsConfigurable(private val project: Project?) : Configurable {
    private lateinit var showGutterIcons: JBCheckBox
    private val pluginCheckBoxes = mutableMapOf<String, JBCheckBox>()

    @Suppress("DialogTitleCapitalization")
    override fun getDisplayName(): String = "Alpine.js"

    override fun createComponent(): JComponent {
        showGutterIcons = JBCheckBox("Show Alpine gutter icons")
        pluginCheckBoxes.clear()

        return panel {
            group("Plugin Settings") {
                row { cell(showGutterIcons) }
            }

            if (project != null) {
                group("Project Settings for “${project.name}”") {
                    AlpinePluginRegistry.instance.getRegisteredPlugins().forEach { plugin ->
                        row {
                            val cb = JBCheckBox("Enable “${plugin.getPackageDisplayName()}” support for this project")
                            pluginCheckBoxes[plugin.getPluginName()] = cb
                            cell(cb)
                        }
                    }
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val appSettings = AlpineSettingsState.instance
        if (showGutterIcons.isSelected != appSettings.showGutterIcons) return true

        if (project != null) {
            val registry = AlpinePluginRegistry.instance
            for ((pluginName, cb) in pluginCheckBoxes) {
                if (cb.isSelected != registry.isPluginEnabled(project, pluginName)) return true
            }
        }

        return false
    }

    override fun apply() {
        AlpineSettingsState.instance.showGutterIcons = showGutterIcons.isSelected

        if (project != null) {
            val registry = AlpinePluginRegistry.instance
            for ((pluginName, cb) in pluginCheckBoxes) {
                if (cb.isSelected) {
                    registry.enablePlugin(project, pluginName)
                } else {
                    registry.disablePlugin(project, pluginName)
                }
            }
        }
    }

    override fun reset() {
        showGutterIcons.isSelected = AlpineSettingsState.instance.showGutterIcons

        if (project != null) {
            val registry = AlpinePluginRegistry.instance
            for ((pluginName, cb) in pluginCheckBoxes) {
                cb.isSelected = registry.isPluginEnabled(project, pluginName)
            }
        }
    }
}
