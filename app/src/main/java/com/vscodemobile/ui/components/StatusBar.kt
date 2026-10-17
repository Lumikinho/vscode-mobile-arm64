package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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

/**
 * Status bar de 24dp, fundo neutro com borda superior (Modern) — nunca bloco cheio.
 * Informação à esquerda, configuração rolável; sino com área de 48dp.
 */
@Composable
fun StatusBar(
    branch: String,
    errorCount: Int,
    warningCount: Int,
    lineCol: String,
    language: String,
    onTerminalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.statusBackground)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.statusBorder)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodiconIcon(Codicon.Remote, tint = colors.statusForeground, sizeSp = 12)
            Spacer(Modifier.width(4.dp))
            Text(branch, color = colors.statusForeground, fontSize = 11.sp)
            Spacer(Modifier.width(10.dp))
            StatusCount(Codicon.Error, errorCount, colors.error)
            Spacer(Modifier.width(6.dp))
            StatusCount(Codicon.Warning, warningCount, colors.warning)
            Spacer(Modifier.width(10.dp))
            Text(lineCol, color = colors.statusForeground, fontSize = 11.sp)
            Spacer(Modifier.width(10.dp))
            Text("Spaces: 4", color = colors.statusForeground, fontSize = 11.sp)
            Spacer(Modifier.width(10.dp))
            Text("UTF-8", color = colors.statusForeground, fontSize = 11.sp)
            Spacer(Modifier.width(10.dp))
            Text(language, color = colors.statusForeground, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(width = 48.dp, height = 24.dp)
                    .clickable { onTerminalClick() },
                contentAlignment = Alignment.CenterEnd
            ) {
                CodiconIcon(Codicon.Bell, tint = colors.statusForeground, sizeSp = 13)
            }
        }
    }
}

@Composable
private fun StatusCount(icon: String, count: Int, tint: androidx.compose.ui.graphics.Color) {
    val colors = LocalVsCodeColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        CodiconIcon(icon, tint = tint, sizeSp = 12)
        Spacer(Modifier.width(2.dp))
        Text("$count", color = colors.statusForeground, fontSize = 11.sp)
    }
}

@Preview(name = "StatusBar escura 360dp", widthDp = 360, heightDp = 40, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewStatusDark() {
    VsCodeTheme(colors = DarkModern) {
        StatusBar(
            branch = "main*", errorCount = 2, warningCount = 1,
            lineCol = "Ln 42, Col 17", language = "Kotlin", onTerminalClick = {}
        )
    }
}

@Preview(name = "StatusBar clara 360dp", widthDp = 360, heightDp = 40, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewStatusLight() {
    VsCodeTheme(colors = LightModern) {
        StatusBar(
            branch = "main", errorCount = 0, warningCount = 0,
            lineCol = "Ln 1, Col 1", language = "Kotlin", onTerminalClick = {}
        )
    }
}
