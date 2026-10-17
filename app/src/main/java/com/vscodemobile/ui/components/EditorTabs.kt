package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.Codicon
import com.vscodemobile.ui.theme.CodiconIcon
import com.vscodemobile.ui.theme.DarkModern
import com.vscodemobile.ui.theme.LightModern
import com.vscodemobile.ui.theme.LocalVsCodeColors
import com.vscodemobile.ui.theme.VsCodeTheme

data class EditorTab(
    val id: String,
    val name: String,
    val isDirty: Boolean
)

/**
 * Abas do editor: LazyRow, borda superior de ativo (accent), ponto de modificado,
 * área de fechar de 40x40dp. Como tab.activeBorderTop do VS Code.
 */
@Composable
fun EditorTabs(
    tabs: List<EditorTab>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.tabBarBackground)
    ) {
        LazyRow(Modifier.fillMaxWidth()) {
            items(tabs, key = { it.id }) { tab ->
                EditorTabItem(
                    tab = tab,
                    active = tab.id == activeId,
                    onSelect = { onSelect(tab.id) },
                    onClose = { onClose(tab.id) }
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.tabBorder)
        )
    }
}

@Composable
private fun EditorTabItem(
    tab: EditorTab,
    active: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalVsCodeColors.current
    Row(
        Modifier
            .background(if (active) colors.tabActiveBackground else colors.tabInactiveBackground)
            .clickable { onSelect() }
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Box(
                Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .background(if (active) colors.tabActiveBorderTop else colors.tabInactiveBackground)
            )
            Text(
                tab.name,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (active) colors.tabActiveForeground else colors.tabInactiveForeground,
                modifier = Modifier.padding(vertical = 9.dp)
            )
        }
        // Área de fechar 40x40dp; ponto quando modificado (como no desktop)
        Box(
            Modifier
                .size(width = 40.dp, height = 40.dp)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            if (tab.isDirty) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (active) colors.tabActiveForeground else colors.tabInactiveForeground)
                )
            } else {
                CodiconIcon(
                    Codicon.Close,
                    tint = if (active) colors.tabActiveForeground else colors.tabInactiveForeground,
                    sizeSp = 16
                )
            }
        }
    }
}

private val previewTabs = listOf(
    EditorTab("1", "VsCodeApp.kt", isDirty = true),
    EditorTab("2", "ThemeManager.kt", isDirty = false),
    EditorTab("3", "main.kt", isDirty = false)
)

@Preview(name = "Tabs escuras 360dp", widthDp = 360, heightDp = 80, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewTabsDark() {
    VsCodeTheme(colors = DarkModern) {
        EditorTabs(tabs = previewTabs, activeId = "1", onSelect = {}, onClose = {})
    }
}

@Preview(name = "Tabs claras 360dp", widthDp = 360, heightDp = 80, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewTabsLight() {
    VsCodeTheme(colors = LightModern) {
        EditorTabs(tabs = previewTabs, activeId = "2", onSelect = {}, onClose = {})
    }
}

@Preview(name = "Tabs tablet", widthDp = 1280, heightDp = 80, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewTabsTablet() {
    VsCodeTheme(colors = DarkModern) {
        EditorTabs(tabs = previewTabs, activeId = "3", onSelect = {}, onClose = {})
    }
}
