package com.vscodemobile.ui.plugins

import android.content.Context
import com.vscodemobile.ui.theme.Codicon
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/**
 * Um plugin carregado de URL.
 *
 * ATENCAO - modelo de ameaca: o conteudo vem da rede e roda com JavaScript
 * ligado. Quem controla a URL controla codigo que executa dentro do app. Por
 * isso [PluginWebView] desliga acesso a arquivo e URL universal e trava a
 * navegacao na origem, mas a ponte de comandos continua sendo poder real:
 * so adicione URLs em que voce confie.
 */
data class PluginSource(
    val id: String,
    val title: String,
    val url: String,
    val icon: String = Codicon.Extensions,
    val enabled: Boolean = true
) {
    /** Origem legivel, para o cabecalho mostrar de onde veio o conteudo. */
    val origin: String
        get() = runCatching {
            val u = URI(url)
            val host = u.host ?: return@runCatching url
            val port = if (u.port > 0) ":${u.port}" else ""
            "${u.scheme ?: "https"}://$host$port"
        }.getOrDefault(url)

    val isWebUrl: Boolean
        get() = url.startsWith("http://") || url.startsWith("https://")

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("url", url)
        .put("icon", icon)
        .put("enabled", enabled)

    companion object {
        private const val PREFS = "vscodemobile.plugins"
        private const val KEY_LIST = "list"

        fun fromJson(o: JSONObject): PluginSource = PluginSource(
            id = o.optString("id").ifBlank { o.optString("url") },
            title = o.optString("title").ifBlank { o.optString("url") },
            url = o.optString("url"),
            icon = o.optString("icon").ifBlank { Codicon.Extensions },
            enabled = o.optBoolean("enabled", true)
        )

        fun load(context: Context): List<PluginSource> {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LIST, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            }.getOrDefault(emptyList())
        }

        fun save(context: Context, plugins: List<PluginSource>) {
            val arr = JSONArray()
            plugins.forEach { arr.put(it.toJson()) }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LIST, arr.toString()).apply()
        }
    }
}