package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.Codicon
import com.vscodemobile.ui.theme.CodiconIcon
import com.vscodemobile.ui.theme.DarkModern
import com.vscodemobile.ui.theme.LightModern
import com.vscodemobile.ui.theme.LocalVsCodeColors
import com.vscodemobile.ui.theme.VsCodeTheme

data class PaletteCommand(val title: String, val hint: String? = null)
data class PaletteFile(val name: String, val detail: String, val id: String = "")

/** Fuzzy simples por subsequência, como o quick pick do VS Code. */
fun fuzzyMatch(query: String, target: String): Boolean {
    if (query.isBlank()) return true
    var qi = 0
    val q = query.lowercase()
    for (c in target.lowercase()) {
        if (c == q[qi]) {
            qi++
            if (qi == q.length) return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPaletteSheet(
    commands: List<PaletteCommand>,
    files: List<PaletteFile>,
    query: String,
    onQuery: (String) -> Unit,
    onCommand: (PaletteCommand) -> Unit,
    onFile: (PaletteFile) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalVsCodeColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sideBarBackground
    ) {
        CommandPaletteContent(
            commands = commands,
            files = files,
            query = query,
            onQuery = onQuery,
            onCommand = onCommand,
            onFile = onFile
        )
    }
}

@Composable
fun CommandPaletteContent(
    commands: List<PaletteCommand>,
    files: List<PaletteFile>,
    query: String,
    onQuery: (String) -> Unit,
    onCommand: (PaletteCommand) -> Unit,
    onFile: (PaletteFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    val shownCommands = commands.filter {
        fuzzyMatch(query.trimStart('>', ' '), it.title)
    }.take(8)
    val shownFiles = files.filter { fuzzyMatch(query, it.name) }.take(12)
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.sideBarBackground)
            .padding(12.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = colors.sideBarForeground),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.inputBackground, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .padding(10.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) Text(
                    "> Type a file name or command",
                    fontSize = 14.sp,
                    color = colors.inputPlaceholder
                )
                inner()
            }
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(shownCommands) { cmd ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCommand(cmd) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cmd.title, fontSize = 13.sp, color = colors.sideBarForeground, modifier = Modifier.weight(1f))
                    cmd.hint?.let { Text(it, fontSize = 11.sp, color = colors.inputPlaceholder) }
                }
            }
            items(shownFiles) { f ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onFile(f) }
                        .padding(vertical = 9.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CodiconIcon(Codicon.GoToFile, tint = colors.inputPlaceholder, sizeSp = 16)
                    Spacer(Modifier.height(0.dp))
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(f.name, fontSize = 13.sp, color = colors.sideBarForeground)
                        Text(f.detail, fontSize = 11.sp, color = colors.inputPlaceholder)
                    }
                }
            }
        }
    }
}

private val previewCommands = listOf(
    PaletteCommand("> File: Save", "Ctrl+S"),
    PaletteCommand("> Terminal: Open in Termux"),
    PaletteCommand("> Preferences: Color Theme")
)
private val previewFiles = listOf(
    PaletteFile("VsCodeApp.kt", "app › ui"),
    PaletteFile("ThemeManager.kt", "app › editor"),
    PaletteFile("main.kt", "workspace")
)

@Preview(name = "Palette escura 360dp", widthDp = 360, heightDp = 500, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewPaletteDark() {
    VsCodeTheme(colors = DarkModern) {
        Surface {
            CommandPaletteContent(commands = previewCommands, files = previewFiles, query = ">", onQuery = {}, onCommand = {}, onFile = {})
        }
    }
}

@Preview(name = "Palette clara 360dp", widthDp = 360, heightDp = 500, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewPaletteLight() {
    VsCodeTheme(colors = LightModern) {
        Surface {
            CommandPaletteContent(commands = previewCommands, files = previewFiles, query = ">", onQuery = {}, onCommand = {}, onFile = {})
        }
    }
}
