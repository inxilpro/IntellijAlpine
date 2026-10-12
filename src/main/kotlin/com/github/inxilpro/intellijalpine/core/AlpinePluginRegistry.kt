package com.github.inxilpro.intellijalpine.core

import com.github.inxilpro.intellijalpine.attributes.AttributeInfo
import com.github.inxilpro.intellijalpine.completion.AutoCompleteSuggestions
import com.github.inxilpro.intellijalpine.settings.AlpineProjectSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.apache.commons.lang3.tuple.MutablePair

@Service(Service.Level.APP)
class AlpinePluginRegistry {
    companion object {
        val instance: AlpinePluginRegistry
            get() = ApplicationManager.getApplication().getService(AlpinePluginRegistry::class.java)
    }

    fun getRegisteredPlugins(): List<AlpinePlugin> {
        return AlpinePlugin.EP_NAME.extensionList
    }

    fun getEnabledPlugins(project: Project): List<AlpinePlugin> {
        val settings = AlpineProjectSettingsState.getInstance(project)
        return getRegisteredPlugins().filter { settings.isPluginEnabled(it.getPluginName()) }
    }

    fun isPluginEnabled(project: Project, pluginName: String): Boolean {
        return AlpineProjectSettingsState.getInstance(project).isPluginEnabled(pluginName)
    }

    fun isPluginEnabled(project: Project, plugin: AlpinePlugin): Boolean {
        return isPluginEnabled(project, plugin.getPluginName())
    }

    fun enablePlugin(project: Project, pluginName: String) {
        AlpineProjectSettingsState.getInstance(project).setPluginEnabled(pluginName, true)
    }

    fun disablePlugin(project: Project, pluginName: String) {
        AlpineProjectSettingsState.getInstance(project).setPluginEnabled(pluginName, false)
    }

    fun getAllDirectives(project: Project): List<String> {
        return getEnabledPlugins(project).flatMap { it.getDirectives() }
    }

    fun getAllPrefixes(project: Project): List<String> {
        return getEnabledPlugins(project).flatMap { it.getPrefixes() }
    }

    fun getTypeText(info: AttributeInfo): String? {
        return getRegisteredPlugins().firstNotNullOfOrNull { it.getTypeText(info) }
    }

    fun injectAllJsContext(project: Project, context: MutablePair<String, String>): MutablePair<String, String> {
        return getEnabledPlugins(project).fold(context) { acc, plugin ->
            plugin.injectJsContext(acc)
        }
    }

    fun injectAllAutoCompleteSuggestions(project: Project, suggestions: AutoCompleteSuggestions) {
        getEnabledPlugins(project).forEach { it.injectAutoCompleteSuggestions(suggestions) }
    }
}