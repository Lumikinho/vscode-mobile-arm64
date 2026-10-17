package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.Codicon
import com.vscodemobile.ui.theme.CodiconIcon
import com.vscodemobile.ui.theme.DarkModern
import com.vscodemobile.ui.theme.LightModern
import com.vscodemobile.ui.theme.LocalVsCodeColors
import com.vscodemobile.ui.theme.VsCodeTheme

data class MenuAction(val icon: String, val label: String, val onClick: () -> Unit)

/**
 * Menu vertical de cima para baixo (abre do hamburger da TopBar).
 * Destinos do registro + ações, com indicador à esquerda no ativo.
 */
@Composable
fun TopDropMenu(
    entries: List<NavEntry>,
    activeKey: String,
    onSelectEntry: (String) -> Unit,
    actions: List<MenuAction>,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.titleBarBackground)
    ) {
        for (entry in entries) {
            val selected = entry.key == activeKey
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onSelectEntry(entry.key) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(if (selected) colors.activityActiveBorder else colors.titleBarBackground)
                )
                Spacer(Modifier.width(14.dp))
                CodiconIcon(
                    entry.icon,
                    tint = if (selected) colors.activityForeground else colors.activityInactiveForeground,
                    sizeSp = 22
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    entry.label,
                    fontSize = 14.sp,
                    color = if (selected) colors.activityForeground else colors.activityInactiveForeground
                )
            }
        }
        HorizontalDivider(color = colors.titleBarBorder)
        for (action in actions) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { action.onClick() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(16.dp))
                CodiconIcon(action.icon, tint = colors.activityInactiveForeground, sizeSp = 20)
                Spacer(Modifier.width(14.dp))
                Text(action.label, fontSize = 14.sp, color = colors.activityInactiveForeground)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.titleBarBorder)
        )
    }
}

private val previewMenuEntries = listOf(
    NavEntry("explorer", Codicon.Files, "Explorer"),
    NavEntry("search", Codicon.Search, "Buscar"),
    NavEntry("scm", Codicon.SourceControl, "Git"),
    NavEntry("debug", Codicon.DebugAlt, "Executar"),
    NavEntry("extensions", Codicon.Extensions, "Extensões")
)

private val previewMenuActions = listOf(
    MenuAction(Codicon.Terminal, "Terminal", {}),
    MenuAction(Codicon.Search, "Command Palette", {}),
    MenuAction(Codicon.NewFolder, "Open Folder…", {})
)

@Preview(name = "Menu vertical escuro", widthDp = 360, heightDp = 560, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewMenuDark() {
    VsCodeTheme(colors = DarkModern) {
        TopDropMenu(
            entries = previewMenuEntries,
            activeKey = "explorer",
            onSelectEntry = {},
            actions = previewMenuActions
        )
    }
}

@Preview(name = "Menu vertical claro", widthDp = 360, heightDp = 560, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewMenuLight() {
    VsCodeTheme(colors = LightModern) {
        TopDropMenu(
            entries = previewMenuEntries,
            activeKey = "scm",
            onSelectEntry = {},
            actions = previewMenuActions
        )
    }
}
