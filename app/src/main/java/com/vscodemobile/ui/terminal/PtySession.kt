package com.vscodemobile.ui.terminal

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

/**
 * Sessão com pseudoterminal de verdade (forkpty via libtermux.so JNI):
 * vim, prompts interativos, Ctrl+C, setas e tab-completion do shell funcionam.
 *
 * O processo nasce preguiçoso em [create]: a TerminalView chama updateSize,
 * que inicializa o emulador e o subprocesso.
 */
class PtySession(
    private val appContext: Context,
    val shellPath: String,
    val cwd: String,
    val args: Array<String>,
    val env: Array<String>,
    override val label: String
) : TermSession {

    private val main = Handler(Looper.getMainLooper())

    private val _running = mutableStateOf(false)
    override val running: State<Boolean> = _running

    @Volatile
    var session: TerminalSession? = null
        private set

    @Volatile
    var pid: Int = 0
        private set

    private val _generation = mutableStateOf(0)
    val generation: State<Int> = _generation

    private val _started = mutableStateOf(false)
    /** Processo já fez fork ao menos uma vez (para overlay "iniciando"). */
    val started: State<Boolean> = _started

    private val client = object : TerminalSessionClient {
        override fun onTextChanged(changedSession: TerminalSession) {}
        override fun onTitleChanged(changedSession: TerminalSession) {}
        override fun onSessionFinished(finishedSession: TerminalSession) {
            main.post { _running.value = false }
        }

        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
            try {
                val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("terminal", text))
            } catch (e: Exception) {
                Log.w("VsCodeMobile", "pty copy", e)
            }
        }

        override fun onPasteTextFromClipboard(session: TerminalSession?) {
            try {
                val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                val text = cm.primaryClip?.getItemAt(0)?.coerceToText(appContext)?.toString()
                    ?: return
                write(text)
            } catch (e: Exception) {
                Log.w("VsCodeMobile", "pty paste", e)
            }
        }

        override fun onBell(session: TerminalSession) {}
        override fun onColorsChanged(session: TerminalSession) {}
        override fun onTerminalCursorStateChange(state: Boolean) {}
        override fun setTerminalShellPid(session: TerminalSession, pid: Int) {
            this@PtySession.pid = pid
            main.post { _running.value = true }
        }

        override fun getTerminalCursorStyle(): Int? = null
        override fun logError(tag: String, message: String) { Log.e(tag, message) }
        override fun logWarn(tag: String, message: String) { Log.w(tag, message) }
        override fun logInfo(tag: String, message: String) { Log.i(tag, message) }
        override fun logDebug(tag: String, message: String) { Log.d(tag, message) }
        override fun logVerbose(tag: String, message: String) { Log.v(tag, message) }
        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
            Log.e(tag, message, e)
        }

        override fun logStackTrace(tag: String, e: Exception) {
            Log.e(tag, "pty", e)
        }
    }

    /** Cria (ou recria) a sessão. Chamar na thread principal. */
    fun create(): TerminalSession {
        session?.finishIfRunning()
        val s = TerminalSession(shellPath, cwd, args, env, 200, client)
        session = s
        _started.value = true
        _running.value = true
        return s
    }

    fun write(data: ByteArray, offset: Int = 0, count: Int = data.size) {
        try {
            session?.write(data, offset, count)
        } catch (e: Exception) {
            Log.w("VsCodeMobile", "pty write", e)
        }
    }

    fun write(text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        write(bytes, 0, bytes.size)
    }

    fun reset() {
        try {
            session?.reset()
        } catch (e: Exception) {
            Log.w("VsCodeMobile", "pty reset", e)
        }
    }

    override fun start() {
        // O processo nasce no primeiro updateSize da view. Só marca "rodando"
        // se a PTY já existe — senão o painel mentiria antes do attach.
        if (session != null) main.post { _running.value = true }
    }

    override fun stop() {
        try {
            session?.finishIfRunning()
            if (pid > 0) {
                try {
                    android.system.Os.kill(pid, 9)
                } catch (_: Exception) {
                }
                pid = 0
            }
        } catch (e: Exception) {
            Log.w("VsCodeMobile", "pty stop", e)
        } finally {
            main.post { _running.value = false }
        }
    }

    override fun restart() {
        stop()
        session = null
        pid = 0
        _generation.value++
        main.post { _running.value = true }
    }

    override fun clear() {
        reset()
    }}
