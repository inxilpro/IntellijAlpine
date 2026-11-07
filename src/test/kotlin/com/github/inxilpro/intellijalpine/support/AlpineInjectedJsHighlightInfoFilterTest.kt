package com.github.inxilpro.intellijalpine.support

import junit.framework.TestCase

class AlpineInjectedJsHighlightInfoFilterTest : TestCase() {
    private val assignmentMismatch =
        "Assigned expression type boolean is not assignable to type " +
            "(url?: string | URL, target?: string, features?: string) => WindowProxy | null"

    fun testSuppressesNotExportedOnlyForBareReferences() {
        assertTrue(suppress("Element is not exported", bare = true))
        assertFalse(suppress("Element is not exported", bare = false))
    }

    fun testSuppressesAssignmentMismatchOnlyForComponentProperties() {
        assertTrue(suppress(assignmentMismatch, componentProperty = true))
        assertFalse(suppress(assignmentMismatch, componentProperty = false))
    }

    fun testKeepsUnrelatedDescriptions() {
        listOf(
            "Unresolved variable or type foo",
            "Expression statement is not assignment or call",
            "Unused local variable item",
        ).forEach { description ->
            assertFalse(AlpineInjectedJsHighlightInfoFilter.isCandidate(description))
            assertFalse(suppress(description, bare = true, componentProperty = true))
        }
    }

    private fun suppress(description: String, bare: Boolean = false, componentProperty: Boolean = false) =
        AlpineInjectedJsHighlightInfoFilter.shouldSuppress(description, { bare }, { componentProperty })
}
