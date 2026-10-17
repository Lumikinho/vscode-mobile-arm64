package com.vscodemobile.ui.plugins

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebView para plugins.
 *
 * Endurecimento (o conteudo vem da rede):
 * - sem acesso a arquivo nem a content:// — o plugin nao le o filesystem
 * - sem URL universal / file-URL cruzando origem
 * - sem janelas multiplas nem popups (o plugin nao abre outra aba)
 * - sem cache e sem cookies persistidos entre sessoes
 * - navegacao presa a origem: um link para fora nao troca a view
 * - a view e destruida ao sair da composicao, junto com a ponte
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PluginWebView(
    plugin: PluginSource,
    bridge: PluginBridge,
    onError: (String) -> Unit = {},
    onPageStarted: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val holder = remember(plugin.id) { PluginWebViewHolder(plugin, bridge, onError, onPageStarted) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                holder.attach(this)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    // Nada de disco/arquivo: o plugin é conteúdo de rede.
                    allowFileAccess = false
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    domStorageEnabled = true
                    cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                    userAgentString = "VSCododeMobile-Plugin/1.0"
                }
                isVerticalScrollBarEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?, request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return true
                        // Preso a origem: link externo abre no browser do sistema.
                        if (url.startsWith(plugin.origin) || url.startsWith("about:")) return false
                        return true
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onPageStarted(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageStarted(false)
                        // Injeta a ponte para páginas que ignoram o script injetado.
                        view?.evaluateJavascript(BOOTSTRAP, null)
                    }

                    override fun onReceivedError(
                        view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError
                    ) {
                        if (request?.isForMainFrame == true) {
                            onError(error.description?.toString() ?: "falha ao carregar")
                        }
                    }
                }
                loadUrl(plugin.url)
            }
        },
        modifier = modifier,
        onRelease = { holder.detach() }
    )

    DisposableEffect(plugin.id) {
        onDispose { holder.dispose() }
    }
}

/**
 * Instala/remove a ponte no WebView. [PluginBridge] vira objeto JavaScript
 * `vscodePlugin`; o script de bootstrap cria o objeto `vscode` que o plugin
 * espera encontrar.
 */
private class PluginWebViewHolder(
    val plugin: PluginSource,
    val bridge: PluginBridge,
    val onError: (String) -> Unit,
    val onPageStarted: (Boolean) -> Unit
) {
    private var web: WebView? = null

    fun attach(view: WebView) {
        web = view
        view.removeJavascriptInterface("vscodePlugin")
        view.addJavascriptInterface(bridge, "vscodePlugin")
        view.evaluateJavascript(BOOTSTRAP, null)
    }

    fun detach() {
        web?.removeJavascriptInterface("vscodePlugin")
        web = null
    }

    fun dispose() {
        // destroy() exige a instancia viva: soltar a referencia antes deixa a
        // WebView registrada na activity e vaza a cada abertura do plugin.
        web?.removeJavascriptInterface("vscodePlugin")
        web?.destroy()
        web = null
    }
}

/** Cria o objeto `vscode` que o plugin usa. Injetado em toda navegação. */
private val BOOTSTRAP = """
(function () {
  if (window.vscode) return;
  var b = window.vscodePlugin;
  window.vscode = {
    apiVersion: function () { return b.version(); },
    commands: {
      all: function () { return JSON.parse(b.commands()); },
      execute: function (id, args) { return JSON.parse(b.execute(id, JSON.stringify(args || null))); }
    },
    workspace: {
      activeDocument: function () { return JSON.parse(b.activeDocument()); }
    },
    window: { showMessage: function (t) { b.showMessage(t); } },
    log: {
      info: function (m) { b.log('info', m); },
      warn: function (m) { b.log('warn', m); },
      error: function (m) { b.log('error', m); }
    }
  };
  window.dispatchEvent(new CustomEvent('vscode-ready', { detail: window.vscode }));
})();
""".trimIndent()

private const val TAG = "VsCodeMobile/plugin"