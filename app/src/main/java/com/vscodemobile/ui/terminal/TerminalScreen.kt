package com.vscodemobile.ui.terminal

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.flow.first

/**
 * Estado lido de fora pela [TerminalViewClient]: modificadores do teclado
 * físico e a própria view (o client é criado antes da view existir).
 */
private class TermMods {
    var ctrl = false
    var alt = false
    var shift = false
    var fn = false
    var view: View? = null

    fun sync(e: KeyEvent) {
        ctrl = e.isCtrlPressed || e.metaState and KeyEvent.META_CTRL_ON != 0
        alt = e.isAltPressed || e.metaState and KeyEvent.META_ALT_ON != 0
        shift = e.isShiftPressed || e.metaState and KeyEvent.META_SHIFT_ON != 0
        fn = e.metaState and KeyEvent.META_FUNCTION_ON != 0
    }
}

/**
 * Tela do terminal com PTY de verdade: o teclado digita direto na grade,
 * vim/prompts/Ctrl+C/setas funcionam. Barra de teclas extra embaixo.
 */
@Composable
fun TerminalScreen(
    pty: PtySession,
    fg: Color,
    fontSp: Int,
    modifier: Modifier = Modifier,
    contentHeight: Dp = 330.dp,
    fillHeight: Boolean = false,
    bg: Color = Color.Black,
    accent: Color = Color(0xFF0078D4)
) {
    val context = LocalContext.current
    var view by remember { mutableStateOf<TerminalView?>(null) }
    var ctrlLatched by remember { mutableStateOf(false) }
    var attachError by remember { mutableStateOf<String?>(null) }
    val mods = remember { TermMods() }

    val client = remember {
        object : TerminalViewClient {
            override fun onScale(scale: Float): Float = 1.0f
            override fun onSingleTapUp(e: MotionEvent) {
                // Toque no terminal = abrir o teclado e focar (como no desktop)
                mods.view?.apply {
                    requestFocus()
                    showKeyboard()
                }
            }
            override fun shouldBackButtonBeMappedToEscape(): Boolean = true
            override fun shouldEnforceCharBasedInput(): Boolean = false
            override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
            override fun isTerminalViewSelected(): Boolean = true
            override fun copyModeChanged(copyMode: Boolean) {}
            override fun onKeyDown(keyCode: Int, e: KeyEvent, session: com.termux.terminal.TerminalSession): Boolean {
                mods.sync(e)
                return false
            }
            override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean {
                mods.sync(e)
                return false
            }
            override fun onLongPress(event: MotionEvent): Boolean = false
            override fun readControlKey(): Boolean = mods.ctrl
            override fun readAltKey(): Boolean = mods.alt
            override fun readShiftKey(): Boolean = mods.shift
            override fun readFnKey(): Boolean = mods.fn
            override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: com.termux.terminal.TerminalSession): Boolean = false
            override fun onEmulatorSet() {}
            override fun logError(tag: String, message: String) { Log.e(tag, message) }
            override fun logWarn(tag: String, message: String) { Log.w(tag, message) }
            override fun logInfo(tag: String, message: String) { Log.i(tag, message) }
            override fun logDebug(tag: String, message: String) { Log.d(tag, message) }
            override fun logVerbose(tag: String, message: String) { Log.v(tag, message) }
            override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) { Log.e(tag, message, e) }
            override fun logStackTrace(tag: String, e: Exception) { Log.e(tag, "pty view", e) }
        }
    }

    Column(
        modifier.fillMaxWidth()
            .then(if (fillHeight) Modifier else Modifier.height(contentHeight))
    ) {
        if (!pty.started.value) {
            Text(
                "iniciando pty…",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = fg.copy(alpha = 0.6f),
                modifier = Modifier.padding(12.dp)
            )
        }
        AndroidView(
            factory = { ctx ->
                TerminalView(ctx, null).apply {
                    setTerminalViewClient(client)
                    setTextSize(fontSp)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    mods.view = this
                    view = this
                }
            },
            update = { v ->
                v.setTextSize(fontSp)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(bg)
        )
        if (attachError != null) {
            Text(
                attachError!!,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = fg.copy(alpha = 0.7f),
                modifier = Modifier.padding(12.dp)
            )
        }
        LaunchedEffect(view, pty) {
            val v = view ?: return@LaunchedEffect
            try {
                // Espera layout real: grade 0x0 quebra o forkpty
                androidx.compose.runtime.snapshotFlow { v.width to v.height }
                    .first { (w, h) -> w > 0 && h > 0 }
                v.attachSession(pty.create())
                pty.session?.let { applyPalette(it, fg, bg, accent) }
                // A PTY notifica por onTextChanged; sem invalidate() aqui a
                // view nunca repinta o texto novo.
                pty.onScreenUpdate = { v.invalidate() }
                v.requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(v, 0)
            } catch (e: Exception) {
                Log.e("VsCodeMobile", "pty attach", e)
            } catch (e: UnsatisfiedLinkError) {
                attachError = "PTY indisponível (libtermux.so): $e"
                Log.e("VsCodeMobile", "pty lib", e)
            }
        }
        // Solta a referência à view (ela segura a TerminalSession anexada) e
        // o callback de repaint, que senão continua apontando para uma view
        // destruída e vaza a Activity.
        // A sessão em si continua viva em background, como no VS Code.
        DisposableEffect(view, pty) {
            onDispose {
                mods.view = null
                if (pty.onScreenUpdate != null) pty.onScreenUpdate = null
            }
        }
        // Barra de teclas: escreve bytes de controle direto no pty
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF181818))
                .horizontalScroll(rememberScrollState())
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PtyKey("Esc", fg, false, { pty.write(byteArrayOf(0x1B)) })
            PtyKey("Tab", fg, false, { pty.write(byteArrayOf(0x09)) })
            PtyKey("Ctrl", fg, ctrlLatched, { ctrlLatched = !ctrlLatched })
            PtyKey("←", fg, false, { pty.write("\u001B[D") })
            PtyKey("↑", fg, false, { pty.write("\u001B[A") })
            PtyKey("↓", fg, false, { pty.write("\u001B[B") })
            PtyKey("→", fg, false, { pty.write("\u001B[C") })
            PtyLetterKeys(fg, ctrlLatched, onUsed = { ctrlLatched = false }) { code ->
                pty.write(byteArrayOf(code.toByte()))
            }
        }
    }
}

