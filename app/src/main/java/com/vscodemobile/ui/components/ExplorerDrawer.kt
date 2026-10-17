package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(),
    val sizeBytes: Long = 0L
)

/** Uma raiz do workspace (estilo multi-root do VS Code: workspace + pastas anexadas). */
data class ExplorerRoot(
    val key: String,
    val title: String,
    val badge: String? = null,
    val rootLabel: String,
    val nodes: List<FileNode>,
    val canNavigateUp: Boolean = false,
    val outlineText: String? = null,
    val emptyHint: String? = null,
    val closeable: Boolean = false
)

private val codeExtensions = setOf(
    "kt", "kts", "java", "js", "ts", "tsx", "py", "c", "h", "cpp",
    "cs", "go", "rs", "html", "css", "xml", "json", "sh", "gradle", "sql"
)

fun fileIconFor(name: String, isDirectory: Boolean, open: Boolean): String = when {
    isDirectory && open -> Codicon.FolderOpened
    isDirectory -> Codicon.Folder
    name.substringAfterLast('.', "").lowercase() in codeExtensions -> Codicon.FileCode
    else -> Codicon.File
}

/**
 * Explorer multi-root: o workspace mais as pastas locais anexadas na mesma
 * árvore, como Add Folder to Workspace. Recebe dados prontos — sem I/O aqui.
 */
@Composable
fun MultiRootExplorer(
    roots: List<ExplorerRoot>,
    expandedPaths: Set<String>,
    selectedPath: String?,
    onToggleDir: (String) -> Unit,
    onOpenNode: (FileNode) -> Unit,
    onNavigateUp: (String) -> Unit,
    onNewFile: (String) -> Unit,
    onNewFolder: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenFolder: () -> Unit,
    onCloseRoot: (String) -> Unit,
    showHidden: Boolean = false,
    onToggleHidden: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(colors.sideBarBackground)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "EXPLORER",
                color = colors.sectionHeaderForeground,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
                CodiconIcon(Codicon.Refresh, tint = colors.sideBarForeground, sizeSp = 18)
            }
            IconButton(onClick = onToggleHidden, modifier = Modifier.size(40.dp)) {
                CodiconIcon(
                    Codicon.Filter,
                    tint = if (showHidden) colors.accent else colors.sideBarForeground,
                    sizeSp = 18
                )
            }
            IconButton(onClick = onOpenFolder, modifier = Modifier.size(40.dp)) {
                CodiconIcon(Codicon.FolderOpened, tint = colors.sideBarForeground, sizeSp = 18)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            for (root in roots) {
                RootSection(
                    root = root,
                    expandedPaths = expandedPaths,
                    selectedPath = selectedPath,
                    onToggleDir = onToggleDir,
                    onOpenNode = onOpenNode,
                    onNavigateUp = { onNavigateUp(root.key) },
                    onNewFile = { onNewFile(root.key) },
                    onNewFolder = { onNewFolder(root.key) },
                    onClose = { onCloseRoot(root.key) }
                )
            }
        }
    }
}

