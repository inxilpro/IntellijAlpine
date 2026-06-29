package com.github.inxilpro.intellijalpine.core.detection

import com.github.inxilpro.intellijalpine.plugins.AlpineAjaxPlugin
import com.github.inxilpro.intellijalpine.plugins.AlpineWizardPlugin
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ScriptReferenceDetectorTest : BasePlatformTestCase() {
    private val detector = ScriptReferenceDetector()

    override fun getTestDataPath(): String = "src/test/testData/detection"

    fun testDetectsAlpineAjaxScriptTag() {
        myFixture.copyFileToProject("html_with_alpine_script.html", "index.html")
        assertTrue(detector.detect(project, AlpineAjaxPlugin()))
    }

    fun testDoesNotDetectMissingScriptReference() {
        myFixture.copyFileToProject("html_without_alpine.html", "index.html")
        assertFalse(detector.detect(project, AlpineAjaxPlugin()))
    }

    fun testDoesNotDetectWrongPluginFromScriptTag() {
        myFixture.copyFileToProject("html_with_alpine_script.html", "index.html")
        assertFalse(detector.detect(project, AlpineWizardPlugin()))
    }

    fun testDetectsImportStatements() {
        myFixture.addFileToProject(
            "app.js",
            "import Ajax from 'alpine-ajax';"
        )
        assertTrue(detector.detect(project, AlpineAjaxPlugin()))
    }

    fun testDetectsRequireStatements() {
        myFixture.addFileToProject(
            "app.js",
            "const wizard = require('@glhd/alpine-wizard');"
        )
        assertTrue(detector.detect(project, AlpineWizardPlugin()))
    }

    fun testDoesNotDetectUnrelatedImports() {
        myFixture.addFileToProject(
            "app.js",
            "import React from 'react';"
        )
        assertFalse(detector.detect(project, AlpineAjaxPlugin()))
    }
}
