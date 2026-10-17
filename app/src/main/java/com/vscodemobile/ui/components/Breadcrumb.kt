package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.DarkModern
import com.vscodemobile.ui.theme.LightModern
import com.vscodemobile.ui.theme.LocalVsCodeColors
import com.vscodemobile.ui.theme.VsCodeTheme

/**
 * Breadcrumb recolhível: mostra "… › pai › arquivo"; toque expande o caminho todo.
 */
@Composable
fun Breadcrumb(
    segments: List<String>,
    language: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    var expanded by remember { mutableStateOf(false) }
    val shown = if (expanded || segments.size <= 3) segments
    else listOf("…") + segments.takeLast(2)
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.editorBackground)
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            shown.joinToString(" › ").ifBlank { "untitled" },
            color = colors.lineNumber,
            fontSize = 11.sp,
            maxLines = if (expanded) 4 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(language, color = colors.lineNumber, fontSize = 11.sp)
    }
}

@Preview(name = "Breadcrumb escuro 360dp", widthDp = 360, heightDp = 60, showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun PreviewBreadcrumbDark() {
    VsCodeTheme(colors = DarkModern) {
        Breadcrumb(
            segments = listOf("workspace", "app", "src", "main", "VsCodeApp.kt"),
            language = "Kotlin"
        )
    }
}

@Preview(name = "Breadcrumb claro 360dp", widthDp = 360, heightDp = 60, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewBreadcrumbLight() {
    VsCodeTheme(colors = LightModern) {
        Breadcrumb(segments = listOf("workspace", "main.kt"), language = "Kotlin")
    }
}
