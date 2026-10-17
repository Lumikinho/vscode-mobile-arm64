package com.vscodemobile.lsp

import com.vscodemobile.ui.components.ProblemItem
import com.vscodemobile.ui.components.ProblemSeverity

/** Diagnóstico LSP -> item do painel Problems (linha 1-based). */
fun LspDiagnostic.toProblemItem(file: String): ProblemItem {
    val severity = if (severity == 1) ProblemSeverity.ERROR else ProblemSeverity.WARNING
    val msg = if (source.isNotBlank()) "$message [$source]" else message
    return ProblemItem(file, line + 1, severity, msg.take(160))
}
