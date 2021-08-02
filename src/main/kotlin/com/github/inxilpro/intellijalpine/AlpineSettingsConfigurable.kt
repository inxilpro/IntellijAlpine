package com.github.inxilpro.intellijalpine

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class AlpineSettingsConfigurable : Configurable {
    private var mySettingsComponent: AlpineSettingsComponent? = null

    override fun getDisplayName(): String {
        return "Alpine.js"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.preferredFocusedComponent
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = AlpineSettingsComponent()
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings: AlpineSettingsState = AlpineSettingsState.instance

        return mySettingsComponent?.showGutterIconsStatus != settings.showGutterIcons
    }

    override fun apply() {
        val settings: AlpineSettingsState = AlpineSettingsState.instance
        settings.showGutterIcons = mySettingsComponent?.showGutterIconsStatus != false
    }

    override fun reset() {
        val settings: AlpineSettingsState = AlpineSettingsState.instance
        mySettingsComponent?.showGutterIconsStatus = settings.showGutterIcons
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}