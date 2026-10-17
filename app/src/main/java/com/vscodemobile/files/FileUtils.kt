package com.vscodemobile.files

import java.io.File

object FileUtils {

    /** Acima disso, ler o arquivo inteiro na memória passa a machucar. */
    const val LARGE_FILE_BYTES = 2_000_000L

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
     * Lê o arquivo, ou devolve `null` se não deu.
     *
     * Antes devolvia `""` em qualquer exceção, o que tornava "arquivo vazio"
     * e "leitura falhou" indistinguíveis: o editor abria em branco, marcava o
     * doc como limpo e o próximo auto-save sobrescrevia o arquivo original.
     */
    fun readOrNull(f: File): String? = try {
        if (!f.isFile) null else f.readText()
    } catch (_: Exception) {
        null
    }

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
        } catch (e: Exception) {
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