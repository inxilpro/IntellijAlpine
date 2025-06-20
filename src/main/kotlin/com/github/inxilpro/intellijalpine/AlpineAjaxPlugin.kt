package com.github.inxilpro.intellijalpine

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.apache.commons.lang3.tuple.MutablePair

class AlpineAjaxPlugin : AlpinePlugin {
    
    override fun getPackageDisplayName(): String = "alpine-ajax"
    
    override fun getPackageNamesForDetection(): List<String> = listOf(
        "alpine-ajax",
        "@imacrayon/alpine-ajax"
    )
    
    override fun getTypeText(info: AttributeInfo): String? {
        if ("x-target:" == info.prefix) {
            return "DOM node to inject response into"
        }

        return when (info.attribute) {
            "x-target" -> "DOM node to inject response into"
            "x-headers" -> "Set AJAX request headers"
            "x-merge" -> "Merge response data with existing data"
            "x-autofocus" -> "Auto-focus on AJAX response"
            "x-sync" -> "Always sync on AJAX response"
            else -> null
        }
    }
    
    override fun injectJsContext(context: MutablePair<String, String>): MutablePair<String, String> {
        val magics = """
            /**
             * @param {string} action
             * @param {Object} options
             * @return {Promise<Response>}
             */
            function ${'$'}ajax(action, options = {}) {}
            
        """.trimIndent()
        
        return MutablePair(context.left + magics, context.right)
    }
    
    override fun getDirectives(): List<String> = listOf(
        "x-target",
        "x-headers", 
        "x-merge",
        "x-autofocus",
        "x-sync"
    )
    
    override fun isEnabled(project: Project): Boolean {
        return AlpineProjectSettingsState.getInstance(project).enableAlpineAjax
    }
    
    override fun enable(project: Project) {
        AlpineProjectSettingsState.getInstance(project).enableAlpineAjax = true
    }
    
    override fun disable(project: Project) {
        AlpineProjectSettingsState.getInstance(project).enableAlpineAjax = false
    }
    
    override fun performDetection(project: Project): Boolean {
        return hasAlpineAjaxInPackageJson(project) || 
               hasAlpineAjaxInScriptTags(project) || 
               hasAlpineAjaxCode(project)
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
        return getPackageNamesForDetection().any { packageName ->
            dependencies?.findProperty(packageName) != null
        }
    }
    
    private fun hasAlpineAjaxInScriptTags(project: Project): Boolean {
        val htmlFiles = mutableListOf<com.intellij.openapi.vfs.VirtualFile>()
        
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
                            isAlpineAjaxScriptSrc(src)
                        } else {
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
    
    private fun isAlpineAjaxScriptSrc(src: String): Boolean {
        return src.contains("alpine-ajax", ignoreCase = true)
    }
    
    private fun hasAlpineAjaxScriptTagsInContent(content: String): Boolean {
        // Use regex to find script tags with alpine-ajax references in raw HTML content
        val scriptTagRegex = Regex("<script[^>]*src=['\"]([^'\"]*)['\"][^>]*>", RegexOption.IGNORE_CASE)
        
        return scriptTagRegex.findAll(content).any { match ->
            val src = match.groupValues[1]
            isAlpineAjaxScriptSrc(src)
        }
    }
    
    private fun hasAlpineAjaxCode(project: Project): Boolean {
        val jsExtensions = listOf("js", "ts", "mjs", "jsx", "tsx")
        val jsFiles = mutableListOf<com.intellij.openapi.vfs.VirtualFile>()
        
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
}