package com.vscodemobile.linux

import android.content.Context
import android.system.Os
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

/**
 * Linux embutido (proot + Alpine), sem depender do app Termux.
 *
 * Por que proot: os binários do Termux têm o prefixo /data/data/com.termux
 * embutido (INTERP/RPATH) e não executam sob outro pacote. O proot traduz os
 * caminhos do convidado, então qualquer rootfs roda. Sem pty próprio aqui:
 * as sessões usam pipes (como o shell local).
 *
 * Fontes (runtime, com progresso):
 * - proot: build portátil baseado no pacote do Termux (skirsten, aarch64)
 * - Alpine minirootfs: versão resolvida via latest-releases.yaml oficial
 */
object LinuxEnv {
    const val PROOT_URL =
        "https://skirsten.github.io/proot-portable-android-binaries/aarch64/proot"
    const val ALPINE_YAML_URL =
        "https://dl-cdn.alpinelinux.org/alpine/latest-stable/releases/aarch64/latest-releases.yaml"

    fun baseDir(ctx: Context): File = File(ctx.filesDir, "linux")
    fun binDir(ctx: Context): File = File(baseDir(ctx), "bin")
    fun prootBin(ctx: Context): File = File(binDir(ctx), "proot")
    fun alpineRoot(ctx: Context): File = File(baseDir(ctx), "alpine-rootfs")
    fun tmpDir(ctx: Context): File = File(ctx.cacheDir, "proot-tmp")
    fun workDir(ctx: Context): File = File(baseDir(ctx), "downloads").apply { mkdirs() }
}

sealed interface LinuxState {
    data object Missing : LinuxState
    data class Downloading(val label: String, val done: Long, val total: Long) : LinuxState
    data object Verifying : LinuxState
    data object Extracting : LinuxState
    data object Ready : LinuxState
    data class Error(val message: String) : LinuxState
}

class LinuxManager(private val appCtx: Context) {

    private val _state =
        MutableStateFlow<LinuxState>(if (isReady()) LinuxState.Ready else LinuxState.Missing)
    val state: StateFlow<LinuxState> = _state.asStateFlow()

    fun isReady(): Boolean {
        val proot = LinuxEnv.prootBin(appCtx)
        val sh = File(LinuxEnv.alpineRoot(appCtx), "bin/sh")
        return proot.canExecute() && sh.isFile
    }

    fun rootfsDir(): File? =
        LinuxEnv.alpineRoot(appCtx).takeIf { isReady() }

    /** Linha de comando proot pronta para virar sessão do terminal. */
    fun prootCommand(): List<String>? {
        val root = rootfsDir() ?: return null
        LinuxEnv.tmpDir(appCtx).mkdirs()
        return listOf(
            LinuxEnv.prootBin(appCtx).absolutePath,
            "-r", root.absolutePath,
            "-0",
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-w", "/root",
            "/bin/sh"
        )
    }

    fun prootEnv(): Map<String, String> = mapOf(
        "HOME" to "/root",
        "TERM" to "xterm-256color",
        "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/bin:/sbin:/bin",
        "TMPDIR" to "/tmp",
        "PROOT_TMPDIR" to LinuxEnv.tmpDir(appCtx).absolutePath
    )

