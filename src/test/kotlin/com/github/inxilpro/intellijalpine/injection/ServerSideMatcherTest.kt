package com.github.inxilpro.intellijalpine.injection

import com.github.inxilpro.intellijalpine.injection.AlpineJavaScriptAttributeValueInjector.Companion.SERVER_VALUE_PLACEHOLDER
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerSideMatcherTest {

    private val php = AlpineJavaScriptAttributeValueInjector.phpMatcher

    private fun ftl(input: String): List<String> = FreeMarkerScanner.tokens(input).map { input.substring(it) }

    @Test
    fun `finds interpolations`() {
        assertEquals(listOf("\${user.id}"), ftl("id: \${user.id},"))
        assertEquals(listOf("\${(list?size > 0)?then(list?first, 0)}"), ftl("x: \${(list?size > 0)?then(list?first, 0)}"))
        assertEquals(listOf("\${prefs!{}}"), ftl("prefs: \${prefs!{}},"))
        assertEquals(listOf("\${a}", "\${b}"), ftl("\${a}\${b}"))
        assertEquals(listOf("\${label!\"}\"}"), ftl("\${label!\"}\"}"))
    }

    @Test
    fun `finds square and angle bracket tags`() {
        assertEquals(listOf("[#if ready]", "[/#if]", "[@widget size=1 /]"), ftl("[#if ready]a[/#if][@widget size=1 /]"))
        assertEquals(listOf("<#if ready>", "</#if>", "<@widget />"), ftl("<#if ready>a</#if><@widget />"))
    }

    @Test
    fun `tag arguments may nest brackets, parentheses and quoted delimiters`() {
        assertEquals(listOf("[@json value=items[indexes[0]] /]"), ftl("f([@json value=items[indexes[0]] /])"))
        assertEquals(listOf("<#if (count > 1)>", "</#if>"), ftl("<#if (count > 1)>many</#if>"))
        assertEquals(listOf("<#if (size(list) > 0)>"), ftl("<#if (size(list) > 0)>x"))
        assertEquals(listOf("[#if label == \"a]b\"]"), ftl("[#if label == \"a]b\"]x"))
        assertEquals(listOf("[#if label == 'it\\'s]']"), ftl("[#if label == 'it\\'s]']x"))
    }

    @Test
    fun `brackets without a directive name are not tags`() {
        assertEquals(emptyList<String>(), ftl("'bg-[#343434] text-[#000]'"))
        assertEquals(emptyList<String>(), ftl("{ open: false, count: items.length, first: items[0] }"))
    }

    @Test
    fun `unterminated tags are not tokens and long input does not overflow the stack`() {
        assertEquals(emptyList<String>(), ftl("[#if open && "))
        val long = "[#if " + "a".repeat(20_000)
        assertEquals(emptyList<String>(), ftl(long))
        assertEquals(listOf("[#if ${"a".repeat(20_000)}]"), ftl("[#if ${"a".repeat(20_000)}]"))
    }

    @Test
    fun `php matcher matches blade echoes`() {
        assertEquals(listOf("{{ \$user->id }}"), php.findAll("id: {{ \$user->id }},").map { it.value }.toList())
    }

    @Test
    fun `each token gets its own stand-in`() {
        val replace = { text: String -> AlpineJavaScriptAttributeValueInjector.replacementFor(text, FreeMarkerScanner::tokens) }
        assertEquals(SERVER_VALUE_PLACEHOLDER, replace("\${amount}"))
        assertEquals("", replace("[#if ok]"))
        assertEquals(SERVER_VALUE_PLACEHOLDER, replace("[#if ok]\${amount}"))
        assertEquals(SERVER_VALUE_PLACEHOLDER, replace("[@json value=items[0] /]"))
        assertEquals("$", replace("\${'$'}"))
        assertEquals(SERVER_VALUE_PLACEHOLDER, AlpineJavaScriptAttributeValueInjector.replacementFor("<%= user.name %>", null))
    }

    @Test
    fun `else arms are masked through the matching close tag`() {
        val content = "a([#if x]1[#else]2[/#if], [#if y][#if z]3[/#if][#elseif w]4[/#if])"
        val intervals = AlpineJavaScriptAttributeValueInjector.alternativeBranchIntervals(content)
        assertEquals(listOf("[#else]2[/#if]", "[#elseif w]4[/#if]"), intervals.map { content.substring(it) })
    }
}
