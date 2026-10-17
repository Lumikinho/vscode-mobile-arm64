package com.vscodemobile.ui.state

import android.net.Uri
import android.util.Log
import com.vscodemobile.lsp.JsonRpcConnection
import com.vscodemobile.lsp.LspClient
import com.vscodemobile.lsp.toProblemItem
import com.vscodemobile.ui.components.ProblemItem
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dono das conexões LSP por linguagem (sobrevive à composição).
 * Servidores rodam onde há runtime (ex.: Termux) e falam via socket localhost.
 */
class LanguageServerManager(private val workspaceRoot: File) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clients = mutableMapOf<String, LspClient>()
    private val versions = mutableMapOf<String, Int>()
    private val names = mutableMapOf<String, String>()
    private val uriToId = mutableMapOf<String, String>()
    private val changeJobs = mutableMapOf<String, Job>()
    private val lastFail = mutableMapOf<String, Long>()

    private val _diagnostics = MutableStateFlow<Map<String, List<ProblemItem>>>(emptyMap())
    val diagnostics: StateFlow<Map<String, List<ProblemItem>>> = _diagnostics.asStateFlow()

    /** Chamado na UI quando o servidor não responde (ex.: mostrar Snackbar). */
    var onConnectionFailed: ((language: String) -> Unit)? = null

    fun languageOf(ref: DocRef): String? = when (
        ref.name.substringAfterLast('.', "").lowercase()
    ) {
        "js", "jsx", "mjs", "cjs" -> "javascript"
        else -> null
    }

    fun docUri(ref: DocRef): String = when (ref) {
        is DocRef.Local -> Uri.fromFile(ref.file).toString()
        is DocRef.Saf -> ref.uri.toString()
    }

    private fun portFor(language: String): Int = when (language) {
        "javascript" -> 2087
        else -> 2087
    }

    fun noteOpen(ref: DocRef, text: String) {
        val lang = languageOf(ref) ?: return
        scope.launch {
            val client = ensureStarted(lang) ?: return@launch
            val id = ref.id
            val uri = docUri(ref)
            uriToId[uri] = id
            names[id] = ref.name
            versions[id] = 1
            update(id, emptyList())
            try {
                client.didOpen(uri, lang, text)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun noteChange(ref: DocRef, text: String) {
        val lang = languageOf(ref) ?: return
        val id = ref.id
        changeJobs[id]?.cancel()
        changeJobs[id] = scope.launch {
            delay(500)
            val client = clients[lang] ?: return@launch
            val v = (versions[id] ?: 1) + 1
            versions[id] = v
            try {
                client.didChange(docUri(ref), text, v)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun noteClose(ref: DocRef) {
        changeJobs.remove(ref.id)?.cancel()
        update(ref.id, emptyList())
        versions.remove(ref.id)
        scope.launch {
            try {
                clients.values.forEach { it.didClose(docUri(ref)) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Encerra definitivo: cancela o scope (o reader fica bloqueado em
     * `input.read()` esperando o socket) e solta os clientes. Sem isto o
     * pool do Dispatchers.IO e os sockets sobrevivem à Activity.
     *
     * Substitui um `stopAll()` que fazia shutdown/exit dentro do scope — a
     * corrotina nunca chegava ao servidor porque close() cancelava o scope
     * logo em seguida. Fechar o socket já sinaliza o fim pro language server.
     */
    fun close() {
        scope.cancel()
        clients.values.forEach {
            runCatching { it.close() }
        }
        clients.clear()
    }

    private suspend fun ensureStarted(language: String): LspClient? {
        clients[language]?.let { return it }
        val now = System.currentTimeMillis()
        if (now - (lastFail[language] ?: 0) < 60_000) return null
        return try {
            val socket = withContext(Dispatchers.IO) {
                Socket().apply {
                    connect(InetSocketAddress("127.0.0.1", portFor(language)), 5000)
                }
            }
            val rpc = JsonRpcConnection(socket)
            val client = LspClient(rpc, scope)
            client.onDiagnostics = { uri, diags ->
                uriToId[uri]?.let { id ->
                    val name = names[id] ?: ""
                    update(id, diags.map { it.toProblemItem(name) })
                }
            }
            client.start()
            val rootUri = Uri.fromFile(workspaceRoot).toString()
            client.initialize(rootUri)
            client.initialized()
            clients[language] = client
            Log.d("VsCodeMobile", "lsp conectado: $language")
            client
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("VsCodeMobile", "lsp sem servidor ($language)", e)
            lastFail[language] = now
            onConnectionFailed?.invoke(language)
            null
        }
    }

    private fun update(id: String, items: List<ProblemItem>) {
        _diagnostics.value = _diagnostics.value + (id to items)
    }
}