@Composable
private fun RootSection(
    root: ExplorerRoot,
    expandedPaths: Set<String>,
    selectedPath: String?,
    onToggleDir: (String) -> Unit,
    onOpenNode: (FileNode) -> Unit,
    onNavigateUp: () -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalVsCodeColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            root.title,
            color = colors.sectionHeaderForeground,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        if (root.badge != null) {
            Text(root.badge, color = colors.accent, fontSize = 10.sp)
        }
        if (root.closeable) {
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                CodiconIcon(Codicon.Close, tint = colors.sideBarForeground, sizeSp = 15)
            }
        }
        IconButton(onClick = onNewFile, modifier = Modifier.size(32.dp)) {
            CodiconIcon(Codicon.NewFile, tint = colors.sideBarForeground, sizeSp = 16)
        }
        IconButton(onClick = onNewFolder, modifier = Modifier.size(32.dp)) {
            CodiconIcon(Codicon.NewFolder, tint = colors.sideBarForeground, sizeSp = 16)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "▸ ${root.rootLabel}",
            color = colors.sideBarForeground,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
    if (root.canNavigateUp) {
        Text(
            "‹ .. (voltar)",
            color = colors.sideBarForeground,
            fontSize = 12.sp,
            modifier = Modifier
                .clickable { onNavigateUp() }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
    if (root.nodes.isEmpty() && root.emptyHint != null) {
        Text(
            root.emptyHint,
            color = colors.inputPlaceholder,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    for (node in root.nodes) {
        FileTreeNode(
            node = node,
            depth = 0,
            expandedPaths = expandedPaths,
            selectedPath = selectedPath,
            onToggleDir = onToggleDir,
            onOpenFile = onOpenNode
        )
    }
    if (root.outlineText != null) {
        Text(
            "OUTLINE",
            color = colors.sectionHeaderForeground,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp)
        )
        Text(
            root.outlineText,
            color = colors.inputPlaceholder,
            fontSize = 11.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun FileTreeNode(
    node: FileNode,
    depth: Int,
    expandedPaths: Set<String>,
    selectedPath: String?,
    onToggleDir: (String) -> Unit,
    onOpenFile: (FileNode) -> Unit
) {
    val colors = LocalVsCodeColors.current
    val pad = (12 + depth * 14).dp
    if (node.isDirectory) {
        val open = node.path in expandedPaths
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onToggleDir(node.path) }
                .padding(start = pad, top = 6.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodiconIcon(
                if (open) Codicon.ChevronDown else Codicon.ChevronRight,
                tint = colors.sideBarForeground,
                sizeSp = 16
            )
            Spacer(Modifier.width(4.dp))
            CodiconIcon(
                fileIconFor(node.name, true, open),
                tint = colors.accent,
                sizeSp = 17
            )
            Spacer(Modifier.width(6.dp))
            Text(
                node.name,
                color = colors.sideBarForeground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (open) {
            for (child in node.children) {
                FileTreeNode(child, depth + 1, expandedPaths, selectedPath, onToggleDir, onOpenFile)
            }
        }
    } else {
        val selected = node.path == selectedPath
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (selected) colors.listSelectionBackground else colors.sideBarBackground)
                .clickable { onOpenFile(node) }
                .padding(start = pad + 22.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CodiconIcon(
                fileIconFor(node.name, false, false),
                tint = if (selected) colors.listSelectionForeground else colors.sideBarForeground,
                sizeSp = 16
            )
            Spacer(Modifier.width(6.dp))
            Text(
                node.name,
                color = if (selected) colors.listSelectionForeground else colors.sideBarForeground,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val previewTree = listOf(
    FileNode("app", "/w/app", true, listOf(
        FileNode("ui", "/w/app/ui", true, listOf(
            FileNode("VsCodeApp.kt", "/w/app/ui/VsCodeApp.kt", false, sizeBytes = 42000),
            FileNode("theme", "/w/app/ui/theme", true, emptyList())
        )),
        FileNode("MainActivity.kt", "/w/app/MainActivity.kt", false, sizeBytes = 1200)
    )),
    FileNode("main.kt", "/w/main.kt", false, sizeBytes = 300)
)

private val previewLocal = listOf(
    FileNode("bot", "content://x/bot", true, listOf(
        FileNode("main.py", "content://x/bot/main.py", false, sizeBytes = 800)
    )),
    FileNode("CHANGELOG.md", "content://x/CHANGELOG.md", false, sizeBytes = 461)
)

private fun previewRoots() = listOf(
    ExplorerRoot(
        key = "workspace",
        title = "WORKSPACE",
        rootLabel = "workspace",
        nodes = previewTree,
        outlineText = "VsCodeApp.kt — 42 KB"
    ),
    ExplorerRoot(
        key = "local",
        title = "BOT-TELEGRAM",
        badge = "LOCAL",
        rootLabel = "bot-telegram",
        nodes = previewLocal,
        closeable = true
    )
)

@Composable
private fun MultiRootPreviewContent() {
    MultiRootExplorer(
        roots = previewRoots(),
        expandedPaths = setOf("/w/app", "/w/app/ui", "content://x/bot"),
        selectedPath = "/w/app/ui/VsCodeApp.kt",
        onToggleDir = {},
        onOpenNode = {},
        onNavigateUp = {},
        onNewFile = {},
        onNewFolder = {},
        onRefresh = {},
        onOpenFolder = {},
        onCloseRoot = {}
    )
}

@Preview(name = "Explorer multi-root escuro", widthDp = 360, heightDp = 700, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewExplorerDark() {
    VsCodeTheme(colors = DarkModern) { MultiRootPreviewContent() }
}

@Preview(name = "Explorer multi-root claro", widthDp = 360, heightDp = 700, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewExplorerLight() {
    VsCodeTheme(colors = LightModern) { MultiRootPreviewContent() }
}
