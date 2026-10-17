package com.vscodemobile.ui.terminal

import androidx.compose.runtime.State

/**
 * Sessão de terminal (modo simples por pipes ou PTY de verdade).
 * A UI trata os dois do mesmo jeito; cada um renderiza à sua maneira.
 */
interface TermSession {
    val label: String
    val running: State<Boolean>
    fun start()
    fun stop()
    fun restart()
    fun clear()
}
