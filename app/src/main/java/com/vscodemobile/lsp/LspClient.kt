package com.vscodemobile.lsp

import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject

/** Diagnóstico LSP (linhas/colunas 0-based, severity 1=Error..4=Hint). */
data class LspDiagnostic(
    val line: Int,
    val character: Int,
    val endLine: Int,
    val endCharacter: Int,
    val severity: Int,
    val message: String,
    val source: String
)

/**
 * Cliente LSP mínimo (subset 3.17): initialize, didOpen/didChange(full)/didClose,
 * shutdown/exit e publishDiagnostics. Sem pty: socket ou stdio servem.
 */
class LspClient(private val rpc: JsonRpcConnection, private val scope: CoroutineScope) {

    var onDiagnostics: ((uri: String, items: List<LspDiagnostic>) -> Unit)? = null

    fun start() {
        rpc.onNotification = { method, params ->
            if (method == "textDocument/publishDiagnostics" && params != null) {
                onDiagnostics?.invoke(
                    params.optString("uri", ""),
                    parseDiagnostics(params.optJSONArray("diagnostics"))
                )
            }
        }
        rpc.start(scope)
    }

    suspend fun initialize(rootUri: String?): JSONObject? {
        val params = JSONObject()
            .put("processId", JSONObject.NULL)
            .put("rootUri", rootUri ?: JSONObject.NULL)
            .put("capabilities", JSONObject())
            .put(
                "clientInfo",
                JSONObject().put("name", "VSCode Mobile").put("version", "1.0")
            )
        return rpc.request("initialize", params)
    }

    fun initialized() {
        rpc.notify("initialized", JSONObject())
    }

    fun didOpen(uri: String, languageId: String, text: String) {
        rpc.notify(
            "textDocument/didOpen",
            JSONObject().put(
                "textDocument",
                JSONObject()
                    .put("uri", uri)
                    .put("languageId", languageId)
                    .put("version", 1)
                    .put("text", text)
            )
        )
    }

    fun didChange(uri: String, text: String, version: Int) {
        val change = JSONObject().put("text", text)
        val changes = org.json.JSONArray().put(change)
        rpc.notify(
            "textDocument/didChange",
            JSONObject().put(
                "textDocument",
                JSONObject().put("uri", uri).put("version", version)
            ).put("contentChanges", changes)
        )
    }

    fun didClose(uri: String) {
        rpc.notify(
            "textDocument/didClose",
            JSONObject().put("textDocument", JSONObject().put("uri", uri))
        )
    }

    suspend fun shutdown() {
        try {
            rpc.request("shutdown", null)
        } catch (_: Exception) {
        }
    }

    fun exit() {
        rpc.notify("exit", null)
    }

    fun close() = rpc.close()

    private fun parseDiagnostics(arr: org.json.JSONArray?): List<LspDiagnostic> {
        val out = mutableListOf<LspDiagnostic>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val d = arr.optJSONObject(i) ?: continue
            val range = d.optJSONObject("range") ?: continue
            val s = range.optJSONObject("start") ?: continue
            val e = range.optJSONObject("end") ?: continue
            out += LspDiagnostic(
                line = s.optInt("line", 0),
                character = s.optInt("character", 0),
                endLine = e.optInt("line", 0),
                endCharacter = e.optInt("character", 0),
                severity = d.optInt("severity", 1),
                message = d.optString("message", ""),
                source = d.optString("source", "")
            )
        }
        return out
    }
}
