package com.vscodemobile.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.Codicon
import com.vscodemobile.ui.theme.CodiconIcon
import com.vscodemobile.ui.theme.LocalVsCodeColors

/**
 * Tela de configurações.
 *
 * Vai num bottom sheet como as outras views em vez de tela cheia: no celular
 * o editor continua visível atrás, que é o que se espera de Preferências.
 */
@Composable
fun SettingsSheet(
    versionName: String,
    versionCode: Long,
    updateState: UpdateState,
    onCheckUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
    editorFontSp: Int,
    onEditorFontSp: (Int) -> Unit,
    terminalFontSp: Int,
    onTerminalFontSp: (Int) -> Unit,
    onColorTheme: () -> Unit,
    onManagePlugins: () -> Unit,
    onUserland: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodiconIcon(Codicon.SettingsGear, tint = colors.sideBarForeground, sizeSp = 16)
            Spacer(Modifier.width(8.dp))
            Text(
                "CONFIGURAÇÕES",
                color = colors.sectionHeaderForeground,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp).semantics { contentDescription = "Fechar" }
            ) {
                CodiconIcon(Codicon.Close, tint = colors.panelTabInactiveForeground, sizeSp = 18)
            }
        }

        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            SectionHeader("GERAL")
            LinkRow(Codicon.Paintcan, "Tema de cores", "Alterar", onColorTheme)
            StepperRow(
                icon = Codicon.TextSize,
                title = "Fonte do editor",
                value = "$editorFontSp sp",
                onDecrement = { onEditorFontSp(editorFontSp - 1) },
                onIncrement = { onEditorFontSp(editorFontSp + 1) }
            )
            StepperRow(
                icon = Codicon.Terminal,
                title = "Fonte do terminal",
                value = "$terminalFontSp sp",
                onDecrement = { onTerminalFontSp(terminalFontSp - 1) },
                onIncrement = { onTerminalFontSp(terminalFontSp + 1) }
            )

            SectionHeader("AMBIENTE")
            LinkRow(Codicon.Package, "Termux (userland)", "Instalar / gerenciar", onUserland)
            LinkRow(Codicon.Extensions, "Plugins WebView", "Gerenciar", onManagePlugins)

            SectionHeader("SOBRE")
            AboutSection(
                versionName = versionName,
                versionCode = versionCode,
                updateState = updateState,
                onCheckUpdate = onCheckUpdate,
                onOpenUrl = onOpenUrl
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val colors = LocalVsCodeColors.current
    Text(
        text,
        color = colors.sectionHeaderForeground,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun LinkRow(icon: String, title: String, value: String, onClick: () -> Unit) {
    val colors = LocalVsCodeColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CodiconIcon(icon, tint = colors.sideBarForeground, sizeSp = 16)
        Spacer(Modifier.width(12.dp))
        Text(title, color = colors.sideBarForeground, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = colors.inputPlaceholder, fontSize = 12.sp)
    }
}

@Composable
private fun StepperRow(
    icon: String,
    title: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val colors = LocalVsCodeColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CodiconIcon(icon, tint = colors.sideBarForeground, sizeSp = 16)
        Spacer(Modifier.width(12.dp))
        Text(title, color = colors.sideBarForeground, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = colors.inputPlaceholder, fontSize = 12.sp)
        IconButton(
            onClick = onDecrement,
            modifier = Modifier.size(36.dp).semantics { contentDescription = "Diminuir $title" }
        ) {
            CodiconIcon(Codicon.Remove, tint = colors.sideBarForeground, sizeSp = 15)
        }
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(36.dp).semantics { contentDescription = "Aumentar $title" }
        ) {
            CodiconIcon(Codicon.Add, tint = colors.sideBarForeground, sizeSp = 15)
        }
    }
}

/**
 * Versão instalada e consulta da release do GitHub.
 *
 * A versão aparece sempre, mesmo sem rede: a resposta para "qual versão eu
 * tenho" não pode depender de a API responder.
 */
@Composable
private fun AboutSection(
    versionName: String,
    versionCode: Long,
    updateState: UpdateState,
    onCheckUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val colors = LocalVsCodeColors.current
    val context = LocalContext.current

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CodiconIcon(Codicon.Info, tint = colors.sideBarForeground, sizeSp = 16)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Versão", color = colors.sideBarForeground, fontSize = 13.sp)
                Text(
                    "$versionName (build $versionCode)",
                    color = colors.inputPlaceholder,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        when (updateState) {
            UpdateState.Checking -> {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            is UpdateState.Available -> {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B2E22), RoundedCornerShape(4.dp))
                        .clickable { onOpenUrl(updateState.url) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CodiconIcon(Codicon.CloudDownload, tint = Color(0xFF4EC9B0), sizeSp = 16)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Versão ${updateState.latest} disponível",
                            color = Color(0xFF4EC9B0),
                            fontSize = 13.sp
                        )
                        Text(
                            "Toque para abrir a release",
                            color = colors.inputPlaceholder,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            is UpdateState.UpToDate -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Atualizado — última release ${updateState.latest}",
                    color = colors.inputPlaceholder,
                    fontSize = 12.sp
                )
            }

            is UpdateState.Failed -> {
                Spacer(Modifier.height(8.dp))
                Text(updateState.reason, color = Color(0xFFF85149), fontSize = 12.sp)
            }

            UpdateState.Idle -> Unit
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (updateState !is UpdateState.Checking) {
                TextButton(onClick = onCheckUpdate) { Text("Verificar atualizações") }
                TextButton(
                    onClick = {
                        val url = "https://github.com/${UpdateChecker.DEFAULT_REPO}/releases"
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                ) { Text("Releases") }
            }
        }
    }
}
