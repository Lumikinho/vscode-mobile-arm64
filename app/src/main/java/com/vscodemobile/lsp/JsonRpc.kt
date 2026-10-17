package com.vscodemobile.lsp

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * JSON-RPC 2.0 sobre socket, como um language server espera:
 * headers `Content-Length` + corpo UTF-8. Leitura em bytes (nunca chars),
 * pois Content-Length conta bytes.
 */
class JsonRpcConnection(private val socket: Socket) {

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()
    private val writeLock = Any()
    private val nextId = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<JSONObject?>>()

    var onNotification: ((method: String, params: JSONObject?) -> Unit)? = null

    @Volatile
    private var running = false

    fun start(scope: CoroutineScope) {
        running = true
        scope.launch(Dispatchers.IO) {
            try {
                while (running) {
                    val msg = readMessage() ?: break
                    handle(msg)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("VsCodeMobile", "lsp leitura", e)
            }
        }
    }

    suspend fun request(method: String, params: JSONObject?): JSONObject? {
        val id = nextId.getAndIncrement()
        val done = CompletableDeferred<JSONObject?>()
        pending[id] = done
        try {
            val msg = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", id)
                .put("method", method)
                .put("params", params ?: JSONObject.NULL)
            send(msg)
            return withTimeout(20_000) { done.await() }
        } finally {
            pending.remove(id)
        }
    }

    fun notify(method: String, params: JSONObject?) {
        try {
            val msg = JSONObject()
                .put("jsonrpc", "2.0")
                .put("method", method)
                .put("params", params ?: JSONObject.NULL)
            send(msg)
        } catch (e: Exception) {
            Log.w("VsCodeMobile", "lsp notify $method", e)
        }
    }

    fun close() {
        running = false
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }

    private fun send(msg: JSONObject) {
        val body = msg.toString().toByteArray(Charsets.UTF_8)
        val head = "Content-Length: ${body.size}\r\n\r\n".toByteArray(Charsets.US_ASCII)
        synchronized(writeLock) {
            output.write(head)
            output.write(body)
            output.flush()
        }
    }

    private fun handle(msg: JSONObject) {
        if (msg.has("method") && !msg.isNull("method")) {
            if (!msg.isNull("id")) {
                // Requisição servidor->cliente (ex.: window/workDoneProgress/create):
                // responde null para não travar o servidor.
                val id = msg.opt("id")
                val resp = JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", JSONObject.NULL)
                try {
                    send(resp)
                } catch (_: Exception) {
                }
            } else {
                try {
                    onNotification?.invoke(
                        msg.optString("method", ""),
                        msg.optJSONObject("params")
                    )
                } catch (e: Exception) {
                    Log.w("VsCodeMobile", "lsp notificação", e)
                }
            }
            return
        }
        if (!msg.isNull("id")) {
            val id = msg.optInt("id", -1)
            val result = msg.optJSONObject("result")
            pending[id]?.complete(result)
        }
    }

    private fun readMessage(): JSONObject? {
        // Acumula os headers byte a byte. Só os 4 últimos importam (para achar
        // \r\n\r\n), então nada é copiado por byte: o ByteArrayOutputStream
        // cresce uma vez e o array final é alocado uma única vez.
        val head = java.io.ByteArrayOutputStream(128)
        val cr = '\r'.code.toByte()
        val lf = '\n'.code.toByte()
        var matched = 0
        while (true) {
            val b = input.read()
            if (b == -1) return null
            val bb = b.toByte()
            // sequência esperada: \r \n \r \n
            matched = when {
                matched == 0 -> if (bb == cr) 1 else 0
                matched == 1 -> if (bb == lf) 2 else if (bb == cr) 1 else 0
                matched == 2 -> if (bb == cr) 3 else 0
                else -> if (bb == lf) 4 else 0
            }
            head.write(b)
            if (matched == 4) break
        }
        val headerText = head.toString(Charsets.US_ASCII.name())
        val length = headerText.lines()
            .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()?.toIntOrNull()
            ?: return null
        if (length < 0 || length > MAX_BODY_BYTES) return null
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n == -1) return null
            read += n
        }
        return JSONObject(String(body, Charsets.UTF_8))
    }

    private companion object {
        /** Teto defensivo: um Content-Length absurdo travaria o reader. */
        const val MAX_BODY_BYTES = 32 * 1024 * 1024
    }
}
