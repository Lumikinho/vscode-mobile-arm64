package com.vscodemobile.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Cobre o caminho que destrói trabalho do usuário: leitura que falha virando
 * string vazia, e escrita que reporta sucesso sem ter escrito.
 */
class FileUtilsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `readOrNull devolve o conteudo de arquivo normal`() {
        val f = tmp.newFile("ok.kt")
        f.writeText("fun main() {}")
        assertEquals("fun main() {}", FileUtils.readOrNull(f))
    }

    @Test
    fun `arquivo vazio devolve string vazia, nao null`() {
        // A distinção é o ponto: vazio ≠ falha. Aqui vazio é a resposta certa.
        val f = tmp.newFile("vazio.kt")
        assertEquals("", FileUtils.readOrNull(f))
    }

    @Test
    fun `readOrNull devolve null quando o arquivo nao existe`() {
        assertNull(FileUtils.readOrNull(File(tmp.root, "fantasma.kt")))
    }

    @Test
    fun `readOrNull devolve null quando o caminho e um diretorio`() {
        val d = tmp.newFolder("pasta")
        assertNull(FileUtils.readOrNull(d))
    }

    @Test
    fun `readOrNull devolve null em vez de vazio quando falta permissao`() {
        val f = tmp.newFile("bloqueado.kt")
        f.writeText("conteudo original")
        f.setReadable(false)
        try {
            // Em Windows a ACL raramente bloqueia leitura; o teste só é
            // significativo onde getReadable() reflete o filesystem.
            if (!f.canRead()) {
                assertNull(FileUtils.readOrNull(f))
                assertEquals("conteudo original", f.readText())
            }
        } finally {
            f.setReadable(true)
        }
    }

    @Test
    fun `writeAtomic cria o arquivo e o conteudo`() {
        val f = File(tmp.root, "novo.kt")
        assertTrue(FileUtils.writeAtomic(f, "conteudo"))
        assertEquals("conteudo", f.readText())
    }

    @Test
    fun `writeAtomic sobrescreve arquivo existente por completo`() {
        val f = tmp.newFile("existe.kt")
        f.writeText("texto muito mais longo que o novo")
        assertTrue(FileUtils.writeAtomic(f, "curto"))
        assertEquals("curto", f.readText())
    }

    @Test
    fun `writeAtomic nao deixa arquivo temporario para tras`() {
        val f = File(tmp.root, "limpo.kt")
        assertTrue(FileUtils.writeAtomic(f, "x"))
        val sobras = tmp.root.listFiles()?.filter { it.name.endsWith(".vsctmp") } ?: emptyList()
        assertTrue("sobrou temporario: $sobras", sobras.isEmpty())
    }

    @Test
    fun `writeAtomic cria diretorios que nao existem`() {
        val f = File(tmp.root, "a/b/c/novo.kt")
        assertTrue(FileUtils.writeAtomic(f, "profundo"))
        assertEquals("profundo", f.readText())
    }

    @Test
    fun `writeAtomic devolve false quando o destino e um diretorio`() {
        val d = tmp.newFolder("destino")
        assertFalse(FileUtils.writeAtomic(d, "x"))
    }

    @Test
    fun `isTextFile aceita extensao conhecida`() {
        assertTrue(FileUtils.isTextFile(File(tmp.root, "a.kt")))
        assertTrue(FileUtils.isTextFile(File(tmp.root, "b.json")))
    }

    @Test
    fun `isTextFile rejeita extensao desconhecida`() {
        assertFalse(FileUtils.isTextFile(File(tmp.root, "a.bin")))
    }

    @Test
    fun `isTooLarge respeita o limite`() {
        val pequeno = tmp.newFile("pequeno.bin")
        pequeno.writeBytes(ByteArray(10))
        assertFalse(FileUtils.isTooLarge(pequeno))

        // Inexistente tem length 0: não é "grande", só não existe.
        assertFalse(FileUtils.isTooLarge(File(tmp.root, "inexistente.bin")))

        val grande = tmp.newFile("grande.bin")
        // Acima do limite de 2 MB precisa existir de verdade para contar.
        grande.writeBytes(ByteArray((FileUtils.LARGE_FILE_BYTES + 1024).toInt()))
        assertTrue(FileUtils.isTooLarge(grande))
        assertFalse(FileUtils.isTooLarge(pequeno))
    }

    @Test
    fun `deleteQuietly remove o arquivo e o temporario`() {
        val f = File(tmp.root, "some.kt")
        FileUtils.writeAtomic(f, "x")
        val tmpFile = File(tmp.root, "some.kt.vsctmp")
        tmpFile.writeText("lixo")
        FileUtils.deleteQuietly(f)
        FileUtils.deleteQuietly(tmpFile)
        assertFalse(f.exists())
        assertFalse(tmpFile.exists())
    }
}