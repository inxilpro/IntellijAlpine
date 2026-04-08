package com.github.inxilpro.intellijalpine.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AlpineCompletionContributorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/completion"

    fun testAlpineDirectiveSuggestionsInHtml() {
        myFixture.configureByText(
            "test.html",
            """<div x-<caret>></div>"""
        )
        val lookups = myFixture.completeBasic()
        assertNotNull("Completion should return results", lookups)
        val lookupStrings = lookups.map { it.lookupString }

        assertContainsElements(lookupStrings, "x-data", "x-show", "x-model", "x-text", "x-html")
        assertContainsElements(lookupStrings, "x-init", "x-ref", "x-cloak", "x-effect")
        assertContainsElements(lookupStrings, "x-bind", "x-on", "x-transition")
    }

    fun testEventShorthandSuggestions() {
        myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><button @cl<caret>></button></div>"""
        )
        val lookups = myFixture.completeBasic()
        assertNotNull("Completion should return results", lookups)
        val lookupStrings = lookups.map { it.lookupString }

        assertTrue(
            "Should contain click-related event suggestions",
            lookupStrings.any { it.contains("click") }
        )
    }

    fun testBindShorthandSuggestions() {
        myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><input :cl<caret>></div>"""
        )
        val lookups = myFixture.completeBasic()
        assertNotNull("Completion should return results", lookups)
        val lookupStrings = lookups.map { it.lookupString }

        assertTrue(
            "Should contain class binding suggestion",
            lookupStrings.any { it.contains("class") }
        )
    }

    fun testModelModifierSuggestions() {
        myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><input x-model.<caret>></div>"""
        )
        val lookups = myFixture.completeBasic()
        assertNotNull("Completion should return results", lookups)
        val lookupStrings = lookups.map { it.lookupString }

        assertContainsElements(lookupStrings, "x-model.lazy", "x-model.number")
    }

    fun testTemplateDirectivesOnlyOnTemplate() {
        myFixture.configureByText(
            "test.html",
            """<div x-<caret>></div>"""
        )
        val lookups = myFixture.completeBasic()
        assertNotNull("Completion should return results", lookups)
        val lookupStrings = lookups.map { it.lookupString }

        assertDoesntContain(lookupStrings, "x-if", "x-for", "x-teleport")
    }

    fun testTemplateDirectivesOnTemplateTag() {
        myFixture.configureByText(
            "test.html",
            """<template x-<caret>></template>"""
        )
        val lookups = myFixture.completeBasic()
        assertNotNull("Completion should return results", lookups)
        val lookupStrings = lookups.map { it.lookupString }

        assertContainsElements(lookupStrings, "x-if", "x-for", "x-teleport")
    }

    fun testNoAlpineDirectivesForNonAlpinePrefix() {
        myFixture.configureByText(
            "test.html",
            """<div cl<caret>></div>"""
        )
        val lookups = myFixture.completeBasic()
        if (lookups != null) {
            val lookupStrings = lookups.map { it.lookupString }
            assertDoesntContain(lookupStrings, "x-show", "x-model", "x-text")
        }
    }
}
