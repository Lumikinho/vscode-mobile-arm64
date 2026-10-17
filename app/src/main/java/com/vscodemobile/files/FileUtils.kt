package com.vscodemobile.files

import java.io.File

/**
 * Resultado de ler um documento.
 *
 * A distinção importa: "vazio" é um texto legítimo, "binário" e "ilegível"
 * não podem virar `""` porque o editor abriria em branco e o próximo save
 * sobrescreveria o arquivo com conteúdo corrompido.
 */
sealed interface DocContent {
    data class Text(val content: String) : DocContent

    /** Tem byte nulo nos primeiros [BINARY_PROBE] bytes — não é texto. */
    data object Binary : DocContent

    /** Não deu para ler (permissão, I/O, arquivo sumiu). */
    data object Unreadable : DocContent
}

object FileUtils {

    /** Acima disso, ler o arquivo inteiro na memória passa a machucar. */
    const val LARGE_FILE_BYTES = 2_000_000L

    /** Quantos bytes inspecionar para decidir se é texto. */
    const val BINARY_PROBE = 8_192

    private val TEXT_EXT = setOf(
        "kt", "java", "js", "ts", "py", "c", "h", "cpp", "cs", "go",
        "rs", "php", "rb", "html", "css", "xml", "json", "md", "txt",
        "sh", "gradle", "yaml", "yml", "toml", "sql"
    )

    fun listFiles(dir: File): List<File> =
        dir.listFiles()
            ?.filter { it.isDirectory || isTextFile(it) }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

    fun isTextFile(f: File): Boolean {
        if (f.isDirectory) return false
        val ext = f.extension.lowercase()
        if (ext in TEXT_EXT) return true
        // sem extensão: aceita se < 1MB (evita binário)
        return f.extension.isEmpty() && f.length() < 1_000_000
    }

    /**
     * Heurística de binário: byte nulo no início. É a mesma regra do git.
     *
     * Sem isto, abrir um .png pelo explorer SAF "funciona": os bytes viram
     * texto via decodificação com replacement chars, e salvar grava o lixo de
     * volta no arquivo — corrompendo de vez.
     */
    fun looksBinary(bytes: ByteArray): Boolean {
        val n = minOf(bytes.size, BINARY_PROBE)
        for (i in 0 until n) if (bytes[i] == 0.toByte()) return true
        return false
    }

    fun readDoc(f: File): DocContent = try {
        if (!f.isFile) {
            DocContent.Unreadable
        } else {
            val bytes = f.readBytes()
            if (looksBinary(bytes)) {
                DocContent.Binary
            } else {
                DocContent.Text(String(bytes, Charsets.UTF_8))
            }
        }
    } catch (_: Exception) {
        DocContent.Unreadable
    }

    /**
     * Lê o arquivo, ou devolve `null` se não deu.
     *
     * Antes devolvia `""` em qualquer exceção, o que tornava "arquivo vazio"
     * e "leitura falhou" indistinguíveis.
     */
    fun readOrNull(f: File): String? = (readDoc(f) as? DocContent.Text)?.content

    /** Arquivo grande demais para carregar inteiro com segurança. */
    fun isTooLarge(f: File): Boolean =
        try {
            f.length() > LARGE_FILE_BYTES
        } catch (_: Exception) {
            false
        }

    /**
     * Escrita atômica: escreve num `.tmp` ao lado e renomeia por cima.
     *
     * Um `writeText` direto que falha no meio deixa o arquivo truncado pela
     * metade, sem volta. O rename é atômico no mesmo filesystem.
     */
    fun writeAtomic(f: File, content: String): Boolean {
        val tmp = File(f.parentFile, "${f.name}.vsctmp")
        return try {
            f.parentFile?.mkdirs()
            tmp.writeText(content)
            if (!tmp.renameTo(f)) {
                // renameTo falha em alguns providers; cai para escrita direta
                // mas só depois de ter escrito o tmp inteiro com sucesso.
                f.writeText(tmp.readText())
                tmp.delete()
            }
            true
        } catch (_: Exception) {
            runCatching { tmp.delete() }
            false
        }
    }

    /** Apaga arquivo e lixo de escrita_atômica que possa ter ficado. */
    fun deleteQuietly(f: File) {
        runCatching { f.delete() }
        runCatching { File(f.parentFile, "${f.name}.vsctmp").delete() }
    }
}