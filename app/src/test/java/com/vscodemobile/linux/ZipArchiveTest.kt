package com.vscodemobile.linux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32

/**
 * O leitor de zip precisa ser testado contra algo que o ZipOutputStream do JDK
 * não consegue produzir: entrada com atributos Unix, symlink e bit de exec.
 * Por isso o zip é montado byte a byte aqui.
 */
class ZipArchiveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private data class Entry(val name: String, val data: ByteArray, val unixMode: Int)

    private fun shortLE(n: Int): ByteArray = byteArrayOf(
        (n and 0xFF).toByte(), ((n shr 8) and 0xFF).toByte()
    )

    private fun intLE(n: Long): ByteArray = byteArrayOf(
        (n and 0xFF).toByte(), ((n shr 8) and 0xFF).toByte(),
        ((n shr 16) and 0xFF).toByte(), ((n shr 24) and 0xFF).toByte()
    )

    /** Zip STORED com "version made by" = Unix, para os atributos valerem. */
    private fun buildZip(entries: List<Entry>): File {
        val body = ByteArrayOutputStream()
        val central = ByteArrayOutputStream()
        var offset = 0L

        for (e in entries) {
            val name = e.name.toByteArray(Charsets.UTF_8)
            val crc = CRC32().apply { update(e.data) }.value

            val lh = ByteArrayOutputStream()
            lh.write(intLE(0x04034b50))
            lh.write(shortLE(20))           // version needed
            lh.write(shortLE(0))            // flags
            lh.write(shortLE(0))            // STORED
            lh.write(shortLE(0)); lh.write(shortLE(0))
            lh.write(intLE(crc))
            lh.write(intLE(e.data.size.toLong()))
            lh.write(intLE(e.data.size.toLong()))
            lh.write(shortLE(name.size))
            lh.write(shortLE(0))
            lh.write(name)
            body.write(lh.toByteArray())
            body.write(e.data)

            val cen = ByteArrayOutputStream()
            cen.write(intLE(0x02014b50))
            cen.write(shortLE((3 shl 8) or 20))   // host = Unix
            cen.write(shortLE(20))
            cen.write(shortLE(0))                 // flags
            cen.write(shortLE(0))                 // STORED
            cen.write(shortLE(0)); cen.write(shortLE(0))
            cen.write(intLE(crc))
            cen.write(intLE(e.data.size.toLong()))
            cen.write(intLE(e.data.size.toLong()))
            cen.write(shortLE(name.size))
            cen.write(shortLE(0))                 // extra
            cen.write(shortLE(0))                 // comment
            cen.write(shortLE(0))                 // disk
            cen.write(shortLE(0))                 // internal
            cen.write(intLE((e.unixMode.toLong() and 0xFFFFL) shl 16))
            cen.write(intLE(offset))
            cen.write(name)
            central.write(cen.toByteArray())

            offset += 30 + name.size + e.data.size
        }

        val cd = central.toByteArray()
        body.write(cd)

        val eocd = ByteArrayOutputStream()
        eocd.write(intLE(0x06054b50))
        eocd.write(shortLE(0)); eocd.write(shortLE(0))
        eocd.write(shortLE(entries.size)); eocd.write(shortLE(entries.size))
        eocd.write(intLE(cd.size.toLong()))
        eocd.write(intLE(offset))
        eocd.write(shortLE(0))
        body.write(eocd.toByteArray())

        val f = tmp.newFile("test.zip")
        f.writeBytes(body.toByteArray())
        return f
    }

    private fun mode(type: Int, perm: Int) = type or perm

    @Test
    fun `le arquivo comum com conteudo e permissao`() {
        val zip = buildZip(
            listOf(
                Entry("bin/bash", "conteudo bash".toByteArray(),
                    mode(0x8000, 0b111_101_101))
            )
        )
        ZipArchive.open(zip).use { a ->
            assertEquals(1, a.entries.size)
            val e = a.entries.single()
            assertEquals("bin/bash", e.name)
            assertFalse(e.isSymlink)
            assertFalse(e.isDirectory)
            assertEquals(0b111_101_101, e.permissions)
            assertTrue(e.permissions and 0b001001001 != 0)
            assertEquals("conteudo bash", a.openStream(e).readBytes().toString(Charsets.UTF_8))
        }
    }

    @Test
    fun `reconhece symlink e le o alvo do proprio conteudo`() {
        // É assim que o bootstrap traz bin/sh: entrada symlink cujo conteúdo
        // é o caminho do alvo, não um arquivo.
        val zip = buildZip(
            listOf(
                Entry("bin/sh", "bash".toByteArray(), mode(0xA000, 0b111_111_111)),
                Entry("bin/bash", "ELF".toByteArray(), mode(0x8000, 0b111_101_101))
            )
        )
        ZipArchive.open(zip).use { a ->
            val sh = a.entries.first { it.name == "bin/sh" }
            assertTrue("bin/sh deveria ser symlink", sh.isSymlink)
            assertEquals(0xA000, sh.unixMode and 0xF000)
            assertEquals("bash", a.openStream(sh).readBytes().toString(Charsets.UTF_8))

            val bash = a.entries.first { it.name == "bin/bash" }
            assertFalse(bash.isSymlink)
        }
    }

    @Test
    fun `reconhece diretorio`() {
        val zip = buildZip(
            listOf(
                Entry("bin/", ByteArray(0), mode(0x4000, 0b111_101_101)),
                Entry("bin/ls", "ELF".toByteArray(), mode(0x8000, 0b111_101_101))
            )
        )
        ZipArchive.open(zip).use { a ->
            assertTrue(a.entries.first { it.name == "bin/" }.isDirectory)
            assertFalse(a.entries.first { it.name == "bin/ls" }.isDirectory)
        }
    }

    @Test
    fun `preserva alinhamento quando ha nomes de tamanhos variados`() {
        // Nomes de comprimento par e impar: um parse sequencial erra o offset
        // da entrada seguinte e o sintoma aparece como lixo ou EOF.
        val names = listOf("a", "bb", "ccc", "bin/dd", "bin/e", "lib/fffff")
        val zip = buildZip(
            names.mapIndexed { i, n ->
                Entry(n, "d$i".toByteArray(), mode(0x8000, 0b110_100_100))
            }
        )
        ZipArchive.open(zip).use { a ->
            assertEquals(names, a.entries.map { it.name })
            names.forEachIndexed { i, n ->
                val e = a.entries.first { it.name == n }
                assertEquals("d$i", a.openStream(e).readBytes().toString(Charsets.UTF_8))
                assertEquals(0b110_100_100, e.permissions)
            }
        }
    }

    @Test
    fun `zip de host nao-unix nao inventa modo de exec`() {
        // Zip criado no Windows: sem atributos Unix, o bit de exec nao existe.
        val zip = buildZipUnixHost(listOf(Entry("bin/ls", "ELF".toByteArray(), 0)))
        ZipArchive.open(zip).use { a ->
            val e = a.entries.single()
            assertEquals(0, e.permissions)
            assertFalse(e.isSymlink)
        }
    }

    private fun buildZipUnixHost(entries: List<Entry>): File {
        val f = buildZip(entries)
        val bytes = f.readBytes()
        // Version made by fica em +4 do inicio de cada entrada do diretorio
        // central; reescreve o byte de host para 0 (FAT).
        var i = 0
        while (i + 4 < bytes.size) {
            if (bytes[i] == 0x50.toByte() && bytes[i + 1] == 0x4b.toByte() &&
                bytes[i + 2] == 0x01.toByte() && bytes[i + 3] == 0x02.toByte()
            ) {
                bytes[i + 5] = 0
            }
            i++
        }
        f.writeBytes(bytes)
        return f
    }

    @Test(expected = java.io.IOException::class)
    fun `rejeita arquivo que nao e zip`() {
        val f = tmp.newFile("lixo.zip")
        f.writeBytes(ByteArray(200) { it.toByte() })
        ZipArchive.open(f).use { }
    }
}
