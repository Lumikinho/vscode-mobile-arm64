package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Title bar enxuta: menu, título central, busca. Borda inferior de 1dp, sem sombra.
 */
@Composable
fun TopBar(
    title: String,
    isDirty: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.titleBarBackground)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(48.dp)) {
                CodiconIcon(Codicon.Menu, tint = colors.titleBarForeground, sizeSp = 20)
            }
            Text(
                text = (if (isDirty) "● " else "") + title,
                color = colors.titleBarForeground,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            IconButton(onClick = onSearchClick, modifier = Modifier.size(48.dp)) {
                CodiconIcon(Codicon.Search, tint = colors.titleBarForeground, sizeSp = 20)
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

@Preview(name = "TopBar escura 360dp", widthDp = 360, heightDp = 80, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewTopBarDark() {
    VsCodeTheme(colors = DarkModern) {
        TopBar(title = "MainActivity.kt", isDirty = true, onMenuClick = {}, onSearchClick = {})
    }
}

@Preview(name = "TopBar clara 360dp", widthDp = 360, heightDp = 80, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewTopBarLight() {
    VsCodeTheme(colors = LightModern) {
        TopBar(title = "VsCodeApp.kt", isDirty = false, onMenuClick = {}, onSearchClick = {})
    }
}

@Preview(name = "TopBar tablet", widthDp = 1280, heightDp = 80, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewTopBarTablet() {
    VsCodeTheme(colors = DarkModern) {
        TopBar(title = "ThemeManager.kt", isDirty = false, onMenuClick = {}, onSearchClick = {})
    }
}
