package com.vscodemobile.lsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * O framing do LSP conta Content-Length em BYTES, nunca em caracteres. Um
 * payload com acento é o caso que separa as duas coisas, e é exatamente onde
 * um servidor real trava se a contagem divergir.
 *
 * Estes testes não sobem socket: usam um par de streams em memória.
 */
class JsonRpcFramingTest {

    /** Monta um pacote LSP com Content-Length calculado em bytes. */
    private fun frame(body: String): ByteArray {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val head = "Content-Length: ${bytes.size}\r\n\r\n"
        return head.toByteArray(StandardCharsets.US_ASCII) + bytes
    }

    @Test
    fun `content-length conta bytes e nao caracteres`() {
        val body = """{"jsonrpc":"2.0","method":"x","params":{"msg":"ação"}}"""
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        // "ç" e "ã" ocupam 2 bytes cada em UTF-8: 4 caracteres a mais que bytes
        assertTrue(bytes.size > body.length)

        val stream = ByteArrayInputStream(frame(body))
        val head = ByteArrayOutputStream()
        var b: Int
        while (true) {
            b = stream.read()
            if (b == -1) break
            head.write(b)
            val h = head.toString(StandardCharsets.US_ASCII).toByteArray(StandardCharsets.US_ASCII)
            val n = h.size
            if (n >= 4 &&
                h[n - 4] == '\r'.code.toByte() &&
                h[n - 3] == '\n'.code.toByte() &&
                h[n - 2] == '\r'.code.toByte() &&
                h[n - 1] == '\n'.code.toByte()
            ) break
        }
        val declared = head.toString(StandardCharsets.US_ASCII.name())
            .lines()
            .first { it.startsWith("Content-Length:") }
            .substringAfter(":").trim().toInt()

        assertEquals(bytes.size, declared)
    }

    @Test
    fun `payload com acento sobrevive ao round trip`() {
        val body = """{"jsonrpc":"2.0","id":1,"result":{"detalhe":"olá, ção — ação"}}"""
        val packet = frame(body)
        val stream = ByteArrayInputStream(packet)

        // Consome o header
        val header = ByteArrayOutputStream()
        var matched = 0
        while (true) {
            val b = stream.read()
            if (b == -1) break
            val bb = b.toByte()
            matched = when {
                matched == 0 -> if (bb == '\r'.code.toByte()) 1 else 0
                matched == 1 -> if (bb == '\n'.code.toByte()) 2 else if (bb == '\r'.code.toByte()) 1 else 0
                matched == 2 -> if (bb == '\r'.code.toByte()) 3 else 0
                else -> if (bb == '\n'.code.toByte()) 4 else 0
            }
            header.write(b)
            if (matched == 4) break
        }

        val length = header.toString(StandardCharsets.US_ASCII.name())
            .lines().first { it.startsWith("Content-Length:") }
            .substringAfter(":").trim().toInt()

        val buf = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = stream.read(buf, read, length - read)
            if (n == -1) break
            read += n
        }

        assertEquals(body, String(buf, StandardCharsets.UTF_8))
        // Não valida com org.json aqui: em teste unitário ele é o stub do
        // Android e lança "not mocked". O que importa é a igualdade de bytes.
        assertTrue(String(buf, StandardCharsets.UTF_8).contains("ação"))
    }

    @Test
    fun `estado do scanner de CRLFCRLF nao se perde com CR isolado`() {
        // Robustez do scanner de CRLFCRLF contra entradas patológicas.
        val casos = listOf(
            intArrayOf(13, 10, 13, 10),        // \r\n\r\n completo
            intArrayOf(13, 13, 10, 13, 10),     // \r repetido
            intArrayOf(10, 13, 10, 13, 10)     // LF antes de CRLF
        )
        for (seq in casos) {
            var matched = 0
            for (raw in seq) {
                val bb = raw.toByte()
                matched = when {
                    matched == 0 -> if (bb == '\r'.code.toByte()) 1 else 0
                    matched == 1 -> if (bb == '\n'.code.toByte()) 2 else if (bb == '\r'.code.toByte()) 1 else 0
                    matched == 2 -> if (bb == '\r'.code.toByte()) 3 else 0
                    else -> if (bb == '\n'.code.toByte()) 4 else 0
                }
            }
            assertTrue("falhou para $seq", matched == 4)
        }
    }
}