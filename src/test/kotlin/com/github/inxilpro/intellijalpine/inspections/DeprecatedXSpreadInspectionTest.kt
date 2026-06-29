package com.github.inxilpro.intellijalpine.inspections

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DeprecatedXSpreadInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(DeprecatedXSpreadInspection::class.java)
    }

    fun testXSpreadWarning() {
        myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><div <warning descr="x-spread is deprecated in Alpine.js v3. Use x-bind with an object instead.">x-spread</warning>="directives"></div></div>"""
        )
        myFixture.checkHighlighting()
    }

    fun testNoWarningForXBind() {
        myFixture.configureByText(
            "test.html",
            """<div x-data="{}"><div x-bind="directives"></div></div>"""
        )
        myFixture.checkHighlighting()
    }

    fun testNoWarningForOtherDirectives() {
        myFixture.configureByText(
            "test.html",
            """<div x-data="{ show: true }"><div x-show="show" x-text="'hello'"></div></div>"""
        )
        myFixture.checkHighlighting()
    }
}
