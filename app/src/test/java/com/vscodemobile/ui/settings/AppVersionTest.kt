package com.vscodemobile.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comparar versão por string ordena ao contrário: "1.10.0" < "1.9.0", e o app
 * passa a oferecer downgrade como se fosse atualização.
 */
class AppVersionTest {

    @Test
    fun `compara numericamente e nao como texto`() {
        val a = AppVersion.parse("1.9.0")!!
        val b = AppVersion.parse("1.10.0")!!
        assertTrue("1.10.0 deve ser maior que 1.9.0", b > a)
        assertFalse("1.9.0 não pode parecer mais novo", a > b)
    }

    @Test
    fun `compara patch e minor`() {
        assertTrue(AppVersion.parse("1.0.1")!! > AppVersion.parse("1.0.0")!!)
        assertTrue(AppVersion.parse("1.1.0")!! > AppVersion.parse("1.0.9")!!)
        assertTrue(AppVersion.parse("2.0.0")!! > AppVersion.parse("1.99.99")!!)
    }

    @Test
    fun `aceita tag com prefixo v`() {
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("v1.2.3"))
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("V1.2.3"))
    }

    @Test
    fun `preenche patch ausente`() {
        assertEquals(AppVersion(1, 2, 0), AppVersion.parse("1.2"))
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("v1.2.3"))
    }

    @Test
    fun `recusa versao de um so numero`() {
        // "3" e ambiguo: 3.0.0 ou 0.3.0? Adivinhar errado faz o app oferecer
        // downgrade, entao um numero so e rejeitado.
        assertNull(AppVersion.parse("v3"))
        assertNull(AppVersion.parse("2"))
    }

    @Test
    fun `ignora sufixo de pre-lançamento na comparacao`() {
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("v1.2.3-rc1"))
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("v1.2.3+build9"))
    }

    @Test
    fun `rejeita o que nao e versao`() {
        assertNull(AppVersion.parse(null))
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse("nightly"))
        assertNull(AppVersion.parse("v"))
        assertNull(AppVersion.parse("1.x.3"))
    }

    @Test
    fun `isNewer responde falso quando e a mesma versao`() {
        assertFalse(AppVersion.isNewer("v1.1.0", AppVersion(1, 1, 0)) == true)
    }

    @Test
    fun `isNewer detecta release acima da instalada`() {
        assertTrue(AppVersion.isNewer("v1.2.0", AppVersion(1, 1, 0)) == true)
    }

    @Test
    fun `isNewer devolve null quando nao da para dizer`() {
        assertNull(AppVersion.isNewer("nightly", AppVersion(1, 1, 0)))
        assertNull(AppVersion.isNewer("v1.2.0", null))
    }

    @Test
    fun `formata de volta em tres partes`() {
        assertEquals("1.2.3", AppVersion.parse("v1.2.3").toString())
        assertEquals("1.2.0", AppVersion.parse("1.2").toString())
    }
}
