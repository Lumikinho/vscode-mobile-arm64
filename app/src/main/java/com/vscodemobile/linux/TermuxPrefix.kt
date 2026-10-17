package com.vscodemobile.linux

import android.content.Context
import android.system.Os
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Prefixo de userland do Termux instalado dentro do app.
 *
 * Substitui o proot + Alpine. Alem de proot nunca ter executado (o dominio
 * SELinux de targetSdk 29+ nega `execute` sobre app_data_file, medido no
 * aparelho), o Termux nativo evita a traducao de caminhos e a copia de
 * rootfs: sao os mesmos binarios, no mesmo formato, rodando direto.
 *
 * Duas adaptacoes sao inevitaveis ao relocalizar o prefixo:
 *
 * 1. Os binarios tem RUNPATH absoluto `/data/data/com.termux/files/usr/lib`
 *    (verificado no bootstrap aarch64 com readelf). O caminho e mais curto que
 *    o nosso, entao nao da para reescrever a string na tabela sem corromper o
 *    ELF. A saida e `LD_LIBRARY_PATH`, que o linker do Android consulta antes
 *    do DT_RUNPATH.
 * 2. Scripts e configs (etc/profile, bin/pkg, var/lib/dpkg/info) tem o prefixo
 *    embutido em texto. Esse pode ser reescrito, e e reescrito na instalacao.
 *    Arquivos ELF sao deixados intactos de proposito.
 */
object TermuxEnv {
    /** Tag e digest publicados pelo proprio repositorio do Termux. */
    const val BOOTSTRAP_TAG = "bootstrap-2026.10.11-r1+apt.android-7"
    const val BOOTSTRAP_URL =
        "https://github.com/termux/termux-packages/releases/download/" +
            "bootstrap-2026.10.11-r1%2Bapt.android-7/bootstrap-aarch64.zip"
    const val BOOTSTRAP_SHA256 =
        "b7827bc99710293d0e871df8b96d9d3a8ab6b22ee4b4a6f3ac076754818990ab"
    const val BOOTSTRAP_BYTES = 33_044_364L

    /** Prefixo de origem, embutido em binarios e scripts. */
    const val ORIGIN_PREFIX = "/data/data/com.termux/files/usr"

    fun prefixDir(ctx: Context): File = File(ctx.filesDir, "usr")
    fun homeDir(ctx: Context): File = File(ctx.filesDir, "home")
    fun tmpDir(ctx: Context): File = File(prefixDir(ctx), "tmp")
    fun binFile(ctx: Context, name: String): File = File(prefixDir(ctx), "bin/$name")
    fun libDir(ctx: Context): File = File(prefixDir(ctx), "lib")
    fun downloadDir(ctx: Context): File = File(ctx.cacheDir, "termux-dl")
    fun stagingDir(ctx: Context): File = File(ctx.filesDir, ".usr-staging")
    fun trashDir(ctx: Context): File = File(ctx.filesDir, ".usr-old")

    fun isWebUrl(url: String) = url.startsWith("http://") || url.startsWith("https://")
}

sealed interface PrefixState {
    data object Missing : PrefixState
    data class Downloading(val label: String, val done: Long, val total: Long) : PrefixState
    data object Verifying : PrefixState
    data class Extracting(val done: Int, val total: Int) : PrefixState
    data object Ready : PrefixState
    data class Error(val message: String) : PrefixState
}

class TermuxPrefix(private val appCtx: Context) {

    private val _state =
        MutableStateFlow<PrefixState>(if (isReady()) PrefixState.Ready else PrefixState.Missing)
    val state: StateFlow<PrefixState> = _state.asStateFlow()

    /** Duas chamadas de setup nao podem extrair por cima uma da outra. */
    private val gate = Mutex()

    fun prefix(): File = TermuxEnv.prefixDir(appCtx)

