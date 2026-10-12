package com.github.inxilpro.intellijalpine.core

import com.github.inxilpro.intellijalpine.core.detection.PluginDetector
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.PROJECT)
class AlpinePluginAutoDetector(private val project: Project, private val cs: CoroutineScope) {
    companion object {
        fun getInstance(project: Project): AlpinePluginAutoDetector = project.service()

        private val DEBOUNCE = 500.milliseconds
    }

    private val detector = PluginDetector()
    private val started = AtomicBoolean(false)

    // A single conflated request stream means bursts of package.json changes (npm install,
    // branch switches) collapse into one detection pass instead of piling up concurrent scans.
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @OptIn(FlowPreview::class)
    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }

        cs.launch {
            requests.debounce(DEBOUNCE).collectLatest { detectAndEnablePlugins() }
        }

        project.messageBus.connect(cs).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (events.any(::isProjectPackageJson)) {
                    schedule()
                }
            }
        })

        schedule()
    }

    fun schedule() {
        requests.tryEmit(Unit)
    }

    private suspend fun detectAndEnablePlugins() {
        val registry = AlpinePluginRegistry.instance

        registry.getRegisteredPlugins()
            .filterNot { registry.isPluginEnabled(project, it) }
            .forEach { plugin ->
                if (detector.detect(project, plugin)) {
                    registry.enablePlugin(project, plugin.getPluginName())
                }
            }
    }

    private fun isProjectPackageJson(event: VFileEvent): Boolean {
        val file = event.file ?: return false

        return file.name == "package.json"
            && file.isValid
            && !file.path.contains("/node_modules/")
            && ProjectFileIndex.getInstance(project).isInContent(file)
    }
}
