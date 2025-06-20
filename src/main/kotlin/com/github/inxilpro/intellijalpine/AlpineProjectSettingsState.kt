package com.github.inxilpro.intellijalpine

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "com.github.inxilpro.intellijalpine.AlpineProjectSettingsState", storages = [Storage("alpine-project.xml")])
class AlpineProjectSettingsState : PersistentStateComponent<AlpineProjectSettingsState?> {
    var enableAlpineAjax = false
    var enableAlpineWizard = false

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