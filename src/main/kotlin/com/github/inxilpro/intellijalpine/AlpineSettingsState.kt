package com.github.inxilpro.intellijalpine

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "com.github.inxilpro.intellijalpine.AppSettingsState", storages = [Storage("IntellijAlpine.xml")])
class AlpineSettingsState : PersistentStateComponent<AlpineSettingsState?> {
    var showGutterIcons = true

    override fun getState(): AlpineSettingsState? {
        return this
    }

    override fun loadState(state: AlpineSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: AlpineSettingsState
            get() = ApplicationManager.getApplication().getService(AlpineSettingsState::class.java)
    }
}
