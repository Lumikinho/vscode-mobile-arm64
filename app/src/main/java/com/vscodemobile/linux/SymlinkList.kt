package com.vscodemobile.linux

import java.io.BufferedReader

/**
 * Parser do `SYMLINKS.txt` do bootstrap do Termux.
 *
 * O zip nao guarda symlinks como entradas: cada linha do arquivo e
 * `"<alvo><U+2190><caminho do link>"`, com o caminho relativo ao prefixo. E o
 * mesmo formato que o instalador do Termux le em
 * `TermuxInstaller.setupBootstrapIfNeeded`.
 *
 * Um exemplo real de linha: `xzmore←./bin/lzmore` — o alvo e o nome do
 * proprio link (dispatch por argv[0], no estilo busybox) e o caminho e onde o
 * link nasce dentro de `$PREFIX`. Trocar os dois lados cria links que parecem
 * corretos e nao funcionam.
 */
object SymlinkList {

    /** U+2190, o separador usado pelo build do Termux. */
    const val ARROW = "←"

    /**
     * @param sanitize transforma o caminho do link; devolve null para rejeitar
     *   (usado para barrar `..` e caminhos absolutos).
     */
    fun parse(reader: BufferedReader, sanitize: (String) -> String?): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val parts = line.split(ARROW)
            // Linha malformada e ruido de um build futuro: pular e melhor que
            // abortar uma instalacao inteira por causa dela.
            if (parts.size != 2) return@forEachLine
            val target = parts[0].trim()
            val linkPath = sanitize(parts[1]) ?: return@forEachLine
            if (target.isNotEmpty()) out.add(target to linkPath)
        }
        return out
    }

    fun parse(text: String, sanitize: (String) -> String?): List<Pair<String, String>> =
        parse(text.reader().buffered(), sanitize)

    /**
     * Normaliza um caminho vindo do zip ou do SYMLINKS.txt, ou devolve null
     * se ele não puder ser escrito com segurança dentro do staging.
     *
     * Cobre `../`, caminho absoluto e string vazia — as três formas de uma
     * entrada mal-intencionada (ou de um build com symlink rompido) escrever
     * fora do prefixo.
     */
    fun sanitizeLinkPath(raw: String): String? {
        val n = raw.trim().removePrefix("./").trimStart('/')
        if (n.isEmpty()) return null
        if (n.split('/').any { it == ".." }) return null
        return n
    }
}
