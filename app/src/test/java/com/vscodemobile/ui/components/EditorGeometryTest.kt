package com.vscodemobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Cobre os cálculos que a UI faz a cada tecla. A propriedade importante de
 * [cursorLineCol] é não alocar cópia do documento: o teste de arquivo grande
 * existe para travar isso, porque a versão anterior usava text.take().
 */
class EditorGeometryTest {

    @Test
    fun `cursor no inicio conta linha zero coluna zero`() {
        assertEquals(0 to 0, cursorLineCol("abc", 0))
    }

    @Test
    fun `cursor no meio da primeira linha`() {
        assertEquals(0 to 2, cursorLineCol("abc", 2))
    }

    @Test
    fun `cursor logo apos a quebra de linha conta a proxima linha`() {
        assertEquals(1 to 0, cursorLineCol("ab\ncd", 3))
    }

    @Test
    fun `linhas multiplas`() {
        val t = "um\ndois\ntres\nquatro"
        assertEquals(0 to 0, cursorLineCol(t, 0))
        assertEquals(0 to 2, cursorLineCol(t, 2))
        assertEquals(1 to 3, cursorLineCol(t, 6))
        assertEquals(3 to 6, cursorLineCol(t, t.length))
    }

    @Test
    fun `caret fora do texto e preso nas bordas`() {
        val t = "abc"
        assertEquals(0 to 0, cursorLineCol(t, -5))
        assertEquals(0 to 3, cursorLineCol(t, 999))
    }

    @Test
    fun `texto vazio`() {
        assertEquals(0 to 0, cursorLineCol("", 0))
    }

    @Test
    fun `linhas em branco contam normalmente`() {
        val t = "a\n\n\nb"
        assertEquals(3 to 0, cursorLineCol(t, 4)) // no inicio de "b"
        assertEquals(3 to 1, cursorLineCol(t, 5)) // depois de "b"
    }

    @Test
    fun `indentLevelOf devolve nivel e nao quantidade de espacos`() {
        // 4 espacos = 1 nivel; tab conta como 4.
        assertEquals(1, indentLevelOf("    quatro"))
        assertEquals(2, indentLevelOf("        oito"))
        assertEquals(2, indentLevelOf("\t\tdois"))
        assertEquals(0, indentLevelOf("nada"))
        assertEquals(0, indentLevelOf("  dois espacos"))
    }

    @Test
    fun `fecha e abre chaves devolvem posicoes`() {
        // matchingBracket devolve as POSIÇÕES do par, não os caracteres.
        assertEquals(1 to 3, matchingBracket("f(a)", 1))
        assertEquals(1 to 3, matchingBracket("f(a)", 3))
        // "[a]" tem 3 chars: abre em 0, fecha em 2.
        assertEquals(0 to 2, matchingBracket("[a]", 3))
        assertEquals(0 to 2, matchingBracket("[a]", 0))
    }

    @Test
    fun `chaves aninhadas`() {
        // "f(g(x))": abre em 1, fecha em 6.
        assertEquals(1 to 6, matchingBracket("f(g(x))", 1))
        assertEquals(3 to 5, matchingBracket("f(g(x))", 3))
    }

    @Test
    fun `sem par nao devolve nada`() {
        assertNull(matchingBracket("f(a", 1))
        assertNull(matchingBracket("abc", 1))
    }
}