package com.github.inxilpro.intellijalpine.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "com.github.inxilpro.intellijalpine.AlpineProjectSettingsState", storages = [Storage("alpinejs-support.xml")])
class AlpineProjectSettingsState : PersistentStateComponent<AlpineProjectSettingsState?> {
    var enabledPlugins = mutableMapOf<String, Boolean>()

    fun isPluginEnabled(pluginName: String): Boolean {
        return enabledPlugins[pluginName] ?: false
    }

    fun setPluginEnabled(pluginName: String, enabled: Boolean) {
        enabledPlugins[pluginName] = enabled
    }

    override fun getState(): AlpineProjectSettingsState? {
        return this
    }

    override fun loadState(state: AlpineProjectSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): AlpineProjectSettingsState {
            return project.getService(AlpineProjectSettingsState::class.java)
        }
    }
}