@Composable
private fun PtyKey(label: String, fg: Color, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 52.dp, height = 48.dp)
            .clickable { onClick() }
            .background(if (highlighted) Color(0xFF0078D4).copy(alpha = 0.3f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = fg)
    }
}

@Composable
private fun PtyLetterKeys(
    fg: Color,
    ctrlLatched: Boolean,
    onUsed: () -> Unit,
    send: (Int) -> Unit
) {
    if (!ctrlLatched) return
    Row {
        for (c in listOf("C", "D", "Z")) {
            Box(
                Modifier
                    .size(width = 52.dp, height = 48.dp)
                    .clickable {
                        val code = when (c) {
                            "C" -> 0x03
                            "D" -> 0x04
                            else -> 0x1A
                        }
                        send(code)
                        onUsed()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(c, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = fg)
            }
        }
        Box(
            Modifier
                .height(48.dp)
                .clickable { onUsed() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("×", fontSize = 14.sp, color = fg.copy(alpha = 0.6f))
        }
    }
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, 0)
}

/** Aplica o tema do app na paleta do emulador (fg/bg/cursor + 16 cores ANSI). */
fun applyPalette(session: TerminalSession, fg: Color, bg: Color, accent: Color) {
    try {
        val pal = session.emulator?.mColors?.mCurrentColors ?: return
        pal[TextStyle.COLOR_INDEX_FOREGROUND] = fg.toArgb()
        pal[TextStyle.COLOR_INDEX_BACKGROUND] = bg.toArgb()
        pal[TextStyle.COLOR_INDEX_CURSOR] = accent.toArgb()
        val ansi = intArrayOf(
            0xFF000000.toInt(), 0xFFCD0000.toInt(), 0xFF00CD00.toInt(), 0xFFCDCD00.toInt(),
            0xFF0000EE.toInt(), 0xFFCD00CD.toInt(), 0xFF00CDCD.toInt(), 0xFFE5E5E5.toInt(),
            0xFF7F7F7F.toInt(), 0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFFFFFF00.toInt(),
            0xFF5C5CFF.toInt(), 0xFFFF00FF.toInt(), 0xFF00FFFF.toInt(), 0xFFFFFFFF.toInt()
        )
        for (i in 0..15) pal[i] = ansi[i]
    } catch (e: Exception) {
        Log.w("VsCodeMobile", "pty palette", e)
    }
}
