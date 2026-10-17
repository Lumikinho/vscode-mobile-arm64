package com.vscodemobile.ui.plugins

import android.util.Log
import android.webkit.JavascriptInterface
import com.vscodemobile.ui.workbench.CommandRegistry

/**
 * Ponte entre o JavaScript do plugin e o app.
 *
 * Superficie deliberadamente estreita: executar comandos do workbench e ler o
 * documento ativo. NAO expoe escrita de arquivo nem execucao no PTY — um plugin
 * remoto com acesso a escrita seria quase tao perigoso quanto rodar codigo
 * desconhecido no Termux.
 *
 * Como [CommandRegistry] contem comandos que gravam (ex.: save), o chamador
 * decide o que Happens: ve [onDangerousCommand] para avisar o usuario.
 */
class PluginBridge(
    private val onExecuteCommand: (id: String, argsJson: String) -> Unit,
    private val onActiveDocument: () -> String,
    private val onShowMessage: (String) -> Unit,
    private val onLog: (String) -> Unit,
    /** Comandos que mexem fora do editor; o app pode pedir confirmacao. */
    private val isSensitive: (String) -> Boolean = { false }
) {

    @JavascriptInterface
    fun version(): String = "1.0.0"

    @JavascriptInterface
    fun commands(): String {
        val arr = org.json.JSONArray()
        for (c in CommandRegistry.paletteCommands()) arr.put(c.id)
        return arr.toString()
    }

    /**
     * Executa um comando do workbench. Devolve `{"ok":true}` ou
     * `{"ok":false,"error":...}` — nunca lanca para dentro do JS.
     */
    @JavascriptInterface
    fun execute(id: String, argsJson: String?): String = runCatching {
        require(id.isNotBlank()) { "id vazio" }
        if (!CommandRegistry.isRegistered(id)) {
            return@runCatching error("comando desconhecido: $id")
        }
        if (isSensitive(id)) {
            return@runCatching error("comando bloqueado para plugins: $id")
        }
        onExecuteCommand(id, argsJson ?: "null")
        ok()
    }.getOrElse { e ->
        Log.w("VsCodeMobile", "plugin execute $id", e)
        error(e.message ?: "falha")
    }

    /** Documento aberto agora: {uri,name,languageId,text,dirty}. */
    @JavascriptInterface
    fun activeDocument(): String = runCatching { onActiveDocument() }
        .getOrElse { error(it.message ?: "sem documento") }

    @JavascriptInterface
    fun showMessage(text: String) {
        onShowMessage(text.take(400))
    }

    @JavascriptInterface
    fun log(level: String, message: String) {
        val tag = "VsCodeMobile/plugin"
        when (level.lowercase()) {
            "error" -> Log.e(tag, message)
            "warn" -> Log.w(tag, message)
            "debug" -> Log.d(tag, message)
            else -> Log.i(tag, message)
        }
        onLog("${level.uppercase()}: $message")
    }

    private fun ok(): String = """{"ok":true}"""

    private fun error(msg: String): String =
        """{"ok":false,"error":${org.json.JSONObject.quote(msg)}}"""
}

/**
 * Comandos que o app NAO deixa um plugin remoto executar.
 *
 * [CommandRegistry] e um alvo honesto para um plugin de confianca, mas para
 * conteudo baixado da rede o risco muda de natureza. Estes alteram estado
 * fora do editor ou spawnam processos.
 */
object SensitiveCommands {
    private val DENIED = setOf(
        "workbench.linux.setupAlpine",
        "workbench.linux.openShell",
        "workbench.action.terminal.new",
        "workbench.action.files.openFolder",
        "workbench.action.terminal.openTermux",
        "workbench.action.debug.start"
    )

    fun isDenied(id: String): Boolean = id in DENIED
}