package com.vscodemobile.files

import java.io.File

object FileUtils {
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

    fun read(f: File): String = try {
        f.readText()
    } catch (_: Exception) {
        ""
    }

    fun write(f: File, content: String): Boolean = try {
        f.parentFile?.mkdirs()
        f.writeText(content)
        true
    } catch (_: Exception) {
        false
    }
}
