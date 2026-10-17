package com.vscodemobile.linux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O `SYMLINKS.txt` é o que traz `sh`, `ls`, `awk` e mais de mil outros: sem
 * ele o prefixo instala e continua sem o coreutils. O parsing é testado com
 * linhas reais do bootstrap aarch64.
 */
class SymlinkListTest {

    private val parse = { text: String ->
        SymlinkList.parse(text, SymlinkList::sanitizeLinkPath)
    }

    @Test
    fun `le linha real do bootstrap`() {
        // Linha real: o alvo e o nome do link (dispatch por argv[0]).
        val got = parse("xzmore←./bin/lzmore")
        assertEquals(1, got.size)
        assertEquals("xzmore" to "bin/lzmore", got.single())
    }

    @Test
    fun `sh aponta para o nome do proprio link em bin`() {
        // É assim que o bootstrap traz o sh: link em bin/sh, alvo "sh", que
        // resolve para bin/bash na hora do exec.
        val got = parse("sh←./bin/sh")
        assertEquals("sh" to "bin/sh", got.single())
    }

    @Test
    fun `mantem a ordem e conta todas as linhas`() {
        val text = """
            xzmore←./bin/lzmore
            coreutils←./bin/sha384sum
            gawk←./bin/awk
            coreutils←./bin/dirname
        """.trimIndent()
        val got = parse(text)
        assertEquals(4, got.size)
        assertEquals("coreutils" to "bin/dirname", got.last())
    }

    @Test
    fun `ignora linha malformada sem perder as validas`() {
        val text = """
            boa1←./bin/a
            linha sem separador
            boa2←./bin/b
        """.trimIndent()
        assertEquals(listOf("boa1" to "bin/a", "boa2" to "bin/b"), parse(text))
    }

    @Test
    fun `descarta link que tentaria escapar do prefixo`() {
        assertTrue(parse("mau←../../etc/passwd").isEmpty())
        assertTrue(parse("mau←bin/../../etc/shadow").isEmpty())
    }

    @Test
    fun `descarta alvo vazio`() {
        assertTrue(parse("←./bin/vazio").isEmpty())
    }

    @Test
    fun `normaliza caminho relativo e absoluto`() {
        assertEquals("bin/sh", SymlinkList.sanitizeLinkPath("./bin/sh"))
        assertEquals("bin/sh", SymlinkList.sanitizeLinkPath("/bin/sh"))
        assertEquals("bin/sh", SymlinkList.sanitizeLinkPath("  bin/sh  "))
    }

    @Test
    fun `rejeita entrada vazia`() {
        assertEquals(null, SymlinkList.sanitizeLinkPath(""))
        assertEquals(null, SymlinkList.sanitizeLinkPath("./"))
    }

    @Test
    fun `nao confunde ponto duplo legitimo com travessia`() {
        // "foo..bar" é um nome de arquivo comum e tem de passar.
        assertEquals("bin/foo..bar", SymlinkList.sanitizeLinkPath("bin/foo..bar"))
    }
}
