package com.vscodemobile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.R

/**
 * Codicons (microsoft/vscode-codicons, CC-BY-4.0) como fonte.
 * Codepoints de codicon.csv @0.0.35 — mesma versão do ttf em res/font.
 */
val CodiconFont = FontFamily(Font(R.font.codicon))

object Codicon {
    const val Files = "\uEAF0"
    const val Search = "\uEA6D"
    const val SourceControl = "\uEA68"
    const val Debug = "\uEAD8"
    const val DebugAlt = "\uEB91"
    const val DebugConsole = "\uEB9B"
    const val DebugStart = "\uEAD3"
    const val DebugStop = "\uEAD7"
    const val Book = "\uEA70"
    const val History = "\uEA82"
    const val Extensions = "\uEAE6"
    const val Terminal = "\uEA85"
    const val SettingsGear = "\uEB51"
    const val Close = "\uEA76"
    const val ChromeClose = "\uEAB8"
    const val Add = "\uEA60"
    const val Refresh = "\uEB37"
    const val ChevronRight = "\uEAB6"
    const val ChevronDown = "\uEAB4"
    const val ArrowLeft = "\uEA9B"
    const val ArrowRight = "\uEA9C"
    const val ArrowUp = "\uEAA1"
    const val ArrowDown = "\uEA9A"
    const val LayoutPanel = "\uEBF2"
    const val LinkExternal = "\uEB14"
    const val ScreenFull = "\uEB4C"
    const val Error = "\uEA87"
    const val Warning = "\uEA6C"
    const val Bell = "\uEAA2"
    const val Folder = "\uEA83"
    const val FolderOpened = "\uEAF7"
    const val File = "\uEA7B"
    const val FileCode = "\uEAE9"
    const val NewFile = "\uEA7F"
    const val NewFolder = "\uEA80"
    const val Check = "\uEAB2"
    const val Sync = "\uEA77"
    const val Play = "\uEB2C"
    const val Trash = "\uEA81"
    const val GoToFile = "\uEA94"
    const val Ellipsis = "\uEA7C"
    const val Menu = "\uEB94"
    const val Save = "\uEB4B"
    const val Remote = "\uEB3A"
    const val Output = "\uEB9D"
    const val ClearAll = "\uEABF"
    const val Filter = "\uEAF1"
    const val GitCommit = "\uEAFC"
    const val Replace = "\uEB3D"
    const val ReplaceAll = "\uEB3C"
    const val ListTree = "\uEB86"
    const val ListFlat = "\uEB84"
    const val CollapseAll = "\uEAC5"
    const val ExpandAll = "\uEB95"

    // Codepoints oficiais do vscode-codicons (src/template/mapping.json).
    const val Info = "\uEA74"
    const val Paintcan = "\uEB2A"
    const val TextSize = "\uEB69"
    const val Remove = "\uEB3B"
    const val Package = "\uEB29"
    const val CloudDownload = "\uEAC2"
    const val Versions = "\uEB78"
    const val GitHub = "\uEA84"

    val all: List<Pair<String, String>> = listOf(
        "files" to Files, "search" to Search, "source-control" to SourceControl,
        "debug" to Debug, "debug-start" to DebugStart, "debug-stop" to DebugStop,
        "extensions" to Extensions, "terminal" to Terminal, "gear" to SettingsGear,
        "close" to Close, "add" to Add, "refresh" to Refresh,
        "chevron-right" to ChevronRight, "chevron-down" to ChevronDown,
        "error" to Error, "warning" to Warning, "bell" to Bell,
        "folder" to Folder, "folder-opened" to FolderOpened,
        "file" to File, "file-code" to FileCode,
        "new-file" to NewFile, "new-folder" to NewFolder,
        "check" to Check, "sync" to Sync, "play" to Play, "trash" to Trash,
        "go-to-file" to GoToFile, "ellipsis" to Ellipsis, "menu" to Menu,
        "save" to Save, "remote" to Remote, "output" to Output,
        "clear-all" to ClearAll, "filter" to Filter, "git-commit" to GitCommit,
        "replace" to Replace, "replace-all" to ReplaceAll,
        "list-tree" to ListTree, "list-flat" to ListFlat,
        "collapse-all" to CollapseAll, "expand-all" to ExpandAll,
        "debug-alt" to DebugAlt, "debug-console" to DebugConsole,
        "book" to Book, "history" to History,
        "arrow-left" to ArrowLeft, "arrow-right" to ArrowRight,
        "arrow-up" to ArrowUp, "arrow-down" to ArrowDown,
        "layout-panel" to LayoutPanel, "link-external" to LinkExternal,
        "screen-full" to ScreenFull
    )
}

@Composable
fun CodiconIcon(
    code: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    sizeSp: Int = 20
) {
    Text(
        text = code,
        fontFamily = CodiconFont,
        fontSize = sizeSp.sp,
        color = tint,
        modifier = modifier
    )
}

@Preview(name = "Codicons escuro", widthDp = 360, heightDp = 700, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewCodiconsDark() {
    CodiconGrid(background = Color(0xFF181818), foreground = Color(0xFFCCCCCC))
}

@Preview(name = "Codicons claro", widthDp = 360, heightDp = 700, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewCodiconsLight() {
    CodiconGrid(background = Color(0xFFF8F8F8), foreground = Color(0xFF3B3B3B))
}

@Composable
private fun CodiconGrid(background: Color, foreground: Color) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.background(background).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Codicon.all) { (name, code) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    CodiconIcon(code, tint = foreground, sizeSp = 22)
                }
                Text(name, fontSize = 9.sp, color = foreground.copy(alpha = 0.7f))
            }
        }
    }
}