    /**
     * Pronto = bash executável, a biblioteca do RUNPATH presente e `bin/sh`
     * criado. O último item não é detalhe: os symlinks do bootstrap só são
     * materializados no fim da extração, e sem `sh` (que é link para bash)
     * boa parte do coreutils simplesmente não existe.
     */
    fun isReady(): Boolean =
        TermuxEnv.binFile(appCtx, "bash").canExecute() &&
            File(TermuxEnv.libDir(appCtx), "libandroid-support.so").isFile &&
            TermuxEnv.binFile(appCtx, "sh").exists()

    /**
     * Ambiente do shell. [LD_LIBRARY_PATH] e o que viabiliza a execucao: sem
     * ele o bash procura as libs no prefixo do Termux, que este processo nao
     * pode nem ler.
     */
    fun env(cwd: String): Array<String> {
        val p = prefix().absolutePath
        val home = TermuxEnv.homeDir(appCtx).absolutePath
        val tmp = TermuxEnv.tmpDir(appCtx).absolutePath
        return arrayOf(
            "PREFIX=$p",
            "HOME=$home",
            "PATH=$p/bin:$p/sbin",
            "LD_LIBRARY_PATH=$p/lib",
            "TMPDIR=$tmp",
            "SHELL=$p/bin/bash",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "LANG=en_US.UTF-8",
            "TERMUX_APP_PACKAGE_MANAGER=apt",
            // apt e dpkg tem o prefixo do Termux compilado no binario, nos
            // caminhos de DADOS (/etc/apt, /var/lib/dpkg). LD_LIBRARY_PATH
            // cobre caminho de biblioteca, nao esse. E binario nao se
            // reescreve sem corromper. Sao as variaveis que os dois respeitam
            // para se relocalizar.
            "APT_CONFIG=$p/etc/apt/apt.conf",
            "DPKG_ADMINDIR=$p/var/lib/dpkg",
            "PWD=$cwd"
        )
    }

    /**
     * Cria o que o apt precisa e redireciona os caminhos de dados.
     *
     * Sem isso o `pkg install` falha com "Unable to determine a suitable
     * packaging system type": o apt procura `etc/apt/apt.conf.d` e o dpkg
     * procura `etc/dpkg/dpkg.cfg.d` no prefixo do Termux, que este processo
     * nao pode nem ler.
     */
    private fun writeAptLayout() {
        val p = prefix()
        val arch = "aarch64"
        listOf(
            "var/cache/apt/archives/partial",
            "var/lib/apt/lists/partial",
            "var/log/apt",
            "etc/apt/apt.conf.d",
            "etc/apt/preferences.d",
            "etc/dpkg/dpkg.cfg.d"
        ).forEach { File(p, it).mkdirs() }

        // Sempre sobrescreve: o arquivo e gerado aqui, nao editado a mao, e
        // uma versao antiga dele mantem o apt quebrado mesmo depois da
        // correcao — que foi o que aconteceu na primeira tentativa.
        val aptConf = File(p, "etc/apt/apt.conf")
        runCatching {
            aptConf.writeText(
                """
                |# Gerado pelo VsCode Mobile: aponta o apt para o prefixo local.
                |# Sem isso o apt procura /etc/apt no prefixo do Termux e falha
                |# com "Unable to determine a suitable packaging system type".
                |Dir "$p";
                |Dir::State "$p/var/lib/apt";
                |Dir::State::status "$p/var/lib/dpkg/status";
                |Dir::Cache "$p/var/cache/apt";
                |Dir::Etc "$p/etc/apt";
                |Dir::Etc::trusted "$p/etc/apt/trusted.gpg";
                |Dir::Etc::trustedparts "$p/etc/apt/trusted.gpg.d";
                |Dir::Etc::preferences "$p/etc/apt/preferences";
                |Dir::Etc::preferencesparts "$p/etc/apt/preferences.d";
                |Dir::Etc::sourcelist "$p/etc/apt/sources.list";
                |Dir::Etc::sourceparts "$p/etc/apt/sources.list.d";
                |Dir::Log "$p/var/log/apt";
                |Dir::Bin "$p/bin";
                |Dir::Bin::dpkg "$p/bin/dpkg";
                |Dir::Bin::methods "$p/lib/apt/methods";
                |Dir::Bin::solvers "$p/lib/apt/solvers";
                |Dir::Bin::planners "$p/lib/apt/planners";
                |Dir::Bin::apt-key "$p/bin/apt-key";
                |Dir::Bin::gpgv "$p/bin/gpgv";
                |Binary::apt-key "$p/bin/apt-key";
                |Binary::gpgv "$p/bin/gpgv";
                |Binary::dpkg "$p/bin/dpkg";
                |APT::Architecture "$arch";
                |APT::Architectures "$arch";
                |""".trimMargin()
            )
        }
    }

