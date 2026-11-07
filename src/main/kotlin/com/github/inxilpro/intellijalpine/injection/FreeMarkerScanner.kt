package com.github.inxilpro.intellijalpine.injection

/**
 * Finds FreeMarker interpolations (`${...}`) and directive/macro tags (`[#if ...]`, `[/#if]`, `[@m /]`,
 * `<#if ...>`, `</#list>`, `<@m />`) in text, for templates the FreeMarker plugin has not parsed.
 *
 * Tag bodies may contain quoted strings (with escapes) and nested brackets, parentheses and braces, so
 * `[@m value=items[indexes[0]] /]` and `<#if (fn(x) > 0)>` are found whole. Scanning is iterative, so long
 * attribute values cannot exhaust the stack. As in FreeMarker itself, `[#`/`<#`/`[@`/`<@` only open a tag
 * when a name follows, so text like Tailwind's `bg-[#343434]` is left alone.
 */
internal object FreeMarkerScanner {

    fun tokens(text: String): List<IntRange> {
        val tokens = mutableListOf<IntRange>()
        var i = 0
        while (i < text.length) {
            val end = tokenEndAt(text, i)
            if (end >= 0) {
                tokens.add(i..end)
                i = end + 1
            } else {
                i++
            }
        }
        return tokens
    }

    /** Index of the last character of the token starting at [start], or -1 if none starts there. */
    private fun tokenEndAt(text: String, start: Int): Int {
        val c = text[start]
        return when {
            c == '$' && text.startsWith("{", start + 1) -> closingIndex(text, start + 1, '}')
            (c == '[' || c == '<') && opensTag(text, start) -> closingIndex(text, start, if (c == '[') ']' else '>')
            else -> -1
        }
    }

    private fun opensTag(text: String, start: Int): Boolean {
        var i = start + 1
        if (text.startsWith("/", i)) i++
        if (i >= text.length || (text[i] != '#' && text[i] != '@')) return false
        val name = text.getOrNull(i + 1) ?: return false
        return name.isLetter() || name == '_'
    }

    /**
     * Index of the delimiter closing the token whose opening delimiter is at [open], skipping quoted
     * strings and balanced (), [] and {} pairs; -1 when the token is unterminated.
     */
    private fun closingIndex(text: String, open: Int, close: Char): Int {
        val nesting = ArrayDeque<Char>()
        var i = open + 1
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' || c == '\'' -> {
                    i = stringEnd(text, i)
                    if (i < 0) return -1
                }
                nesting.isEmpty() && c == close -> return i
                c == '(' -> nesting.addLast(')')
                c == '[' -> nesting.addLast(']')
                c == '{' -> nesting.addLast('}')
                nesting.isNotEmpty() && c == nesting.last() -> nesting.removeLast()
            }
            i++
        }
        return -1
    }

    private fun stringEnd(text: String, quoteAt: Int): Int {
        val quote = text[quoteAt]
        var i = quoteAt + 1
        while (i < text.length) {
            when (text[i]) {
                '\\' -> i++
                quote -> return i
            }
            i++
        }
        return -1
    }
}
