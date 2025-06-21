package com.github.inxilpro.intellijalpine.completion

import com.github.inxilpro.intellijalpine.core.AlpinePlugin
import com.github.inxilpro.intellijalpine.core.AlpinePluginRegistry
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext

abstract class AlpinePluginCompletionProvider(
    private val plugin: AlpinePlugin
) : CompletionProvider<CompletionParameters>() {
    
    final override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        if (AlpinePluginRegistry.instance.isPluginEnabled(parameters.position.project, plugin)) {
            addPluginCompletions(parameters, context, result)
        }
    }

    protected abstract fun addPluginCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    )
}