package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.plugins.AlpineAjaxPlugin
import com.github.inxilpro.intellijalpine.plugins.AlpineWizardPlugin
import com.github.inxilpro.intellijalpine.plugins.TooltipPlugin
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PackageJsonDetectorTest : BasePlatformTestCase() {
    private val detector = PackageJsonDetector()

    private fun createPackageJson(content: String) {
        runWriteAction {
            val sourceRoot = myFixture.tempDirFixture.getFile("")!!
            val file = sourceRoot.findChild("package.json")?.also {
                VfsUtil.saveText(it, content)
            } ?: sourceRoot.createChildData(this, "package.json").also {
                VfsUtil.saveText(it, content)
            }
            file
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    fun testDetectsAlpineAjaxInDependencies() {
        createPackageJson("""{"name":"test","dependencies":{"alpine-ajax":"^0.8.0"}}""")
        assertTrue(detector.detect(project, AlpineAjaxPlugin()))
    }

    fun testDetectsAlpineWizardInDevDependencies() {
        createPackageJson("""{"name":"test","devDependencies":{"@glhd/alpine-wizard":"^1.0.0"}}""")
        assertTrue(detector.detect(project, AlpineWizardPlugin()))
    }

    fun testDetectsAlpineTooltipInDependencies() {
        createPackageJson("""{"name":"test","dependencies":{"@ryangjchandler/alpine-tooltip":"^2.0.0"}}""")
        assertTrue(detector.detect(project, TooltipPlugin()))
    }

    fun testDoesNotDetectMissingPlugin() {
        createPackageJson("""{"name":"test","dependencies":{"react":"^18.0.0"}}""")
        assertFalse(detector.detect(project, AlpineAjaxPlugin()))
        assertFalse(detector.detect(project, AlpineWizardPlugin()))
        assertFalse(detector.detect(project, TooltipPlugin()))
    }

    fun testDoesNotDetectWrongPlugin() {
        createPackageJson("""{"name":"test","dependencies":{"alpine-ajax":"^0.8.0"}}""")
        assertFalse(detector.detect(project, AlpineWizardPlugin()))
        assertFalse(detector.detect(project, TooltipPlugin()))
    }
}
