package com.github.inxilpro.intellijalpine.injection

/** Injection behavior shared by plain HTML and every template language. */
class AlpineInjectionTest : AlpineInjectionTestCase() {

    fun testPlainHtmlInjects() {
        cleanJs("""<div x-data="{ open: false }"><button @click="open = !open"></button></div>""", "@click")
    }

    fun testEmptyAttributeStillInjects() {
        // A zero-length injection gives completion a JavaScript context while the user types the value
        for (attribute in listOf("x-show", "x-init", "@click")) {
            val js = cleanJs("""<div x-data="{ open: false }"><div $attribute=""></div></div>""", attribute)
            assertTrue("$attribute should still see component state:\n${js.text}", js.text.contains("let \$data = { open: false };"))
        }
    }

    fun testXForParenDestructuringParses() {
        val js = cleanJs(
            """<ul x-data="{ colors: [] }"><template x-for="(color, index) in colors"><li x-text="color"></li></template></ul>""",
            "x-for",
        )
        assertTrue("Expected array destructuring in:\n${js.text}", js.text.contains("for (let [color, index] in colors"))
        assertTrue("The loop should read its variables so they are not reported unused:\n${js.text}", js.text.contains("void color;"))
    }

    fun testXForVariablesResolveInChildDirectives() {
        val html = """
            <ul x-data="{ colors: [] }">
                <template x-for="(color, index) in colors" :key="index">
                    <li x-text="color.name" :class="{ 'first': index === 0 }"></li>
                </template>
            </ul>
        """.trimIndent()

        for (attribute in listOf(":key", "x-text", ":class")) {
            val js = cleanJs(html, attribute)
            assertTrue("$attribute should declare the loop variables:\n${js.text}", js.text.contains("let color;") && js.text.contains("let index;"))
        }
    }

    fun testNestedLoopsReusingANameDeclareItOnce() {
        val js = cleanJs(
            """
            <div x-data="{ groups: [] }">
                <template x-for="item in groups">
                    <template x-for="item in item.children">
                        <span x-text="item.label"></span>
                    </template>
                </template>
            </div>
            """.trimIndent(),
            "x-text",
        )
        assertEquals("item should be declared once:\n${js.text}", 1, Regex("let item;").findAll(js.text).count())
    }

    fun testFunctionXDataIsInvoked() {
        val js = cleanJs("""<div x-data="dropdown"><button @click="toggle()"></button></div>""", "@click")
        assertTrue(
            "A function-valued x-data should be invoked as Alpine does:\n${js.text}",
            js.text.contains("let \$data = (typeof (dropdown) === 'function' ? (dropdown)() : (dropdown));"),
        )
    }

    fun testObjectLiteralXDataStaysInline() {
        val js = cleanJs("""<div x-data="{ open: false }" x-show="open"></div>""", "x-show")
        assertTrue(js.text.contains("let \$data = { open: false };"))
    }

    fun testBlankXDataIsAnEmptyComponent() {
        val js = cleanJs("""<div x-data=""><span x-text="'hi'"></span></div>""", "x-text")
        assertTrue(js.text.contains("let \$data = {};"))
    }

    fun testHyphenatedXRefUsesBracketAccess() {
        val js = cleanJs("""<div x-ref="upload-area"></div>""", "x-ref")
        assertTrue(js.text.contains("\$refs[\"upload-area\"] = \$el"))
    }
}