    /** Pipeline completo: proot + Alpine + verificação + extração. */
    suspend fun setup() {
        _state.value = LinuxState.Downloading("proot", 0, -1)
        try {
            val binDir = LinuxEnv.binDir(appCtx).apply { mkdirs() }
            val proot = File(binDir, "proot")
            if (!proot.canExecute()) {
                downloadTo(LinuxEnv.PROOT_URL, proot) { done, _ ->
                    _state.value = LinuxState.Downloading("proot", done, -1)
                }
                proot.setExecutable(true)
            }
            val (tarUrl, sha) = withContext(Dispatchers.IO) { resolveAlpine() }
            val work = LinuxEnv.workDir(appCtx)
            val tar = File(work, tarUrl.substringAfterLast('/'))
            _state.value = LinuxState.Downloading("Alpine Linux", 0, -1)
            downloadTo(tarUrl, tar) { done, total ->
                _state.value = LinuxState.Downloading("Alpine Linux", done, total)
            }
            _state.value = LinuxState.Verifying
            withContext(Dispatchers.IO) { verifySha256(tar, sha) }
            _state.value = LinuxState.Extracting
            try {
                withContext(Dispatchers.IO) {
                    extractTarGz(tar, LinuxEnv.alpineRoot(appCtx))
                    writeResolvConf()
                }
            } finally {
                // Mesmo se a extração falhar, o .tar.gz (~10 MB) não fica
                // ocupando espaço para sempre em filesDir/linux/downloads.
                runCatching { tar.delete() }
            }
            _state.value = if (isReady()) LinuxState.Ready
            else LinuxState.Error("extração ok, mas /bin/sh não apareceu")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("VsCodeMobile", "linux setup", e)
            _state.value = LinuxState.Error(e.message ?: "falha no setup")
        }
    }

    private fun resolveAlpine(): Pair<String, String> {
        val text = httpGet(LinuxEnv.ALPINE_YAML_URL)
        val base = LinuxEnv.ALPINE_YAML_URL.substringBeforeLast('/')
        val lines = text.lines()
        for (i in lines.indices) {
            val l = lines[i].trim()
            if (l.startsWith("file:") && "minirootfs" in l && l.endsWith(".tar.gz")) {
                val file = l.substringAfter("file:").trim()
                val sha = lines.drop(i + 1)
                    .firstOrNull { it.trim().startsWith("sha256:") }
                    ?.substringAfter("sha256:")?.trim()
                    ?: throw IllegalStateException("sha256 ausente no yaml")
                return "$base/$file" to sha
            }
        }
        throw IllegalStateException("minirootfs não encontrado no yaml")
    }

    private fun httpGet(url: String): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private suspend fun downloadTo(url: String, dest: File, onProgress: (Long, Long) -> Unit) {
        withContext(Dispatchers.IO) {
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 60_000
            }
            try {
                if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
                val total = c.contentLengthLong
                var done = 0L
                c.inputStream.use { input ->
                    dest.outputStream().use { output ->
                        val buf = ByteArray(32 * 1024)
                        while (true) {
                            ensureActive()
                            val n = input.read(buf)
                            if (n == -1) break
                            output.write(buf, 0, n)
                            done += n
                            onProgress(done, total)
                        }
                    }
                }
            } finally {
                c.disconnect()
            }
        }
    }

    private fun verifySha256(file: File, expectedHex: String) {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n == -1) break
                md.update(buf, 0, n)
            }
        }
        val actual = md.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expectedHex.trim().split("\\s+".toRegex()).first(), ignoreCase = true)) {
            throw IllegalStateException("sha256 divergente (arquivo pode estar corrompido)")
        }
    }

    private fun extractTarGz(tarGz: File, dest: File) {
        dest.mkdirs()
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(tarGz.inputStream()))).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                var name = entry.name.trimStart('.', '/')
                require(!name.contains("..")) { "entrada insegura: ${entry.name}" }
                if (name.isNotEmpty()) {
                    val out = File(dest, name)
                    when {
                        entry.isSymbolicLink -> {
                            runCatching { out.delete() }
                            out.parentFile?.mkdirs()
                            Os.symlink(entry.linkName, out.absolutePath)
                        }
                        entry.isDirectory -> out.mkdirs()
                        entry.isFile -> {
                            out.parentFile?.mkdirs()
                            out.outputStream().use { o -> tar.copyTo(o) }
                            if (entry.mode and 0b001001001 != 0) out.setExecutable(true)
                        }
                    }
                }
                entry = tar.nextTarEntry
            }
        }
    }

    private fun writeResolvConf() {
        val resolv = File(LinuxEnv.alpineRoot(appCtx), "etc/resolv.conf")
        resolv.parentFile?.mkdirs()
        resolv.writeText("nameserver 8.8.8.8\nnameserver 8.4.4.8\n")
    }
}