    fun shell(): File {
        // Idempotente e barato: garante o .bashrc e o layout do apt mesmo
        // quando o prefixo veio de uma instalação feita antes destas regras
        // existirem — evita obrigar a reinstalar 31 MB por causa delas.
        runCatching {
            writeAptLayout()
            dropTermuxAppOnlyFiles()
            ensureBashrc()
        }
        return TermuxEnv.binFile(appCtx, "bash")
    }

    suspend fun install() = gate.withLock {
        if (isReady()) {
            _state.value = PrefixState.Ready
            return@withLock
        }
        try {
            val zip = withContext(Dispatchers.IO) { fetch() }
            _state.value = PrefixState.Verifying
            withContext(Dispatchers.IO) { verifySha256(zip) }
            _state.value = PrefixState.Extracting(0, 0)
            withContext(Dispatchers.IO) { extract(zip) }
            _state.value =
                if (isReady()) PrefixState.Ready
                else PrefixState.Error("extraiu, mas bash não está executável")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "prefixo do Termux", e)
            _state.value = PrefixState.Error(e.message ?: "falha na instalação")
        }
    }

    /** Remove o prefixo (para reinstalar do zero). */
    suspend fun uninstall() = gate.withLock {
        withContext(Dispatchers.IO) {
            TermuxEnv.prefixDir(appCtx).deleteRecursively()
        }
        _state.value = PrefixState.Missing
    }

    private fun fetch(): File {
        val dir = TermuxEnv.downloadDir(appCtx).apply { mkdirs() }
        val dest = File(dir, "bootstrap-aarch64.zip")
        // Reaproveita o download anterior so se ele bater o hash publicado.
        if (dest.isFile && dest.length() == TermuxEnv.BOOTSTRAP_BYTES &&
            verifySha256(dest)
        ) {
            return dest
        }
        runCatching { dest.delete() }
        val c = open(TermuxEnv.BOOTSTRAP_URL)
        try {
            val total = c.contentLengthLong
            _state.value = PrefixState.Downloading("Termux userland", 0, total)
            var done = 0L
            c.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n == -1) break
                        out.write(buf, 0, n)
                        done += n
                        _state.value = PrefixState.Downloading("Termux userland", done, total)
                    }
                }
            }
        } finally {
            c.disconnect()
        }
        return dest
    }

    private fun open(url: String): HttpURLConnection {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 60_000
        }
        if (c.responseCode !in 200..299) {
            c.disconnect()
            throw IOException("HTTP ${c.responseCode} ao baixar o bootstrap")
        }
        return c
    }

    private fun verifySha256(file: File): Boolean {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n == -1) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().toHex() == TermuxEnv.BOOTSTRAP_SHA256
    }

    /**
     * Extrai para um diretorio de staging e so entao troca de lugar. Se
     * aparecer no meio, o prefixo antigo continua inteiro e utilizavel em vez
     * de um userland pela metade.
     */
    private fun extract(zip: File) {
        val staging = TermuxEnv.stagingDir(appCtx)
        staging.deleteRecursively()
        staging.mkdirs()

        // O bootstrap nao guarda symlinks como entradas do zip: eles vao num
        // SYMLINKS.txt no formato "<alvo><seta><caminho do link>", relativo ao
        // prefixo. Sem recria-los falta o dispatch por argv[0] no estilo
        // busybox: sh, ls, awk, sha384sum e mais de mil outros.
        val symlinks = ArrayList<Pair<String, String>>()

        ZipArchive.open(zip).use { archive ->
            val entries = archive.entries.filter { safeName(it.name) != null }
            var done = 0
            var reported = -1
            for (e in entries) {
                val rel = safeName(e.name) ?: continue
                if (rel == SYMLINKS_ENTRY) {
                    archive.openStream(e).bufferedReader().use { readSymlinks(it, symlinks) }
                } else {
                    val out = File(staging, rel)
                    when {
                        e.isDirectory -> out.mkdirs()

                        e.isSymlink -> {
                            out.parentFile?.mkdirs()
                            out.delete()
                            val target = relocateTarget(
                                archive.openStream(e).use { it.readBytes() }
                                    .toString(Charsets.UTF_8)
                            )
                            Os.symlink(target, out.absolutePath)
                        }

                        else -> {
                            out.parentFile?.mkdirs()
                            archive.openStream(e).use { input ->
                                out.outputStream().use { o -> input.copyTo(o, 64 * 1024) }
                            }
                            // O proprio Termux deixa bin/ e libexec em 700.
                            if (e.permissions and 0b001001001 != 0) {
                                out.setExecutable(true, false)
                            }
                            rewriteTermuxPaths(out)
                        }
                    }
                }
                done++
                val pct = done * 100 / entries.size.coerceAtLeast(1)
                if (pct != reported) {
                    reported = pct
                    _state.value = PrefixState.Extracting(done, entries.size)
                }
            }
        }

        if (symlinks.isEmpty()) {
            throw IOException("bootstrap sem SYMLINKS.txt — corrompido ou de outra versão")
        }
        for ((target, linkPath) in symlinks) {
            val link = File(staging, linkPath)
            link.parentFile?.mkdirs()
            link.delete()
            Os.symlink(relocateTarget(target), link.absolutePath)
        }

        TermuxEnv.homeDir(appCtx).mkdirs()
        TermuxEnv.tmpDir(appCtx).mkdirs()
        writeAptLayout()
        ensureBashrc()

        val target = TermuxEnv.prefixDir(appCtx)
        val trash = TermuxEnv.trashDir(appCtx)
        trash.deleteRecursively()
        if (target.exists() && !target.renameTo(trash)) {
            throw IOException("não consegui aposar o prefixo antigo")
        }
        if (!staging.renameTo(target)) {
            // Deixa o que existia no lugar; melhor um prefixo velho do que nenhum.
            trash.renameTo(target)
            throw IOException("não consegui ativar o prefixo novo")
        }
        trash.deleteRecursively()
    }

    /**
     * Remove o que so faz sentido dentro do app Termux.
     *
     * O script de fallback da "second stage" roda a cada shell e tenta criar
     * `/data/data/com.termux` e copiar para o home do Termux. Num prefixo
     * relocavel ele so produz ruido; essa segunda etapa existe para configurar
     * o app Termux na primeira execucao, o que ja fazemos aqui.
     */
    private fun dropTermuxAppOnlyFiles() {
        val p = prefix()
        listOf(
            "etc/profile.d/01-termux-bootstrap-second-stage-fallback.sh",
            "etc/motd.sh"
        ).forEach { rel ->
            runCatching { File(p, rel).delete() }
        }
    }

    /**
     * Cria o `~/.bashrc` que o shell sourcing no lugar do `/etc/profile`.
     *
     * O bash do Termux tem o caminho do profile compilado no binário
     * (`/data/data/com.termux/files/usr/etc/profile`) e roda com `-l` tenta
     * lê-lo. Esse caminho não existe para nós e não pode ser criado, então o
     * shell é aberto com `-i` e é este arquivo que puxa o profile — que já foi
     * reescrito para o nosso prefixo durante a instalação.
     *
     * Não sobrescreve um .bashrc que o usuário já tenha.
     */
    private fun ensureBashrc() {
        val home = TermuxEnv.homeDir(appCtx)
        val rc = File(home, ".bashrc")
        if (rc.exists()) return
        val p = prefix().absolutePath
        runCatching {
            rc.writeText(
                """
                |# Gerado pelo VsCode Mobile na primeira instalacao do Termux.
                |#
                |# O bash do Termux procura /etc/profile e /etc/bash.bashrc em um
                |# caminho compilado no binario (/data/data/com.termux/...), que
                |# nao existe neste prefixo. O profile do prefixo local, que foi
                |# reescrito durante a instalacao, e carregado a partir daqui.
                |
                |[ -r "$p/etc/profile" ] && . "$p/etc/profile"
                |[ -r "$p/etc/bash.bashrc" ] && . "$p/etc/bash.bashrc"
                |""".trimMargin()
            )
        }
    }

    /**
     * Lê o SYMLINKS.txt no mesmo formato do instalador do Termux:
     * `"<alvo><seta><caminho do link>"`, com o caminho relativo ao prefixo.
     */
    private fun readSymlinks(
        reader: BufferedReader,
        into: MutableList<Pair<String, String>>
    ) {
        into.addAll(SymlinkList.parse(reader) { safeName(it) })
    }

    /**
     * Reloca um alvo de symlink que aponte para o prefixo de origem.
     *
     * O bootstrap usa symlink absoluto em varios lugares — o keyring do apt e
     * o caso que importa: os arquivos `.gpg` em `etc/apt/trusted.gpg.d/`
     * apontam para `/data/data/com.termux/files/usr/share/termux-keyring/`.
     * Deixados como vem, ficam pendurados e o apt falha com NO_PUBKEY, sem
     * nenhuma mensagem de arquivo quebrado.
     *
     * Alvo relativo nao e tocado: `bin/sh -> dash` continua valido.
     */
    private fun relocateTarget(target: String): String {
        val from = TermuxEnv.ORIGIN_PREFIX
        return when {
            target == from -> prefix().absolutePath
            target.startsWith("$from/") ->
                prefix().absolutePath + target.removePrefix(from)
            else -> target
        }
    }

    /** Rejeita entrada que escape do staging. */
    private fun safeName(name: String): String? = SymlinkList.sanitizeLinkPath(name)

    /**
     * Troca o prefixo de origem pelo nosso em arquivos de texto.
     *
     * Só em não-ELF: em binário a string mora numa tabela de tamanho fixo e
     * mudá-la de comprimento corrompe o arquivo. Quem resolve o caso do ELF é
     * o LD_LIBRARY_PATH, não isto.
     */
    private fun rewriteTermuxPaths(file: File) {
        if (file.length() > MAX_REWRITE_BYTES) return
        val bytes = try {
            file.readBytes()
        } catch (_: IOException) {
            return
        }
        if (bytes.size >= 4 && bytes[0] == 0x7F.toByte() && bytes[1] == 0x45.toByte()) return

        val needle = TermuxEnv.ORIGIN_PREFIX.toByteArray(Charsets.UTF_8)
        val at = bytes.indexOf(needle)
        if (at < 0) return

        val ourPrefix = prefix().absolutePath.toByteArray(Charsets.UTF_8)
        val out = java.io.ByteArrayOutputStream(bytes.size)
        var i = 0
        while (i < bytes.size) {
            if (bytes.startsWithAt(needle, i)) {
                out.write(ourPrefix)
                i += needle.size
            } else {
                out.write(bytes[i].toInt())
                i++
            }
        }
        try {
            file.writeBytes(out.toByteArray())
        } catch (e: IOException) {
            Log.w(TAG, "não consegui reescrever ${file.name}", e)
        }
    }

    private fun ByteArray.startsWithAt(needle: ByteArray, at: Int): Boolean {
        if (at + needle.size > size) return false
        for (i in needle.indices) if (this[at + i] != needle[i]) return false
        return true
    }

    private fun ByteArray.indexOf(needle: ByteArray): Int {
        for (i in 0..(size - needle.size)) if (startsWithAt(needle, i)) return i
        return -1
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "VsCodeMobile/prefixo"
        const val MAX_REWRITE_BYTES = 8L * 1024 * 1024
        const val SYMLINKS_ENTRY = "SYMLINKS.txt"

        /** U+2190, o separador que o build do Termux usa no SYMLINKS.txt. */
        const val SYMLINK_ARROW = "←"
    }
}
