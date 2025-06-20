package com.github.inxilpro.intellijalpine.settings

import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.github.inxilpro.intellijalpine.settings.AlpineSettingsComponent
import com.github.inxilpro.intellijalpine.settings.AlpineSettingsState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class AlpineSettingsConfigurable(private val project: Project?) : Configurable {
    private var mySettingsComponent: AlpineSettingsComponent? = null

    override fun getDisplayName(): String {
        return "Alpine.js"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.preferredFocusedComponent
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = AlpineSettingsComponent(project)
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val appSettings = AlpineSettingsState.Companion.instance
        var isModified = mySettingsComponent?.showGutterIconsStatus != appSettings.showGutterIcons

        // Check project settings if we have a project
        if (project != null) {
            val registry = AlpinePluginRegistry.getInstance()
            registry.getRegisteredPlugins().forEach { plugin ->
                val pluginName = plugin.getPluginName()
                val currentStatus = mySettingsComponent?.getPluginStatus(pluginName) ?: false
                val savedStatus = registry.isPluginEnabled(project, pluginName)
                if (currentStatus != savedStatus) {
                    isModified = true
                }
            }
        }

        return isModified
    }

    override fun apply() {
        val appSettings = AlpineSettingsState.Companion.instance
        appSettings.showGutterIcons = mySettingsComponent?.showGutterIconsStatus != false

        // Apply project settings if we have a project
        if (project != null) {
            val registry = AlpinePluginRegistry.getInstance()
            registry.getRegisteredPlugins().forEach { plugin ->
                val pluginName = plugin.getPluginName()
                val enabled = mySettingsComponent?.getPluginStatus(pluginName) ?: false
                if (enabled) {
                    registry.enablePlugin(project, pluginName)
                } else {
                    registry.disablePlugin(project, pluginName)
                }
            }
        }
    }

    override fun reset() {
        val appSettings = AlpineSettingsState.Companion.instance
        mySettingsComponent?.showGutterIconsStatus = appSettings.showGutterIcons

        // Reset project settings if we have a project
        if (project != null) {
            val registry = AlpinePluginRegistry.getInstance()
            registry.getRegisteredPlugins().forEach { plugin ->
                val pluginName = plugin.getPluginName()
                val enabled = registry.isPluginEnabled(project, pluginName)
                mySettingsComponent?.setPluginStatus(pluginName, enabled)
            }
        }
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}