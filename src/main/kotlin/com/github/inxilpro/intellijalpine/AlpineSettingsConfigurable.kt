package com.github.inxilpro.intellijalpine

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
        val appSettings = AlpineSettingsState.instance
        var isModified = mySettingsComponent?.showGutterIconsStatus != appSettings.showGutterIcons

        // Check project settings if we have a project
        if (project != null) {
            val projectSettings = AlpineProjectSettingsState.getInstance(project)
            isModified = isModified || mySettingsComponent?.enableAlpineAjaxStatus != projectSettings.enableAlpineAjax
        }

        return isModified
    }

    override fun apply() {
        val appSettings = AlpineSettingsState.instance
        appSettings.showGutterIcons = mySettingsComponent?.showGutterIconsStatus != false

        // Apply project settings if we have a project
        if (project != null) {
            val projectSettings = AlpineProjectSettingsState.getInstance(project)
            projectSettings.enableAlpineAjax = mySettingsComponent?.enableAlpineAjaxStatus != false
        }
    }

    override fun reset() {
        val appSettings = AlpineSettingsState.instance
        mySettingsComponent?.showGutterIconsStatus = appSettings.showGutterIcons

        // Reset project settings if we have a project
        if (project != null) {
            val projectSettings = AlpineProjectSettingsState.getInstance(project)
            mySettingsComponent?.enableAlpineAjaxStatus = projectSettings.enableAlpineAjax
        }
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
