package com.vscodemobile.linux

import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Uma entrada do zip com os atributos Unix preservados.
 *
 * [unixMode] vem dos atributos externos do diretório central e é o que
 * distingue symlink de arquivo comum e o que marca executável.
 */
data class ZipEntryInfo(
    val name: String,
    val unixMode: Int,
    val compressionMethod: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val localHeaderOffset: Long
) {
    val isSymlink: Boolean get() = (unixMode and S_IFMT) == S_IFLNK
    val isDirectory: Boolean get() = name.endsWith("/") || (unixMode and S_IFMT) == S_IFDIR

    /** Preserva grupo/outros para arquivos que o Termux instala como 755. */
    val permissions: Int get() = unixMode and 0xFFF

    companion object {
        const val S_IFMT = 0xF000
        const val S_IFDIR = 0x4000
        const val S_IFLNK = 0xA000
    }
}

/**
 * Leitor de zip que enxerga os atributos Unix do diretório central.
 *
 * O [java.util.zip.ZipInputStream] do JDK esconde justamente o que importa
 * para um prefixo de userland: o bit de symlink e o bit de exec. O bootstrap
 * do Termux depende dos dois (`bin/sh` aponta para `bash`, e tudo em `bin/`
 * precisa ser executável), então extrair com a API padrão gera um prefixo que
 * compila mas não roda.
 *
 * Só o diretório central é lido de uma vez; os dados de cada entrada são
 * lidos sob demanda, para não carregar 33 MB na memória.
 */
