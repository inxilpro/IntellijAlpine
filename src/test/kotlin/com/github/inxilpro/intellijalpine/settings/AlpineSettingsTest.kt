package com.github.inxilpro.intellijalpine.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AlpineSettingsTest : BasePlatformTestCase() {

    fun testAppSettingsPersistGutterIcons() {
        val settings = ApplicationManager.getApplication().getService(AlpineSettingsState::class.java)
            ?: return

        val original = settings.showGutterIcons

        settings.showGutterIcons = false
        assertFalse(settings.showGutterIcons)

        settings.showGutterIcons = true
        assertTrue(settings.showGutterIcons)

        settings.showGutterIcons = original
    }

    fun testProjectSettingsPluginEnableDisable() {
        val settings = project.getService(AlpineProjectSettingsState::class.java)
            ?: return

        assertFalse("Plugins should be disabled by default", settings.isPluginEnabled("alpine-ajax"))

        settings.setPluginEnabled("alpine-ajax", true)
        assertTrue("Plugin should be enabled after enabling", settings.isPluginEnabled("alpine-ajax"))

        settings.setPluginEnabled("alpine-ajax", false)
        assertFalse("Plugin should be disabled after disabling", settings.isPluginEnabled("alpine-ajax"))
    }

    fun testProjectSettingsMultiplePlugins() {
        val settings = project.getService(AlpineProjectSettingsState::class.java)
            ?: return

        settings.setPluginEnabled("alpine-ajax", true)
        settings.setPluginEnabled("alpine-wizard", true)
        settings.setPluginEnabled("alpine-tooltip", false)

        assertTrue(settings.isPluginEnabled("alpine-ajax"))
        assertTrue(settings.isPluginEnabled("alpine-wizard"))
        assertFalse(settings.isPluginEnabled("alpine-tooltip"))
    }

    fun testProjectSettingsStateRoundTrip() {
        val settings = AlpineProjectSettingsState()
        settings.setPluginEnabled("alpine-ajax", true)
        settings.setPluginEnabled("alpine-wizard", false)

        val state = settings.state
        assertNotNull("State should not be null", state)

        val newSettings = AlpineProjectSettingsState()
        newSettings.loadState(state!!)

        assertTrue(newSettings.isPluginEnabled("alpine-ajax"))
        assertFalse(newSettings.isPluginEnabled("alpine-wizard"))
    }

    fun testAppSettingsStateRoundTrip() {
        val original = AlpineSettingsState()
        original.showGutterIcons = false

        val state = original.state
        assertNotNull("State should not be null", state)

        val restored = AlpineSettingsState()
        restored.loadState(state!!)
        assertFalse(restored.showGutterIcons)
    }
}
