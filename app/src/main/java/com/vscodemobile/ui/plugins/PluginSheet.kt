package com.vscodemobile.ui.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.Codicon
import com.vscodemobile.ui.theme.CodiconIcon
import com.vscodemobile.ui.theme.LocalVsCodeColors

/**
 * Conteúdo do plugin no bottom sheet: cabeçalho com título + origem (para
 * ficar claro de onde vem o conteúdo), estado de carregamento e o WebView.
 */
@Composable
fun PluginSheetContent(
    plugin: PluginSource,
    bridge: PluginBridge,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val colors = LocalVsCodeColors.current
    var loading by remember(plugin.id) { mutableStateOf(plugin.isWebUrl) }
    var error by remember(plugin.id) { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodiconIcon(plugin.icon, tint = colors.accent, sizeSp = 16)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    plugin.title,
                    color = colors.sideBarForeground,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    plugin.origin,
                    color = colors.inputPlaceholder,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        }
        if (error != null) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CodiconIcon(Codicon.Warning, tint = Color(0xFFF85149), sizeSp = 28)
                Spacer(Modifier.height(8.dp))
                Text(
                    error!!,
                    color = Color(0xFFF85149),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { error = null; loading = true }) { Text("Tentar de novo") }
            }
        } else {
            PluginWebView(
                plugin = plugin,
                bridge = bridge,
                onError = { error = it },
                onPageStarted = { loading = it },
                modifier = Modifier.fillMaxWidth().height(380.dp)
            )
        }
    }
}

/**
 * Gerenciador: lista os plugins de URL e permite adicionar/remover.
 *
 * Fica atrás de `View: Manage WebView Plugins` na palette, e não na barra de
 * navegação — são configuração, não destino de uso diário.
 */
@Composable
fun PluginManagerSheet(
    plugins: List<PluginSource>,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onToggle: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PLUGINS WEBVIEW",
                color = colors.sectionHeaderForeground,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { showAdd = true },
                modifier = Modifier.size(40.dp).semantics { contentDescription = "Adicionar plugin" }
            ) {
                CodiconIcon(Codicon.Add, tint = colors.sideBarForeground, sizeSp = 18)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp).semantics { contentDescription = "Fechar" }
            ) {
                CodiconIcon(Codicon.Close, tint = colors.panelTabInactiveForeground, sizeSp = 18)
            }
        }

        if (plugins.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    "Nenhum plugin. Adicione a URL de uma página HTML.",
                    color = colors.inputPlaceholder,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "O conteúdo roda com JavaScript e pode executar comandos do workbench. " +
                        "Use apenas URLs em que você confiar.",
                    color = colors.inputPlaceholder,
                    fontSize = 11.sp,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(plugins, key = { it.id }) { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(p.id) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CodiconIcon(
                            p.icon,
                            tint = if (p.enabled) colors.accent else colors.inputPlaceholder,
                            sizeSp = 16,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.title,
                                color = colors.sideBarForeground,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                p.origin,
                                color = colors.inputPlaceholder,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { onRemove(p.id) },
                            modifier = Modifier.size(36.dp).semantics {
                                contentDescription = "Remover ${p.title}"
                            }
                        ) {
                            CodiconIcon(Codicon.Trash, tint = colors.sideBarForeground, sizeSp = 15)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddPluginDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, url ->
                onAdd(title, url)
                showAdd = false
            }
        )
    }
}

@Composable
private fun AddPluginDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    val valid = url.startsWith("http://") || url.startsWith("https://")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar plugin WebView") },
        text = {
            Column {
                androidx.compose.foundation.text.BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            if (title.isEmpty()) {
                                Text("Nome (opcional)", fontSize = 14.sp, color = Color.Gray)
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            inner()
                        }
                    }
                )
                if (!valid) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "A URL precisa começar com http:// ou https://",
                        color = Color(0xFFF85149),
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "O conteúdo vai rodar com JavaScript e acesso a comandos do app.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(title, url) }
            ) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}