package com.github.inxilpro.intellijalpine.support

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AlpineBoundLabelHighlightInfoFilterTest : BasePlatformTestCase() {
    private val filter = AlpineBoundLabelHighlightInfoFilter()

    fun testSuppressesMissingLabelWhenAriaLabelIsBound() {
        for (attribute in AlpineBoundLabelHighlightInfoFilter.BOUND_LABEL_ATTRIBUTES) {
            val source = """<div x-data="{ name: 'Ada' }"><input type="checkbox" $attribute="'Select ' + name"></div>"""
            val file = myFixture.configureByText("test.html", source)

            assertFalse(
                "Missing-label warning should be filtered for $attribute",
                filter.accept(missingLabelAt(source.indexOf("<input")), file),
            )
        }
    }

    fun testKeepsMissingLabelWithoutBoundLabel() {
        val source = """<div x-data="{ name: 'Ada' }"><input type="checkbox" :value="name"></div>"""
        val file = myFixture.configureByText("test.html", source)

        assertTrue(
            "Missing-label warning should stay when no label is bound",
            filter.accept(missingLabelAt(source.indexOf("<input")), file),
        )
    }

    fun testKeepsOtherWarnings() {
        val source = """<div x-data><input type="checkbox" :aria-label="'Select'"></div>"""
        val file = myFixture.configureByText("test.html", source)
        val other = HighlightInfo.newHighlightInfo(HighlightInfoType.WARNING)
            .range(source.indexOf("<input"), source.indexOf("<input") + 1)
            .description("Some other warning")
            .createUnconditionally()

        assertTrue(filter.accept(other, file))
    }

    private fun missingLabelAt(offset: Int): HighlightInfo =
        HighlightInfo.newHighlightInfo(HighlightInfoType.WARNING)
            .range(offset + 1, offset + 2)
            .description("Missing associated label")
            .createUnconditionally()
}