class ZipArchive private constructor(
    private val raf: RandomAccessFile,
    val entries: List<ZipEntryInfo>
) : Closeable {

    private val byName: Map<String, ZipEntryInfo> = entries.associateBy { it.name }

    operator fun get(name: String): ZipEntryInfo? = byName[name]

    /** Nome relativo canônico (sem `./` inicial), como o zip do Termux traz. */
    fun normalize(name: String): String =
        name.removePrefix("./").trimStart('/')

    fun openStream(entry: ZipEntryInfo): InputStream {
        raf.seek(entry.localHeaderOffset)
        if (readInt(raf) != LOC_SIG) {
            throw IOException("cabeçalho local inválido em ${entry.name}")
        }
        val nameLen = readShortAt(raf, entry.localHeaderOffset + 26)
        val extraLen = readShortAt(raf, entry.localHeaderOffset + 28)
        val dataStart = entry.localHeaderOffset + LOC_HEADER_SIZE + nameLen + extraLen
        raf.seek(dataStart)

        // O limite não é cosmético: sem ele uma entrada STORED continua lendo
        // dentro da seguinte e a extração grava lixo no arquivo de destino.
        val raw = Bounded(NonClosing(raf), entry.compressedSize)
        return when (entry.compressionMethod) {
            0 -> BufferedInputStream(raw, BUFFER)
            8 -> InflaterInputStream(raw, Inflater(true), BUFFER)
            else -> throw IOException("compressão ${entry.compressionMethod} não suportada")
        }
    }

    /** Stream que para em [limit] bytes e depois devolve -1. */
    private class Bounded(private val src: InputStream, private val limit: Long) : InputStream() {
        private var left = limit

        override fun read(): Int {
            if (left <= 0) return -1
            val b = src.read()
            if (b >= 0) left--
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (left <= 0) return -1
            val n = src.read(b, off, minOf(len.toLong(), left).toInt())
            if (n > 0) left -= n
            return n
        }

        override fun available(): Int =
            minOf(src.available().toLong(), left).toInt()
    }

    override fun close() = raf.close()

    /**
     * Envolve o RandomAccessFile sem fechá-lo: o stream de uma entrada não
     * pode derrubar o arquivo inteiro, já que o archive vive até o fim da
     * extração e pode ser reaberto por outra entrada.
     */
    private class NonClosing(private val raf: RandomAccessFile) : InputStream() {
        override fun read(): Int = raf.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
        override fun available(): Int = raf.length().toInt()
        override fun skip(n: Long): Long = raf.skipBytes(n.toInt()).toLong()
    }

    companion object {
        private const val EOCD_SIG = 0x06054b50L
        private const val CEN_SIG = 0x02014b50L
        private const val LOC_SIG = 0x04034b50L
        private const val CEN_HEADER_SIZE = 46
        private const val LOC_HEADER_SIZE = 30
        private const val BUFFER = 64 * 1024
        private const val ZIP64_MARKER = 0xFFFFFFFFL

        fun open(file: File): ZipArchive {
            if (!file.isFile) throw FileNotFoundException("zip ausente: ${file.path}")
            val raf = RandomAccessFile(file, "r")
            try {
                val eocd = findEndOfCentralDirectory(raf)
                val count = readShortAt(raf, eocd + 10)
                val cdOffset = readIntAt(raf, eocd + 16)
                if (count == 0xFFFF || cdOffset == ZIP64_MARKER) {
                    throw IOException("zip64 não suportado")
                }
                val list = readCentralDirectory(raf, cdOffset, count)
                return ZipArchive(raf, list)
            } catch (e: Exception) {
                raf.close()
                throw e
            }
        }

        private fun findEndOfCentralDirectory(raf: RandomAccessFile): Long {
            val len = raf.length()
            val max = minOf(len, 66_000L)
            val start = len - max
            val buf = ByteArray(max.toInt())
            raf.seek(start)
            raf.readFully(buf)
            var i = buf.size - 22
            while (i >= 0) {
                if (readInt(buf, i) == EOCD_SIG) return start + i
                i--
            }
            throw IOException("EOCD não encontrado (zip truncado ou não é zip)")
        }

        /**
         * Lê o diretório central por offsets absolutos, não por leitura
         * sequencial: o cabeçalho tem 14 campos e um campo pulado desalinha
         * todos os seguintes sem erro visível.
         */
        private fun readCentralDirectory(
            raf: RandomAccessFile,
            cdOffset: Long,
            count: Int
        ): List<ZipEntryInfo> {
            val out = ArrayList<ZipEntryInfo>(count)
            var pos = cdOffset
            repeat(count) {
                raf.seek(pos)
                if (readInt(raf) != CEN_SIG) throw IOException("diretório central corrompido")
                val madeBy = readShortAt(raf, pos + 4)
                val method = readShortAt(raf, pos + 10)
                val compressed = readIntAt(raf, pos + 20)
                val uncompressed = readIntAt(raf, pos + 24)
                val nameLen = readShortAt(raf, pos + 28)
                val extraLen = readShortAt(raf, pos + 30)
                val commentLen = readShortAt(raf, pos + 32)
                val external = readIntAt(raf, pos + 38)
                val localOffset = readIntAt(raf, pos + 42)

                val nameBytes = ByteArray(nameLen)
                raf.seek(pos + CEN_HEADER_SIZE)
                raf.readFully(nameBytes)

                val hostSystem = (madeBy ushr 8) and 0xFF
                val mode = if (hostSystem == HOST_UNIX) ((external ushr 16) and 0xFFFF).toInt() else 0

                out.add(
                    ZipEntryInfo(
                        name = String(nameBytes, Charsets.UTF_8),
                        unixMode = mode,
                        compressionMethod = method,
                        compressedSize = compressed,
                        uncompressedSize = uncompressed,
                        localHeaderOffset = localOffset
                    )
                )
                pos += CEN_HEADER_SIZE + nameLen + extraLen + commentLen
            }
            return out
        }

        private const val HOST_UNIX = 3

        private fun readShort(raf: RandomAccessFile): Int {
            val b0 = raf.read()
            val b1 = raf.read()
            if (b0 < 0 || b1 < 0) throw IOException("fim de arquivo inesperado")
            return b0 or (b1 shl 8)
        }

        /** Lê 4 bytes. Somar shorts aqui estouraria a borda do arquivo. */
        private fun readInt(raf: RandomAccessFile): Long {
            val b0 = raf.read()
            val b1 = raf.read()
            val b2 = raf.read()
            val b3 = raf.read()
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
                throw IOException("fim de arquivo inesperado")
            }
            return b0.toLong() or
                (b1.toLong() shl 8) or
                (b2.toLong() shl 16) or
                (b3.toLong() shl 24)
        }

        private fun readShortAt(raf: RandomAccessFile, pos: Long): Int {
            raf.seek(pos)
            return readShort(raf)
        }

        private fun readIntAt(raf: RandomAccessFile, pos: Long): Long {
            raf.seek(pos)
            return readInt(raf)
        }

        private fun readInt(buf: ByteArray, i: Int): Long =
            (buf[i].toLong() and 0xFF) or
                ((buf[i + 1].toLong() and 0xFF) shl 8) or
                ((buf[i + 2].toLong() and 0xFF) shl 16) or
                ((buf[i + 3].toLong() and 0xFF) shl 24)
    }
}
