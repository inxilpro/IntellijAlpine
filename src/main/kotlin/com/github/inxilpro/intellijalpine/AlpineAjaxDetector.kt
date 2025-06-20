package com.github.inxilpro.intellijalpine

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.ConcurrentHashMap

object AlpineAjaxDetector {
    private val listeners = ConcurrentHashMap<String, MessageBusConnection>()

    fun checkAndAutoEnable(project: Project) {
        val projectSettings = AlpineProjectSettingsState.getInstance(project)
        
        // Only auto-enable if currently disabled
        if (!projectSettings.enableAlpineAjax) {
            if (performDetection(project)) {
                projectSettings.enableAlpineAjax = true
            }
        }
        
        // Set up listener for package.json changes if not already set up
        setupPackageJsonListener(project)
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
                    // Re-check and potentially auto-enable on package.json changes
                    checkAndAutoEnable(project)
                }
            }
        })
    }

    fun cleanup(project: Project) {
        val projectPath = project.basePath ?: project.name
        listeners.remove(projectPath)?.disconnect()
    }

    private fun performDetection(project: Project): Boolean {
        return hasAlpineAjaxInPackageJson(project) || hasAlpineAjaxInScriptTags(project) || hasAlpineAjaxCode(project)
    }

    private fun hasAlpineAjaxInPackageJson(project: Project): Boolean {
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
                
                hasAlpineAjaxDependency(dependencies) || hasAlpineAjaxDependency(devDependencies)
            } else {
                false
            }
        }
    }
    
    private fun hasAlpineAjaxDependency(dependencies: JsonObject?): Boolean {
        return dependencies?.findProperty("alpine-ajax") != null ||
               dependencies?.findProperty("@imacrayon/alpine-ajax") != null
    }

    private fun hasAlpineAjaxInScriptTags(project: Project): Boolean {
        val htmlFiles = mutableListOf<VirtualFile>()
        
        // Get files by extension
        val extensions = listOf("html", "htm", "php", "twig")
        for (extension in extensions) {
            htmlFiles.addAll(
                FilenameIndex.getAllFilesByExt(
                    project,
                    extension,
                    GlobalSearchScope.projectScope(project)
                )
            )
        }
        
        return htmlFiles.any { virtualFile ->
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            
            when {
                psiFile is XmlFile -> {
                    // Handle HTML and XML files
                    val scriptTags = PsiTreeUtil.collectElementsOfType(psiFile, XmlTag::class.java)
                        .filter { it.name.equals("script", ignoreCase = true) }
                    
                    scriptTags.any { scriptTag ->
                        val src = scriptTag.getAttributeValue("src")
                        if (src != null) {
                            // Check script src attributes
                            isAlpineAjaxScriptSrc(src)
                        } else {
                            // Check inline script content
                            hasAlpineAjaxPatterns(scriptTag.value.text)
                        }
                    }
                }
                virtualFile.name.endsWith(".blade.php") -> {
                    // Handle Blade files by checking their raw content
                    try {
                        val content = String(virtualFile.contentsToByteArray())
                        hasAlpineAjaxPatterns(content) || hasAlpineAjaxScriptTagsInContent(content)
                    } catch (_: Exception) {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun hasAlpineAjaxCode(project: Project): Boolean {
        val jsExtensions = listOf("js", "ts", "mjs", "jsx", "tsx")
        val jsFiles = mutableListOf<VirtualFile>()
        
        for (extension in jsExtensions) {
            jsFiles.addAll(
                FilenameIndex.getAllFilesByExt(
                    project,
                    extension,
                    GlobalSearchScope.projectScope(project)
                )
            )
        }
        
        return jsFiles.any { virtualFile ->
            try {
                val content = String(virtualFile.contentsToByteArray())
                hasAlpineAjaxPatterns(content)
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun isAlpineAjaxScriptSrc(src: String): Boolean {
        return src.contains("alpine-ajax", ignoreCase = true) ||
               src.contains("imacrayon/alpine-ajax", ignoreCase = true) ||
               src.contains("unpkg.com/@imacrayon/alpine-ajax", ignoreCase = true) ||
               src.contains("jsdelivr.net/npm/@imacrayon/alpine-ajax", ignoreCase = true) ||
               src.contains("unpkg.com/alpine-ajax", ignoreCase = true) ||
               src.contains("jsdelivr.net/npm/alpine-ajax", ignoreCase = true)
    }

    private fun hasAlpineAjaxScriptTagsInContent(content: String): Boolean {
        // Use regex to find script tags with alpine-ajax references in raw HTML content
        val scriptTagRegex = Regex("<script[^>]*src=['\"]([^'\"]*)['\"][^>]*>", RegexOption.IGNORE_CASE)
        
        return scriptTagRegex.findAll(content).any { match ->
            val src = match.groupValues[1]
            isAlpineAjaxScriptSrc(src)
        }
    }

    private fun hasAlpineAjaxPatterns(content: String): Boolean {
        val alpineAjaxPatterns = listOf(
            // Import statements
            "import.*alpine-ajax",
            "from.*alpine-ajax",
            "require.*alpine-ajax",
            
            // Alpine.js plugin registration
            "Alpine\\.plugin\\s*\\(.*ajax",
            "alpine\\.plugin\\s*\\(.*ajax",
            
            // Ajax-specific functions from the alpine-ajax source
            "AjaxInterceptor",
            "AjaxCommand",
            "ajaxCommand",
            "processAjaxResponse",
            "handleAjaxRequest",
            
            // Magic helper usage patterns
            "\\\$ajax\\s*\\(",
            "x-target",
            "x-swap",
            "x-headers",
            "x-replace"
        )
        
        return alpineAjaxPatterns.any { pattern ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(content)
        }
    }

    fun getAlpineAjaxMagics(): String {
        return """
            /**
             * @param {string} action
             * @param {Object} options
             * @return {Promise<Response>}
             */
            function ${'$'}ajax(action, options = {}) {}
            
        """.trimIndent()
    }
}