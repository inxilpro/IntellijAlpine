package com.github.inxilpro.intellijalpine.core

import com.github.inxilpro.intellijalpine.completion.AlpinePluginCompletionProvider
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement

data class CompletionProviderRegistration(
    val pattern: ElementPattern<out PsiElement>,
    val provider: AlpinePluginCompletionProvider,
    val type: CompletionType = CompletionType.BASIC,
)