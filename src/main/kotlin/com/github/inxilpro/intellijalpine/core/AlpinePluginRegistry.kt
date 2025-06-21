package com.github.inxilpro.intellijalpine.core

import com.github.inxilpro.intellijalpine.attributes.AttributeInfo
import com.github.inxilpro.intellijalpine.completion.AutoCompleteSuggestions
import com.github.inxilpro.intellijalpine.settings.AlpineProjectSettingsState
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.messages.MessageBusConnection
import org.apache.commons.lang3.tuple.MutablePair
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class AlpinePluginRegistry {
    private val listeners = ConcurrentHashMap<String, MessageBusConnection>()

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

    fun checkAndAutoEnablePlugins(project: Project) {
        getRegisteredPlugins().forEach { plugin ->
            if (!isPluginEnabled(project, plugin.getPluginName()) && isPluginDetected(project, plugin)) {
                enablePlugin(project, plugin.getPluginName())
            }
        }

        setupPackageJsonListener(project)
    }

    private fun isPluginDetected(project: Project, plugin: AlpinePlugin): Boolean {
        return hasPluginInPackageJson(project, plugin) || plugin.performDetection(project)
    }

    private fun hasPluginInPackageJson(project: Project, plugin: AlpinePlugin): Boolean {
        val packageJsonFiles = FilenameIndex.getVirtualFilesByName(
            "package.json",
            GlobalSearchScope.projectScope(project)
        )

        return packageJsonFiles.any { virtualFile ->
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            if (psiFile is JsonFile) {
                val rootObject = psiFile.topLevelValue as? JsonObject
                val dependencies = rootObject?.findProperty("dependencies")?.value as? JsonObject
                val devDependencies = rootObject?.findProperty("devDependencies")?.value as? JsonObject

                hasPluginDependency(plugin, dependencies) || hasPluginDependency(plugin, devDependencies)
            } else {
                false
            }
        }
    }

    private fun hasPluginDependency(plugin: AlpinePlugin, dependencies: JsonObject?): Boolean {
        return plugin.getPackageNamesForDetection().any { packageName ->
            dependencies?.findProperty(packageName) != null
        }
    }

    private fun setupPackageJsonListener(project: Project) {
        val projectPath = project.basePath ?: project.name

        // Don't set up listener if already exists
        if (listeners.containsKey(projectPath)) {
            return
        }

        val connection = project.messageBus.connect()
        listeners[projectPath] = connection

        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val hasPackageJsonChanges = events.any { event ->
                    val file = event.file
                    file != null && file.name == "package.json"
                }

                if (hasPackageJsonChanges) {
                    ApplicationManager.getApplication().runReadAction {
                        getRegisteredPlugins().forEach { plugin ->
                            if (!isPluginEnabled(project, plugin.getPluginName()) && isPluginDetected(project, plugin)) {
                                enablePlugin(project, plugin.getPluginName())
                            }
                        }
                    }
                }
            }
        })
    }

    fun cleanup(project: Project) {
        val projectPath = project.basePath ?: project.name
        listeners.remove(projectPath)?.disconnect()
    }
}