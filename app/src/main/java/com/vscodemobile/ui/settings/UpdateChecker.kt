package com.vscodemobile.ui.settings

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Consulta a release mais recente no GitHub.
 *
 * O repositório é privado, então a API pública responde 404 tanto para "não
 * existe" quanto para "sem acesso". Isso não é erro do app: é a resposta
 * esperada, e a tela precisa dizer "não foi possível consultar" em vez de
 * fingir que está atualizado.
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState

    /** Versão do app é a mais recente conhecida. */
    data class UpToDate(val latest: String) : UpdateState

    data class Available(val latest: String, val url: String) : UpdateState

    data class Failed(val reason: String) : UpdateState
}

class UpdateChecker(
    private val repo: String = DEFAULT_REPO,
    private val fetchTag: suspend () -> Pair<String, String> = { fetchLatestRelease(repo) }
) {

    /**
     * Verifica sem bloquear e publica o resultado. Chamadas concorrentes
     * compartilham a mesma consulta: tocar duas vezes em "verificar" não deve
     * gerar duas requisições.
     */
    suspend fun check(current: AppVersion?): UpdateState {
        return try {
            val (tag, url) = fetchTag()
            when (AppVersion.isNewer(tag, current)) {
                true -> UpdateState.Available(AppVersion.parse(tag)?.toString() ?: tag, url)
                false -> UpdateState.UpToDate(AppVersion.parse(tag)?.toString() ?: tag)
                null -> UpdateState.Failed("tag de release em formato desconhecido: $tag")
            }
        } catch (e: Exception) {
            UpdateState.Failed(e.message ?: "sem conexão")
        }
    }

    companion object {
        const val DEFAULT_REPO = "Lumikinho/vscode-mobile-arm64"
        const val ENDPOINT = "https://api.github.com/repos/$DEFAULT_REPO/releases/latest"

        private const val TIMEOUT_MS = 15_000

        /** Devolve (tag_name, html_url) da release mais recente. */
        suspend fun fetchLatestRelease(repo: String): Pair<String, String> {
            val url = URL("$BASE$repo/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "VSCodeMobile-ARM64")
            }
            try {
                return when (conn.responseCode) {
                    in 200..299 -> {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val tag = JSONObject(body).optString("tag_name")
                        if (tag.isBlank()) throw IOException("release sem tag_name")
                        val page = JSONObject(body).optString("html_url").ifBlank {
                            "https://github.com/$repo/releases"
                        }
                        tag to page
                    }
                    // Repo sem releases, ou sem permissão de leitura. São casos
                    // distintos com o mesmo código, então a mensagem cobre os
                    // dois em vez de cravar um motivo errado.
                    403, 404 -> throw IOException(
                        "sem release publicada (ou repositório inacessível)"
                    )
                    else -> throw IOException("HTTP ${conn.responseCode}")
                }
            } finally {
                conn.disconnect()
            }
        }

        private const val BASE = "https://api.github.com/repos/"
    }
}
