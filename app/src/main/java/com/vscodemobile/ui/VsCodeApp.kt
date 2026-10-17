package com.vscodemobile.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
import com.vscodemobile.editor.EditorTheme
import com.vscodemobile.editor.SyntaxHighlighter
import com.vscodemobile.editor.ThemeManager
import com.vscodemobile.editor.toCompose
import com.vscodemobile.files.DocContent
import com.vscodemobile.files.DocumentUtils
import com.vscodemobile.files.FileUtils
import com.vscodemobile.termux.TermuxHelper
import com.vscodemobile.ui.components.AdaptiveBottomBar
import com.vscodemobile.ui.components.BottomBarState
import com.vscodemobile.ui.components.MAX_TERM_FONT_SP
import com.vscodemobile.ui.components.MIN_TERM_FONT_SP
import com.vscodemobile.ui.components.Breadcrumb
import com.vscodemobile.ui.components.CommandPaletteSheet
import com.vscodemobile.ui.components.EditKey
import com.vscodemobile.ui.components.NavEntry
import com.vscodemobile.ui.components.EditorSurface
import com.vscodemobile.ui.components.EditorTab
import com.vscodemobile.ui.components.EditorTabs
import com.vscodemobile.ui.components.ExplorerRoot
import com.vscodemobile.ui.components.MultiRootExplorer
import com.vscodemobile.ui.components.FileNode
import com.vscodemobile.ui.components.fuzzyMatch
import com.vscodemobile.ui.components.PaletteCommand
import com.vscodemobile.ui.components.PaletteFile
import com.vscodemobile.ui.components.ProblemsSheetContent
import com.vscodemobile.ui.components.ProblemItem
import com.vscodemobile.ui.components.ProblemSeverity
import com.vscodemobile.ui.components.StatusBar
import com.vscodemobile.ui.components.TopBar
import com.vscodemobile.ui.components.TopDropMenu
import com.vscodemobile.ui.components.MenuAction
import com.vscodemobile.ui.theme.Codicon
import com.vscodemobile.ui.theme.CodiconIcon
import com.vscodemobile.ui.theme.VsCodeTheme
import com.vscodemobile.ui.theme.vscodeColorsFor
import com.vscodemobile.linux.PrefixState
import com.vscodemobile.linux.TermuxEnv
import com.vscodemobile.linux.TermuxPrefix
import com.vscodemobile.ui.state.DocRef
import com.vscodemobile.ui.state.ExplorerViewModel
import com.vscodemobile.ui.state.LanguageServerManager
import com.vscodemobile.ui.terminal.PtySession
import com.vscodemobile.ui.terminal.TermSession
import com.vscodemobile.ui.terminal.TerminalScreen
import com.vscodemobile.ui.workbench.BuiltinPlugins
import com.vscodemobile.ui.plugins.PluginBridge
import com.vscodemobile.ui.plugins.PluginManagerSheet
import com.vscodemobile.ui.plugins.PluginSheetContent
import com.vscodemobile.ui.plugins.PluginSource
import com.vscodemobile.ui.plugins.SensitiveCommands
import com.vscodemobile.ui.workbench.CommandRegistry
import org.json.JSONObject
import com.vscodemobile.ui.workbench.QuickAccessProvider
import com.vscodemobile.ui.workbench.QuickAccessRegistry
import com.vscodemobile.ui.workbench.QuickPickItem
import com.vscodemobile.ui.workbench.ViewRegistry
import com.vscodemobile.ui.workbench.WorkbenchActions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlinx.coroutines.withContext

/** Containers embutidos que ocupam lugar fixo na bottom bar. */
private val BuiltinContainerIds = setOf(
    "workbench.view.explorer",
    "workbench.view.search",
    "workbench.view.scm",
    "workbench.view.debug",
    "workbench.view.extensions"
)

private fun BuiltinContainerLabel(id: String): String = when (id) {
    "workbench.view.explorer" -> "Explorer"
    "workbench.view.search" -> "Buscar"
    "workbench.view.scm" -> "Git"
    "workbench.view.debug" -> "Executar"
    "workbench.view.extensions" -> "Extensões"
    else -> id
}

/**
 * Identidade de uma view aberta.
 *
 * Antes era um enum fechado de 5 valores, o que descartava qualquer container
 * que um plugin registrasse ([navEntries] fazia mapNotNull com `when`).
 * Agora é um id: embutidos usam [BUILTIN], plugins usam o id do container.
 */
data class ActiveView(val id: String, val plugin: PluginSource? = null) {
    val isPlugin: Boolean get() = plugin != null

    companion object {
        val EXPLORER = ActiveView("workbench.view.explorer")
        val SEARCH = ActiveView("workbench.view.search")
        val GIT = ActiveView("workbench.view.scm")
        val RUN = ActiveView("workbench.view.debug")
        val EXT = ActiveView("workbench.view.extensions")

        val BUILTIN = setOf(EXPLORER, SEARCH, GIT, RUN, EXT)
    }
}

private const val KEY_SAF_ROOT = "saf_root"
private const val KEY_SAF_NAME = "saf_name"
private const val KEY_TERM_FONT = "term_font_sp"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VsCodeApp(
    workspace: File,
    filesDirPath: String,
    initialTheme: EditorTheme,
    onThemeChange: (EditorTheme) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var theme by remember { mutableStateOf(initialTheme) }
    var chrome = ThemeManager.chromeOf(theme)
    var activeView by remember { mutableStateOf(ActiveView.EXPLORER) }
    var sheetVisible by remember { mutableStateOf(false) }
    var panelTab by remember { mutableStateOf(0) } // 0 problems 1 output 2 terminal
    var showPalette by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }

    var currentDir by remember { mutableStateOf(workspace) }
    var explorerTick by remember { mutableStateOf(0) }
    val openFiles = remember { mutableStateListOf<DocRef>() }
    var currentFile by remember { mutableStateOf<DocRef?>(null) }
    var rawText by remember { mutableStateOf("") }
    var dirty by remember { mutableStateOf(false) }
    var fieldState by remember { mutableStateOf(TextFieldValue("")) }
    var editorFontSize by remember { mutableStateOf(13) }
    var paletteQuery by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val outputLines = remember { mutableStateListOf("VSCode Mobile — pronto.", "Terminal embutido na aba TERMINAL.") }
    var loadGen by remember { mutableIntStateOf(0) }

    // Raiz local via SAF (pasta do aparelho), com permissão persistida
    val prefs = remember {
        context.getSharedPreferences(ThemeManager.PREFS, Context.MODE_PRIVATE)
    }
    var safRoot by remember { mutableStateOf<Uri?>(null) }
    var safRootName by remember { mutableStateOf("local") }
    var newTarget by remember { mutableStateOf("workspace") }

    // Listagem SAF fora da composição: sobrevive a fechar palette/sheet
    val explorerVm: ExplorerViewModel = viewModel()
    val safNodes by explorerVm.nodes.collectAsStateWithLifecycle()
    val showHidden by explorerVm.showHidden.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val keyboard = LocalSoftwareKeyboardController.current
    val undoStack = remember { ArrayDeque<String>() }
    val redoStack = remember { ArrayDeque<String>() }
    var lastPushTime by remember { mutableLongStateOf(0L) }
    /** Linha (1-based) para onde o cursor vai quando o texto do doc carregar. */
    var pendingCaretLine by remember { mutableIntStateOf(0) }
    /**
     * Se não-nulo, o documento ativo NÃO PODE ser salvo, e o valor é o motivo.
     *
     * Cobre os dois casos em que o buffer da UI não representa o disco: a
     * leitura falhou, ou o arquivo é binário (abriria como texto e salvar
     * gravaria o conteúdo corrompido por cima do original).
     */
    var loadBlock by remember { mutableStateOf<String?>(null) }
    var ctrlLatched by remember { mutableStateOf(false) }
    var altLatched by remember { mutableStateOf(false) }
    var terminalPinned by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var sheetView by remember { mutableStateOf<ActiveView?>(null) }
    var showPluginManager by remember { mutableStateOf(false) }

    /** Plugins WebView registrados (URL). Sobrevivem ao restart. */
    var plugins by remember {
        mutableStateOf(PluginSource.load(context.applicationContext))
    }

    var ptyLibBroken by remember { mutableStateOf(false) }

    /** Garante libtermux.so (usada pelas sessões PTY). */
    fun ensurePtyLib(): Boolean {
        if (ptyLibBroken) return false
        return try {
            System.loadLibrary("termux")
            true
        } catch (e: UnsatisfiedLinkError) {
            ptyLibBroken = true
            Toast.makeText(context, "PTY indisponível neste aparelho", Toast.LENGTH_LONG).show()
            false
        }
    }

    // Userland do Termux instalado no app. Enquanto nao houver, o terminal
    // cai no sh do sistema (que e o toybox do Android: existe, mas nao tem
    // coreutils, apt nem nada de programacao).
    val termux = remember { TermuxPrefix(context.applicationContext) }
    val prefixState by termux.state.collectAsStateWithLifecycle()
    var showLinuxSetup by remember { mutableStateOf(false) }

    /**
     * Fábrica da sessão PTY padrão: bash do Termux quando instalado, senão o
     * sh do sistema. Não há proot: o binário roda direto, e é por isso que o
     * targetSdk precisa ficar em 28.
     */
    fun makePtyShell(label: String = "TERM"): PtySession {
        val bash = termux.shell()
        return if (bash.canExecute()) {
            PtySession(
                context.applicationContext,
                bash.absolutePath,
                filesDirPath,
                // -i e não -l: o login tentaria ler o /etc/profile compilado no
                // binário do Termux. O ~/.bashrc gerado cobre esse papel.
                arrayOf("bash", "-i"),
                termux.env(filesDirPath),
                label
            )
        } else {
            PtySession(
                context.applicationContext,
                "/system/bin/sh",
                filesDirPath,
                arrayOf("sh", "-i"),
                arrayOf(
                    "TERM=xterm-256color",
                    "HOME=$filesDirPath",
                    "PATH=/system/bin:/vendor/bin",
                    "TMPDIR=${context.cacheDir.absolutePath}",
                    "PS1=$ "
                ),
                label
            )
        }
    }

    val shells = remember {
        mutableStateListOf<TermSession>().also {
            if (ensurePtyLib()) it.add(makePtyShell())
        }
    }

    // Cliente LSP (diagnósticos JS via socket localhost; ver LanguageServerManager)
    val lspManager = remember(workspace) { LanguageServerManager(workspace) }
    val lspDiags by lspManager.diagnostics.collectAsState()
    LaunchedEffect(Unit) {
        lspManager.onConnectionFailed = { lang ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Servidor $lang não encontrado (inicie no Termux)"
                )
            }
        }
    }
    var activeShellIdx by remember { mutableStateOf(0) }
    var termFullscreen by remember { mutableStateOf(false) }
    var terminalFontSp by remember {
        mutableStateOf(prefs.getInt(KEY_TERM_FONT, 12))
    }
    /** Shell ativo (a UI recompõe ao trocar de sessão). */
    val shell: TermSession? =
        shells.getOrElse(activeShellIdx.coerceIn(shells.indices)) { shells.firstOrNull() }

    fun addShell() {
        if (!ensurePtyLib()) return
        if (shells.size >= MAX_TERM_SESSIONS) {
            Toast.makeText(
                context,
                "Limite de $MAX_TERM_SESSIONS sessões — feche uma para abrir outra",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val s = makePtyShell()
        s.start()
        shells.add(s)
        activeShellIdx = shells.lastIndex
    }

    fun closeShellAt(i: Int) {
        if (shells.size <= 1) return
        shells[i].stop()
        shells.removeAt(i)
        activeShellIdx = activeShellIdx.coerceIn(shells.indices)
    }

    fun setTerminalFont(sp: Int) {
        terminalFontSp = sp.coerceIn(MIN_TERM_FONT_SP, MAX_TERM_FONT_SP)
        prefs.edit().putInt(KEY_TERM_FONT, terminalFontSp).apply()
    }

    /** Abre um shell do Termux (ou a tela de instalação se ainda não houver). */
    fun openLinuxShell() {
        if (!termux.isReady()) {
            showLinuxSetup = true
            return
        }
        if (!ensurePtyLib()) return
        val s = makePtyShell("TERMUX")
        s.start()
        if (shells.size >= MAX_TERM_SESSIONS) {
            shells[activeShellIdx.coerceIn(shells.indices)].stop()
            shells[activeShellIdx.coerceIn(shells.indices)] = s
        } else {
            shells.add(s)
            activeShellIdx = shells.lastIndex
        }
        sheetView = null
        panelTab = 2
        sheetVisible = true
    }

    // Ícones da status/nav bar seguem o tema do app (não o do sistema)
    SideEffect {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = if (theme.isDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            },
            navigationBarStyle = if (theme.isDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )
    }

    var expandedPaths by remember { mutableStateOf(setOf<String>()) }

    fun File.toNode(depth: Int): FileNode {
        val kids = if (isDirectory && depth < 3) {
            FileUtils.listFiles(this).map { it.toNode(depth + 1) }
        } else emptyList()
        return FileNode(name, absolutePath, isDirectory, kids, length())
    }

    fun DocumentFile.toSafNode(depth: Int): FileNode {
        val kids = if (isDirectory && depth < 2) {
            listFiles()
                .sortedWith(compareBy({ !it.isDirectory }, { (it.name ?: "").lowercase() }))
                .map { it.toSafNode(depth + 1) }
        } else emptyList()
        return FileNode(name ?: "?", uri.toString(), isDirectory, kids, length())
    }

    fun queryKids(treeUri: Uri, parentId: String, depth: Int): List<FileNode> {
        val kidsUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val out = mutableListOf<FileNode>()
        context.contentResolver.query(
            kidsUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null, null, null
        )?.use { c ->
            val idI = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nI = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mI = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sI = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            while (c.moveToNext()) {
                val docId = if (idI >= 0) c.getString(idI) else null ?: continue
                val name = if (nI >= 0) c.getString(nI) else null ?: "?"
                val isDir = if (mI >= 0) c.getString(mI) == DocumentsContract.Document.MIME_TYPE_DIR else false
                val size = if (sI >= 0) c.getLong(sI) else 0L
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val kids = if (isDir && depth < 2) queryKids(treeUri, docId, depth + 1) else emptyList()
                out += FileNode(name, docUri.toString(), isDir, kids, size)
            }
        }
        return out.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    /** Listagem crua via DocumentsContract, sem passar pelo DocumentFile. */
    fun rawTreeList(treeUri: Uri): List<FileNode> {
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        return queryKids(treeUri, treeId, 0)
    }

    /** Árvore da pasta local (SAF), com limite de profundidade (I/O por query). */
    fun safTree(root: Uri): List<FileNode> {
        val tree = DocumentFile.fromTreeUri(context, root)
        ?: throw IllegalArgumentException("URI não é Árvore SAF")
        return try {
            tree.listFiles()
                .sortedWith(compareBy({ !it.isDirectory }, { (it.name ?: "").lowercase() }))
                .map { it.toSafNode(0) }
        } catch (e: Exception) {
            // Fallback: query direta via DocumentsContract (alguns providers falham no DocumentFile)
            Log.w("VsCodeMobile", "DocumentFile falhou, tentando query crua", e)
            rawTreeList(root)
        }
    }

    fun List<FileNode>.flatFiles(): List<FileNode> =
        flatMap { if (it.isDirectory) it.children.flatFiles() else listOf(it) }

    fun refreshExplorer() { explorerTick++ }

    // Nós do explorer: workspace (sync, rápido) e SAF (async, IPC fora da main)
    val localNodes = remember(currentDir, explorerTick) {
        FileUtils.listFiles(currentDir).map { it.toNode(0) }
    }
    LaunchedEffect(safRoot) {
        val root = safRoot
        if (root != null) explorerVm.load(root)
    }
    LaunchedEffect(Unit) {
        explorerVm.errors.collect { snackbarHostState.showSnackbar(it) }
    }

    /** Aplica edição ao campo, registrando histórico p/ desfazer/refazer (camada UI). */
    fun applyEdit(next: TextFieldValue, record: Boolean = true) {
        if (record) {
            val now = SystemClock.uptimeMillis()
            if (undoStack.lastOrNull() != rawText && now - lastPushTime > 800) {
                undoStack.addLast(rawText)
                if (undoStack.size > 50) undoStack.removeFirst()
                lastPushTime = now
            }
            redoStack.clear()
        }
        fieldState = next
        if (next.text != rawText) {
            rawText = next.text
            dirty = true
        }
        currentFile?.let { lspManager.noteChange(it, next.text) }
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(rawText)
        val caret = fieldState.selection.start.coerceIn(0, prev.length)
        rawText = prev
        dirty = true
        fieldState = TextFieldValue(prev, TextRange(caret))
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(rawText)
        val caret = fieldState.selection.start.coerceIn(0, next.length)
        rawText = next
        dirty = true
        fieldState = TextFieldValue(next, TextRange(caret))
    }

    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * Lê o documento. `null` = a leitura FALHOU (não é "arquivo vazio").
     * O chamador precisa distinguir os dois casos: salvar por cima de uma
     * leitura falha destrói o arquivo original.
     */
    /**
     * Lê o documento classificando texto/binário/ilegível.
     *
     * O chamador precisa dos três casos separados: salvar por cima de uma
     * leitura falha destrói o arquivo, e salvar um binário aberto como texto
     * o corrompe do mesmo jeito.
     */
    fun readRef(ref: DocRef): DocContent = when (ref) {
        is DocRef.Local -> FileUtils.readDoc(ref.file)
        is DocRef.Saf -> DocumentUtils.readDoc(context, ref.uri)
    }

    /** Motivo que impede salvar, ou null se o documento está ok. */
    fun blockReason(doc: DocContent, name: String): String? = when (doc) {
        is DocContent.Text -> null
        is DocContent.Binary -> "$name é um arquivo binário — aberto somente para leitura"
        is DocContent.Unreadable -> "Não foi possível ler $name"
    }

    fun writeRef(ref: DocRef, text: String): Boolean = when (ref) {
        is DocRef.Local -> FileUtils.writeAtomic(ref.file, text)
        is DocRef.Saf -> DocumentUtils.writeText(context, ref.uri, text)
    }

    /** Arquivo local p/ abrir no Termux externo (SAF não é visível p/ outro app). */
    fun termuxFile(): File? = (currentFile as? DocRef.Local)?.file

    fun openFile(ref: DocRef) {
        if (ref is DocRef.Local && ref.file.isDirectory) {
            currentDir = ref.file
            refreshExplorer()
            return
        }
        if (currentFile == ref) return
        val prev = currentFile
        val prevText = rawText
        val prevDirty = dirty
        val gen = ++loadGen
        currentFile = ref
        if (!openFiles.contains(ref)) openFiles.add(ref)
        rawText = ""
        dirty = false
        fieldState = TextFieldValue("")
        clearHistory()
        scope.launch {
            if (prevDirty && prev != null) {
                withContext(Dispatchers.IO) { writeRef(prev, prevText) }
            }
            val doc = withContext(Dispatchers.IO) { readRef(ref) }
            if (gen == loadGen) {
                val blocked = blockReason(doc, ref.name)
                if (blocked != null) {
                    // Binário ou ilegível: NÃO abre em branco fingindo estar
                    // vazio — salvar assim sobrescreveria o original.
                    loadBlock = blocked
                    dirty = true
                    rawText = ""
                    fieldState = TextFieldValue("")
                    return@launch
                }
                loadBlock = null
                val text = (doc as DocContent.Text).content
                rawText = text
                dirty = false
                fieldState = TextFieldValue(text)
                clearHistory()
                lspManager.noteOpen(ref, text)
            }
        }
    }

    fun saveCurrent(silent: Boolean = false) {
        // Nunca grava quando o buffer não representa o disco: leitura falhou ou o
        // arquivo é binário (abrir um .png e salvar corromperia o original).
        loadBlock?.let { reason ->
            val msg = "$reason — não salvo para não corromper o arquivo"
            if (!silent) Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            scope.launch { snackbarHostState.showSnackbar(msg) }
            return
        }
        val target = currentFile ?: DocRef.Local(
            workspace.resolve("untitled.txt").also {
                currentFile = DocRef.Local(it)
                if (!openFiles.contains(currentFile)) openFiles.add(currentFile!!)
            }
        )
        val snapshot = rawText
        scope.launch {
            val ok = withContext(Dispatchers.IO) { writeRef(target, snapshot) }
            if (ok) {
                if (rawText == snapshot) dirty = false
                refreshExplorer()
                if (!silent) Toast.makeText(context, "Saved ${target.name}", Toast.LENGTH_SHORT).show()
            } else if (!silent) {
                Toast.makeText(context, "Erro ao salvar ${target.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val symbolPairs = mapOf(
        "(" to "()",
        "{" to "{}",
        "[" to "[]",
        "\"" to "\"\"",
        "'" to "''"
    )

    fun insertSymbol(s: String) {
        if (currentFile == null) return
        val cur = fieldState
        val a = cur.selection.start.coerceIn(0, cur.text.length)
        val b = cur.selection.end.coerceIn(0, cur.text.length)
        val from = minOf(a, b)
        val to = maxOf(a, b)
        val pair = symbolPairs[s]
        val (ins, cursor) = if (pair != null && from == to) pair to from + 1
        else s to from + s.length
        val newText = cur.text.substring(0, from) + ins + cur.text.substring(to)
        applyEdit(TextFieldValue(newText, TextRange(cursor.coerceIn(0, newText.length))))
    }

    fun moveCursor(dLine: Int, dCol: Int) {
        val text = fieldState.text
        if (text.isEmpty()) return
        val caret = fieldState.selection.start.coerceIn(0, text.length)
        val lines = text.split('\n')
        var offset = 0
        var line = 0
        var col = 0
        for ((i, l) in lines.withIndex()) {
            if (caret <= offset + l.length) {
                line = i
                col = caret - offset
                break
            }
            offset += l.length + 1
            if (i == lines.lastIndex) {
                line = i
                col = l.length
            }
        }
        val newLine = (line + dLine).coerceIn(0, lines.lastIndex)
        val newCol = if (dLine == 0) (col + dCol).coerceIn(0, lines[newLine].length)
        else col.coerceIn(0, lines[newLine].length)
        val newCaret = lines.take(newLine).sumOf { it.length + 1 } + newCol
        fieldState = fieldState.copy(selection = TextRange(newCaret.coerceIn(0, text.length)))
    }

    /** Cursor no início/fim da palavra vizinha (dir = 1 próxima, -1 anterior). */
    fun moveByWord(dir: Int) {
        val text = fieldState.text
        if (text.isEmpty()) return
        var i = fieldState.selection.start.coerceIn(0, text.length)
        if (dir < 0 && i > 0) i--
        val isWord = { c: Char -> c.isLetterOrDigit() || c == '_' }
        if (dir > 0) {
            while (i < text.length && !isWord(text[i])) i++
            while (i < text.length && isWord(text[i])) i++
        } else {
            while (i > 0 && !isWord(text[i - 1])) i--
            while (i > 0 && isWord(text[i - 1])) i--
        }
        fieldState = fieldState.copy(selection = TextRange(i.coerceIn(0, text.length)))
    }

    fun handleEditKey(key: EditKey) {
        when (key) {
            is EditKey.Text -> {
                when {
                    ctrlLatched && (key.value == "z" || key.value == "Z") -> undo()
                    ctrlLatched && (key.value == "y" || key.value == "Y") -> redo()
                    else -> insertSymbol(key.value)
                }
                ctrlLatched = false
                altLatched = false
            }
            EditKey.Tab -> {
                insertSymbol("    ")
                ctrlLatched = false
                altLatched = false
            }
            EditKey.Esc -> {
                if (menuOpen) menuOpen = false
                else keyboard?.hide()
                ctrlLatched = false
                altLatched = false
            }
            // Alt = mover por palavra (como Ctrl+← do VS Code)
            EditKey.ArrowLeft ->
                if (altLatched) moveByWord(-1) else moveCursor(0, -1)
            EditKey.ArrowRight ->
                if (altLatched) moveByWord(1) else moveCursor(0, 1)
            EditKey.ArrowUp -> moveCursor(-1, 0)
            EditKey.ArrowDown -> moveCursor(1, 0)
            EditKey.Undo -> undo()
            EditKey.Redo -> redo()
            EditKey.Ctrl -> {
                ctrlLatched = !ctrlLatched
                altLatched = false
            }
            EditKey.Alt -> {
                altLatched = !altLatched
                ctrlLatched = false
            }
        }
    }

    fun closeSheet() {
        if (panelTab == 2 && (shell?.running?.value == true)) terminalPinned = true
        sheetVisible = false
        sheetView = null
    }

    /** Cursor no início da linha (1-based); ignora linha fora do arquivo. */
    fun jumpToLine(line: Int) {
        val text = fieldState.text
        var idx = 0
        var n = 1
        while (n < line && idx < text.length) {
            val nl = text.indexOf('\n', idx)
            if (nl < 0) break
            idx = nl + 1
            n++
        }
        if (n != line) return
        fieldState = fieldState.copy(selection = TextRange(idx))
    }

    /** Abre o arquivo do diagnóstico e posiciona o cursor na linha. */
    fun openProblem(p: ProblemItem) {
        val target = openFiles.firstOrNull { it.name == p.file }
            ?: allFiles(workspace).firstOrNull { it.name == p.file }?.let { DocRef.Local(it) }
        if (target == null) {
            scope.launch { snackbarHostState.showSnackbar("Abra ${p.file} para ver o diagnóstico") }
            return
        }
        if (target == currentFile) {
            jumpToLine(p.line)
        } else {
            pendingCaretLine = p.line
            openFile(target)
        }
        closeSheet()
    }

    // Pickers SAF: pasta e arquivo do aparelho (com fallback p/ intent crua)
    fun onTreePicked(uri: Uri) {
        Log.d("VsCodeMobile", "pasta escolhida: $uri")
        if (DocumentFile.fromTreeUri(context, uri) == null) {
            Toast.makeText(
                context,
            "Esta pasta não veio como Árvore SAF (${uri.authority})",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        prefs.edit().putString(KEY_SAF_ROOT, uri.toString()).apply()
        val rootName = DocumentFile.fromTreeUri(context, uri)?.name ?: "local"
        prefs.edit().putString(KEY_SAF_NAME, rootName).apply()
        safRoot = uri
        safRootName = rootName
        refreshExplorer()
    }

    fun onDocPicked(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val name = DocumentUtils.displayName(context, uri) ?: "untitled"
        openFile(DocRef.Saf(uri, name))
    }

    val fallbackTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        onTreePicked(uri)
    }
    val fallbackDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        onDocPicked(uri)
    }

    val openTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        onTreePicked(uri)
    }
    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        onDocPicked(uri)
    }

    fun pickFolder() {
        Toast.makeText(context, "Escolha uma pasta…", Toast.LENGTH_SHORT).show()
        try {
            openTreeLauncher.launch(null)
        } catch (_: Exception) {
            try {
                fallbackTreeLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
            } catch (_: Exception) {
                Toast.makeText(context, "Seletor de pastas indisponível neste aparelho", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun pickFile() {
        try {
            openDocLauncher.launch(arrayOf("*/*"))
        } catch (_: Exception) {
            try {
                fallbackDocLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*" })
            } catch (_: Exception) {
                Toast.makeText(context, "Seletor de arquivos indisponível neste aparelho", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Conteúdo do Explorer para o bottom sheet (multi-root). */
    @Composable
    fun ExplorerSheetContent() {
        val roots = buildList {
            add(
                ExplorerRoot(
                    key = "workspace",
                    title = "WORKSPACE",
                    rootLabel = currentDir.absolutePath.removePrefix(
                        workspace.parentFile?.absolutePath ?: ""
                    ),
                    nodes = localNodes,
                    canNavigateUp = currentDir != workspace,
                                        emptyHint = "Workspace vazio — use ＋ ou abra uma pasta local",
                    outlineText = currentFile?.let {
                                            if (it is DocRef.Local) "${it.name} — ${it.file.length()} bytes"
                        else it.name
                    } ?: "No active editor"
                )
            )
            if (safRoot != null) {
                add(
                    ExplorerRoot(
                        key = "local",
                        title = safRootName.uppercase(),
                        badge = "LOCAL",
                        rootLabel = safRootName,
                        nodes = safNodes,
                                        emptyHint = "Pasta vazia ou sem acesso — toque em ↻ para tentar de novo",
                        closeable = true
                    )
                )
            }
        }
        MultiRootExplorer(
            roots = roots,
            expandedPaths = expandedPaths,
            selectedPath = currentFile?.id,
            onToggleDir = { p ->
                expandedPaths = if (p in expandedPaths) expandedPaths - p else expandedPaths + p
            },
            onOpenNode = { n ->
                if (n.path.startsWith("content://")) {
                    openFile(DocRef.Saf(Uri.parse(n.path), n.name))
                } else {
                    openFile(DocRef.Local(File(n.path)))
                }
                closeSheet()
            },
            onNavigateUp = { key ->
                if (key == "workspace") {
                    currentDir = currentDir.parentFile ?: workspace
                    refreshExplorer()
                }
            },
            onNewFile = { key ->
                newTarget = key
                showNewFile = true
            },
            onNewFolder = { key ->
                newTarget = key
                showNewFolder = true
            },
            onRefresh = { refreshExplorer(); explorerVm.refresh() },
            onOpenFolder = { pickFolder() },
            onCloseRoot = { key ->
                if (key == "local") {
                    safRoot = null
                    explorerVm.clear()
                    prefs.edit()
                        .remove(KEY_SAF_ROOT)
                        .remove(KEY_SAF_NAME)
                        .apply()
                    refreshExplorer()
                }
            },
            showHidden = showHidden,
            onToggleHidden = { explorerVm.toggleHidden() }
        )
    }

    /** Views legadas (Search/Git/Run/Ext) para o bottom sheet. */
    @Composable
    fun LegacyViewSheetContent(view: ActiveView) {
        SideBar(
            view = view,
            dir = currentDir,
            workspace = workspace,
            bg = chrome.sideBar,
            sectionColor = chrome.sideBarSection,
            fg = if (theme.isDark) Color(0xFFCCCCCC) else Color(0xFF333333),
            tick = explorerTick,
            searchQuery = searchQuery,
            onSearch = { searchQuery = it },
            onOpen = {
                openFile(DocRef.Local(it))
                closeSheet()
            },
            onNavigate = { currentDir = it; refreshExplorer() },
            onNewFile = { newTarget = "workspace"; showNewFile = true },
            onNewFolder = { newTarget = "workspace"; showNewFolder = true },
            onRefresh = { refreshExplorer() },
            onRun = { TermuxHelper.openInTermux(context, termuxFile()) },
            currentFile = (currentFile as? DocRef.Local)?.file
        )
    }

    // Workspace começa em branco (sem placeholder); shells + restaura pasta SAF
    LaunchedEffect(Unit) {
        shells.forEach { it.start() }
        // Limpeza única do placeholder de versões antigas (só se intacto)
        try {
            val legacy = workspace.resolve("main.kt")
            if (legacy.exists() && legacy.readText() ==
                "fun main() {\n    // Bem-vindo ao VSCode Mobile!\n" +
                "    val msg = \"Olá, ARM64\"\n    println(msg)\n}\n"
            ) {
                legacy.delete()
            }
        } catch (_: Exception) {
        }
        prefs.getString(KEY_SAF_ROOT, null)?.let { saved ->
            val uri = Uri.parse(saved)
            val held = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
            if (held) {
                safRoot = uri
                safRootName = prefs.getString(KEY_SAF_NAME, null) ?: "local"
            } else {
                prefs.edit().remove(KEY_SAF_ROOT).apply()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            shells.forEach { it.stop() }
            lspManager.close()
        }
    }

    // Auto-save afterDelay (igual ao settings do desktop)
    LaunchedEffect(rawText, currentFile) {
        if (!dirty) return@LaunchedEffect
        kotlinx.coroutines.delay(1200)
        saveCurrent(silent = true)
    }

    // Highlight com debounce, fora da main thread; arquivo grande = texto puro
    var highlighted by remember { mutableStateOf(SyntaxHighlighter.highlightAnnotated("", theme)) }
    LaunchedEffect(rawText, theme) {
        kotlinx.coroutines.delay(450)
        val snapshot = rawText
        val snapshotTheme = theme
        val result = withContext(Dispatchers.Default) {
            val big = snapshot.length > 100_000 || snapshot.count { it == '\n' } > 3000
            if (big) null else SyntaxHighlighter.highlightAnnotated(snapshot, snapshotTheme)
        }
        if (snapshot == rawText && snapshotTheme == theme) {
            highlighted = result ?: AnnotatedString(snapshot)
        }
    }

    // Cursor vai para a linha do diagnóstico assim que o texto carrega
    LaunchedEffect(rawText, currentFile) {
        val line = pendingCaretLine
        if (line > 0 && rawText.isNotEmpty()) {
            pendingCaretLine = 0
            jumpToLine(line)
        }
    }

    // Problems locais (TODO/FIXME/ERROR) + diagnósticos LSP.
    // O scan vai para Dispatchers.Default com debounce: antes rodava dentro do
    // remember(...) na main thread, a cada tecla, varrendo o arquivo inteiro.
    var localProblems by remember { mutableStateOf<List<ProblemItem>>(emptyList()) }
    LaunchedEffect(rawText, currentFile) {
        val snapshot = rawText
        val name = currentFile?.name ?: ""
        if (snapshot.isEmpty()) {
            localProblems = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(300)
        val found = withContext(Dispatchers.Default) {
            collectProblems(snapshot, name).map {
                ProblemItem(
                    it.file,
                    it.line,
                    if (it.severity == "error") ProblemSeverity.ERROR else ProblemSeverity.WARNING,
                    it.msg
                )
            }
        }
        if (snapshot == rawText) localProblems = found
    }

    val problems: List<ProblemItem> = remember(localProblems, currentFile, lspDiags) {
        localProblems + (currentFile?.let { lspDiags[it.id] } ?: emptyList())
    }

    fun openViewSheet(view: ActiveView) {
        activeView = view
        sheetView = view
        sheetVisible = true
    }

    /** Container do registro -> view legada do drawer. */
    fun viewForContainer(id: String) = when (id) {
        "workbench.view.explorer" -> ActiveView.EXPLORER
        "workbench.view.search" -> ActiveView.SEARCH
        "workbench.view.scm" -> ActiveView.GIT
        "workbench.view.debug" -> ActiveView.RUN
        else -> ActiveView.EXT
    }

    fun containerForView(view: ActiveView): String = view.id

    /**
     * Ponte do plugin. Superficie: executar comando do workbench e ler o
     * documento ativo. Comandos que abrem PTY, instalam Alpine ou gravam em
     * massa ficam bloqueados — quem controla a URL controla código.
     */
    fun pluginBridge(): PluginBridge = PluginBridge(
        onExecuteCommand = { id, _ -> CommandRegistry.execute(id) },
        onActiveDocument = {
            val cf = currentFile
            JSONObject()
                .put("uri", cf?.id ?: "")
                .put("name", cf?.name ?: "")
                .put("languageId", SyntaxHighlighter.languageOf(cf?.name ?: ""))
                .put("text", rawText)
                .put("dirty", dirty)
                .toString()
        },
        onShowMessage = { m ->
            scope.launch { snackbarHostState.showSnackbar(m) }
        },
        onLog = { line -> Log.d("VsCodeMobile/plugin", line) },
        isSensitive = { id -> SensitiveCommands.isDenied(id) }
    )

    /** Resolve um id de container para plugin, se houver um habilitado. */
    fun pluginFor(id: String): PluginSource? =
        plugins.firstOrNull { it.enabled && it.id == id }

    fun viewFor(id: String): ActiveView =
        pluginFor(id)?.let { ActiveView(it.id, it) } ?: viewForContainer(id)

    /**
     * Entradas da bottom bar: containers embutidos (título vem do registro, e
     * não mais de um `when` hardcoded) seguidos dos plugins habilitados.
     */
    fun navEntries(): List<NavEntry> {
        val builtin = ViewRegistry.sidebarContainers()
            .filter { it.id in BuiltinContainerIds }
            .map { NavEntry(it.id, it.icon, BuiltinContainerLabel(it.id)) }
        val fromPlugins = plugins.filter { it.enabled }
            .map { NavEntry("plugin:${it.id}", it.icon, it.title) }
        return builtin + fromPlugins
    }

    fun closeTab(ref: DocRef) {
        lspManager.noteClose(ref)
        // dirty mas bloqueado (binário/ilegível) não pode ser salvo.
        if (ref == currentFile && dirty && loadBlock == null) {
            val snapshot = rawText
            scope.launch {
                withContext(Dispatchers.IO) { writeRef(ref, snapshot) }
            }
        }
        openFiles.remove(ref)
        if (currentFile == ref) {
            val next = openFiles.lastOrNull()
            currentFile = next
            if (next is DocRef.Local) {
                val doc = FileUtils.readDoc(next.file)
                val blocked = blockReason(doc, next.name)
                if (blocked != null) {
                    loadBlock = blocked
                    rawText = ""
                    fieldState = TextFieldValue("")
                } else {
                    loadBlock = null
                    rawText = (doc as DocContent.Text).content
                    dirty = false
                    fieldState = TextFieldValue(rawText)
                    clearHistory()
                    lspManager.noteOpen(next, rawText)
                }
            } else if (next != null) {
                val gen = ++loadGen
                rawText = ""
                dirty = false
                fieldState = TextFieldValue("")
                clearHistory()
                scope.launch {
                    val doc = withContext(Dispatchers.IO) { readRef(next) }
                    if (gen == loadGen) {
                        val blocked = blockReason(doc, next.name)
                        if (blocked != null) {
                            loadBlock = blocked
                        } else {
                            loadBlock = null
                            val text = (doc as DocContent.Text).content
                            rawText = text
                            fieldState = TextFieldValue(text)
                            lspManager.noteOpen(next, text)
                        }
                    }
                }
            } else {
                rawText = ""
                dirty = false
                loadBlock = null
                fieldState = TextFieldValue("")
                clearHistory()
            }
        }
    }

    // Registro dos plugins built-in (containers, views, comandos, quick access)
    val pickIndex = remember { mutableStateOf<List<QuickPickItem>>(emptyList()) }
    remember(Unit) {
        ViewRegistry.clear()
        CommandRegistry.clear()
        QuickAccessRegistry.clear()
        BuiltinPlugins.registerAll(
            WorkbenchActions(
                save = { saveCurrent() },
                saveAll = { saveCurrent() },
                newFile = { newTarget = "workspace"; showNewFile = true },
                openFile = { pickFile() },
                openFolder = { pickFolder() },
                closeEditor = { currentFile?.let { closeTab(it) } },
                showView = { id -> openViewSheet(viewForContainer(id)) },
                toggleDrawer = { menuOpen = !menuOpen },
                togglePanel = { tab -> panelTab = tab; sheetView = null; sheetVisible = true },
                showPalette = { showPalette = true },
                showThemes = { showThemePicker = true },
                openTermux = { TermuxHelper.openInTermux(context, termuxFile()) },
                restartShell = { shell?.restart() },
                showProblems = { panelTab = 0; sheetVisible = true },
                onLinuxSetup = { showLinuxSetup = true },
                onLinuxShell = { openLinuxShell() },
                managePlugins = { showPluginManager = true }
            )
        )
        // Índice da palette construído fora da composição. Antes, items() fazia
        // allFiles() + safNodes.flatFiles() na MAIN THREAD a cada tecla.
        // Holder separado porque o provider vive num remember(Unit) e precisa
        // enxergar o valor atual.
        QuickAccessRegistry.register(object : QuickAccessProvider {
            override val prefix = ""
            override val placeholder = "Type a file name"
            override fun items(query: String): List<QuickPickItem> {
                val index = pickIndex.value
                if (index.isEmpty()) return emptyList()
                if (query.isBlank()) return index.take(15)
                return index.filter { fuzzyMatch(query, it.label) }.take(15)
            }
            override fun onPick(item: QuickPickItem) {
                when {
                    item.id.startsWith("saf:") ->
                        openFile(DocRef.Saf(Uri.parse(item.id.removePrefix("saf:")), item.label))
                    item.id.startsWith("file:") -> {
                        val f = File(item.id.removePrefix("file:"))
                        if (f.exists()) openFile(DocRef.Local(f))
                    }
                }
                showPalette = false
            }
        })
    }

    val imeVisible = WindowInsets.isImeVisible

    // Popula o índice da palette em IO, só quando a palette abre ou o explorer
    // muda. Não depende de paletteQuery — digitar não dispara nova varredura.
    val pickKey = listOf(explorerTick, safRoot?.toString() ?: "", safNodes.size).hashCode()
    LaunchedEffect(showPalette, pickKey) {
        if (!showPalette) return@LaunchedEffect
        val safLabel = safRootName
        val safTreeSnapshot = safNodes
        pickIndex.value = withContext(Dispatchers.IO) {
            val ws = allFiles(workspace).take(200).map {
                QuickPickItem(it.name, it.parentFile?.name ?: "", "file:${it.absolutePath}")
            }
            val saf = safTreeSnapshot.flatFiles().take(300).map {
                QuickPickItem(it.name, safLabel, "saf:${it.path}")
            }
            ws + saf
        }
    }

    val bottomState: BottomBarState = when {
        imeVisible && currentFile != null -> BottomBarState.Editing(
            ctrlActive = ctrlLatched,
            altActive = altLatched,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            onKey = ::handleEditKey,
            onExit = { keyboard?.hide() }
        )
        terminalPinned -> BottomBarState.Terminal(
            running = shell?.running?.value == true,
            fontSp = terminalFontSp,
            onFontChange = ::setTerminalFont,
            onStart = {
                val s = shell
                if (s != null && s.running.value) {
                    panelTab = 2
                    sheetVisible = true
                } else s?.restart()
            },
            onStop = { shell?.stop() },
            onClear = { shell?.clear() },
            onTogglePanel = { panelTab = 2; sheetView = null; sheetVisible = true },
            onExit = { terminalPinned = false }
        )
        else -> BottomBarState.Navigation(
            entries = navEntries(),
            activeKey = containerForView(activeView),
            onSelect = { key ->
                terminalPinned = false
                openViewSheet(
                    if (key.startsWith("plugin:")) {
                        val pid = key.removePrefix("plugin:")
                        plugins.firstOrNull { it.id == pid }
                            ?.let { ActiveView(it.id, it) } ?: viewForContainer(key)
                    } else viewFor(key)
                )
            }
        )
    }

    VsCodeTheme(colors = vscodeColorsFor(theme)) {
        Surface(Modifier.fillMaxSize(), color = theme.background.toCompose()) {
            Scaffold(
                        containerColor = theme.background.toCompose(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            Box(Modifier.navigationBarsPadding()) {
                                AdaptiveBottomBar(bottomState)
                            }
                        },
                        floatingActionButton = {
                            SmallFloatingActionButton(
                                onClick = { panelTab = 2; sheetView = null; sheetVisible = true },
                                containerColor = chrome.statusBar,
                                contentColor = chrome.statusBarFg
                            ) {
                                Icon(Icons.Filled.Terminal, "Terminal")
                            }
                        }
                    ) { padding ->
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            TopBar(
                                title = currentFile?.name ?: "VSCode Mobile",
                                isDirty = dirty,
                                onMenuClick = { menuOpen = !menuOpen },
                                onSearchClick = { showPalette = true }
                            )
                            if (menuOpen) {
                                TopDropMenu(
                                    entries = navEntries(),
                                    activeKey = containerForView(activeView),
                                    onSelectEntry = { key ->
                                        menuOpen = false
                                        terminalPinned = false
                                        openViewSheet(
                                            if (key.startsWith("plugin:")) {
                                                val pid = key.removePrefix("plugin:")
                                                plugins.firstOrNull { it.id == pid }
                                                    ?.let { ActiveView(it.id, it) }
                                                    ?: viewForContainer(key)
                                            } else viewFor(key)
                                        )
                                    },
                                    actions = listOf(
                                        MenuAction(
                                            Codicon.Terminal, "Terminal",
                                            { menuOpen = false; sheetView = null; panelTab = 2; sheetVisible = true }
                                        ),
                                        MenuAction(
                                            Codicon.Search, "Command Palette",
                                            { menuOpen = false; showPalette = true }
                                        ),
                                        MenuAction(
                                            Codicon.NewFolder, "Open Folder…",
                                            { menuOpen = false; pickFolder() }
                                        ),
                                        MenuAction(
                                            Codicon.SettingsGear, "Color Theme",
                                            { menuOpen = false; showThemePicker = true }
                                        )
                                    )
                                )
                            }

                            EditorTabs(
                                tabs = openFiles.map {
                                    EditorTab(it.id, it.name, dirty && it == currentFile)
                                },
                                activeId = currentFile?.id,
                                onSelect = { id ->
                                    openFiles.firstOrNull { it.id == id }?.let { openFile(it) }
                                },
                                onClose = { id ->
                                    openFiles.firstOrNull { it.id == id }?.let { closeTab(it) }
                                }
                            )
                            currentFile?.let { cf ->
                                val segs = when (cf) {
                                    is DocRef.Local -> cf.file.absolutePath.removePrefix(filesDirPath)
                                        .split("/").filter { it.isNotEmpty() }
                                    is DocRef.Saf -> listOf(safRootName, cf.name)
                                }
                                Breadcrumb(
                                    segments = segs,
                                    language = SyntaxHighlighter.languageOf(cf.name)
                                )
                            }

                            if (loadBlock != null) {
                                // Binário ou ilegível: mostrar o editor vazio
                                // sem aviso levaria o usuário a salvar por cima.
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(theme.background.toCompose()),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CodiconIcon(
                                        Codicon.Warning,
                                        tint = Color(0xFFF85149),
                                        sizeSp = 32
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        loadBlock!!,
                                        color = Color(0xFFF85149),
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Salvamento bloqueado para não corromper o arquivo.",
                                        color = theme.foreground.toCompose().copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            } else {
                                EditorSurface(
                                    state = fieldState,
                                    highlighted = highlighted,
                                    fontSizeSp = editorFontSize,
                                    onFontSizeChange = { editorFontSize = it },
                                    empty = currentFile == null,
                                    onChange = { v -> applyEdit(v) },
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                            }

                            // A status bar some com o teclado aberto (a bottom bar vira teclas)
                            if (!imeVisible) {
                                StatusBar(
                                    branch = "main*",
                                    errorCount = problems.count { it.severity == ProblemSeverity.ERROR },
                                    warningCount = problems.count { it.severity == ProblemSeverity.WARNING },
                                    lineCol = lineColOf(fieldState),
                                    language = currentFile?.let { SyntaxHighlighter.languageOf(it.name) } ?: "—",
                                    onTerminalClick = { panelTab = 2; sheetView = null; sheetVisible = true }
                                )
                            }
                        }
                    }
            }
        }

    /** Conteúdo da aba terminal (sempre PTY). */
    @Composable
    fun TerminalTabContent(fillHeight: Boolean, showExpand: Boolean, modifier: Modifier = Modifier) {
        val s = shell
        val fg = if (theme.isDark) Color(0xFFCCCCCC) else Color(0xFF333333)
        val bg = theme.background.toCompose()
        val accent = if (theme.isDark) Color(0xFF0078D4) else Color(0xFF005FB8)
        if (s is PtySession) {
            Column(modifier.fillMaxWidth()) {
                if (showExpand) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(
                            onClick = { termFullscreen = true },
                            modifier = Modifier.size(40.dp).semantics {
                                contentDescription = "Terminal em tela cheia"
                            }
                        ) {
                            CodiconIcon(Codicon.ScreenFull, tint = fg, sizeSp = 18)
                        }
                    }
                }
                TerminalScreen(
                    pty = s,
                    fg = fg,
                    fontSp = terminalFontSp,
                    fillHeight = fillHeight,
                    bg = bg,
                    accent = accent
                )
            }
        } else {
            Text(
                "Nenhuma sessão — toque em + para criar",
                color = fg.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // ===== BOTTOM SHEET: views (menu vertical) ou problems / output / terminal =====
    if (sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = chrome.panel
        ) {
            val sv = sheetView
            if (sv != null) {
                Box(Modifier.height(520.dp)) {
                    val plugin = sv.plugin
                    when {
                        plugin != null -> PluginSheetContent(
                            plugin = plugin,
                            bridge = pluginBridge(),
                            onMessage = { m ->
                                scope.launch { snackbarHostState.showSnackbar(m.take(200)) }
                            }
                        )
                        sv == ActiveView.EXPLORER -> ExplorerSheetContent()
                        else -> LegacyViewSheetContent(sv)
                    }
                }
            } else {
            ProblemsSheetContent(
                activeTab = panelTab,
                onTabSelect = { panelTab = it },
                problemCount = problems.size,
                problems = problems,
                output = outputLines,
                terminalContent = {
                    SessionRow(
                        count = shells.size,
                        active = activeShellIdx,
                        fg = if (theme.isDark) Color(0xFFCCCCCC) else Color(0xFF333333),
                        accent = if (theme.isDark) Color(0xFF0078D4) else Color(0xFF005FB8),
                        onSelect = { activeShellIdx = it },
                        onAdd = { addShell() },
                        onClose = { closeShellAt(it) }
                    )
                    TerminalTabContent(fillHeight = false, showExpand = true)
                },
                onOpenProblem = ::openProblem,
                onClose = { closeSheet() },
                contentHeight = 430.dp
            )
            }
        }
    }

    // ===== TERMINAL TELA CHEIA =====
    if (termFullscreen) {
        Dialog(
            onDismissRequest = { termFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
                    .background(theme.background.toCompose()),
                color = theme.background.toCompose()
            ) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Terminal",
                            color = if (theme.isDark) Color(0xFFCCCCCC) else Color(0xFF333333),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { termFullscreen = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Fechar tela cheia",
                                tint = if (theme.isDark) Color(0xFFCCCCCC) else Color(0xFF333333)
                            )
                        }
                    }
                    SessionRow(
                        count = shells.size,
                        active = activeShellIdx,
                        fg = if (theme.isDark) Color(0xFFCCCCCC) else Color(0xFF333333),
                        accent = if (theme.isDark) Color(0xFF0078D4) else Color(0xFF005FB8),
                        onSelect = { activeShellIdx = it },
                        onAdd = { addShell() },
                        onClose = { closeShellAt(it) }
                    )
                    TerminalTabContent(fillHeight = true, showExpand = false, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // ===== DIALOGS =====
    if (showLinuxSetup) {
        LinuxSetupSheet(
            state = prefixState,
            onDownload = { scope.launch { termux.install() } },
            onOpenShell = {
                showLinuxSetup = false
                openLinuxShell()
            },
            onDismiss = { showLinuxSetup = false }
        )
    }
    if (showPalette) {
        val provider = QuickAccessRegistry.providerFor(paletteQuery)
        val stripped = paletteQuery.removePrefix(provider.prefix).trimStart()
        // Filtragem pura sobre o índice pré-calculado; memoizado para não
        // refiltrar a cada recomposição do sheet.
        val picks = remember(stripped, pickIndex.value) { provider.items(stripped) }
        val cmdSection = if (provider.prefix == ">") {
            picks.map { PaletteCommand(it.label, it.detail) }
        } else emptyList()
        val fileSection = if (provider.prefix == ">") {
            emptyList()
        } else {
            picks.map { PaletteFile(it.label, it.detail ?: "", it.id) }
        }
        CommandPaletteSheet(
            commands = cmdSection,
            files = fileSection,
            query = paletteQuery,
            onQuery = { paletteQuery = it },
            onCommand = { cmd ->
                picks.firstOrNull { it.label == cmd.title }?.let { provider.onPick(it) }
                showPalette = false
            },
            onFile = { pf ->
                picks.firstOrNull { it.id == pf.id }?.let { provider.onPick(it) }
                showPalette = false
            },
            onDismiss = { showPalette = false; paletteQuery = "" }
        )
    }
    if (showPluginManager) {
        ModalBottomSheet(
            onDismissRequest = { showPluginManager = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = chrome.panel
        ) {
            PluginManagerSheet(
                plugins = plugins,
                onAdd = { title, url ->
                    val p = PluginSource(
                        id = url,
                        title = title.ifBlank { url },
                        url = url
                    )
                    plugins = plugins.filterNot { it.id == p.id } + p
                    PluginSource.save(context.applicationContext, plugins)
                },
                onRemove = { id ->
                    plugins = plugins.filterNot { it.id == id }
                    PluginSource.save(context.applicationContext, plugins)
                },
                onToggle = { id ->
                    plugins = plugins.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
                    PluginSource.save(context.applicationContext, plugins)
                },
                onClose = { showPluginManager = false }
            )
        }
    }
    if (showThemePicker) {
        ThemePickerDialog(
            current = theme,
            onPick = {
                theme = it
                onThemeChange(it)
                chrome = ThemeManager.chromeOf(it)
                highlighted = SyntaxHighlighter.highlightAnnotated(rawText, it)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }
    if (showNewFile) {
        NameDialog("New File", "nome.kt", { showNewFile = false }) { name ->
            val clean = name.ifBlank { "untitled.txt" }
            if (newTarget == "local" && safRoot != null) {
                scope.launch {
                    val created = withContext(Dispatchers.IO) {
                        val tree = DocumentFile.fromTreeUri(context, safRoot!!)
                        tree?.createFile(DocumentUtils.mimeFor(clean), clean)?.uri
                    }
                    if (created != null) {
                        openFile(DocRef.Saf(created, clean))
                        refreshExplorer()
                    } else {
                    Toast.makeText(context, "Não foi possível criar o arquivo", Toast.LENGTH_SHORT).show()
                    }
                    showNewFile = false
                }
            } else {
                val f = currentDir.resolve(clean)
                if (FileUtils.writeAtomic(f, "")) {
                    refreshExplorer()
                    openFile(DocRef.Local(f))
                } else {
                    Toast.makeText(context, "Não foi possível criar o arquivo", Toast.LENGTH_SHORT).show()
                }
                showNewFile = false
            }
        }
    }
    if (showNewFolder) {
        NameDialog("New Folder", "pasta", { showNewFolder = false }) { name ->
            val clean = name.ifBlank { "pasta" }
            if (newTarget == "local" && safRoot != null) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        DocumentFile.fromTreeUri(context, safRoot!!)?.createDirectory(clean)
                    }
                    refreshExplorer()
                    showNewFolder = false
                }
            } else {
                currentDir.resolve(clean).mkdirs()
                refreshExplorer()
                showNewFolder = false
            }
        }
    }
}

// ---------- SESSÕES DO TERMINAL (abas estilo VS Code) ----------
/**
 * Teto de sessões: cada PtySession custa 2 threads (TermSessionInputReader /
 * TermSessionOutputWriter), 1 processo e o buffer do emulador.
 */
private const val MAX_TERM_SESSIONS = 8

@Composable
private fun SessionRow(
    count: Int,
    active: Int,
    fg: Color,
    accent: Color,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onClose: (Int) -> Unit
) {
    val canAdd = count < MAX_TERM_SESSIONS
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until count) {
            val sel = i == active
            Text(
                "Terminal ${i + 1}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (sel) accent else fg.copy(alpha = 0.55f),
                modifier = Modifier.clickable { onSelect(i) }
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
            )
            if (count > 1) {
                IconButton(
                    onClick = { onClose(i) },
                    modifier = Modifier.size(40.dp).semantics { contentDescription = "Fechar sessão" }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
        IconButton(
            onClick = onAdd,
            enabled = canAdd,
            modifier = Modifier.size(40.dp).semantics {
                contentDescription = if (canAdd) "Nova sessão"
                else "Limite de $MAX_TERM_SESSIONS sessões atingido"
            }
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = if (canAdd) fg else fg.copy(alpha = 0.3f)
            )
        }
    }
}

// ---------- SIDE BAR (conteúdo das views no bottom sheet) ----------
@Composable
private fun SideBar(
    view: ActiveView, dir: File, workspace: File,
    bg: Color, sectionColor: Color, fg: Color, tick: Int,
    searchQuery: String, onSearch: (String) -> Unit,
    onOpen: (File) -> Unit, onNavigate: (File) -> Unit,
    onNewFile: () -> Unit, onNewFolder: () -> Unit, onRefresh: () -> Unit,
    onRun: () -> Unit, currentFile: File?
) {
    Column(Modifier.fillMaxWidth().fillMaxHeight().background(bg)) {
        when (view.id) {
            "workbench.view.explorer" -> {
                SideHeader("EXPLORER", sectionColor, fg,
                    actions = {
                        IconButton(onClick = onNewFile, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Add, "New File", tint = fg, modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = onNewFolder, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.CreateNewFolder, "New Folder", tint = fg, modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Refresh, "Refresh", tint = fg, modifier = Modifier.size(15.dp))
                        }
                    })
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
            "▸ " + dir.absolutePath.removePrefix(workspace.parentFile?.absolutePath ?: ""),
                        color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                }
                if (dir != workspace) {
                    Text("‹ .. (voltar)",
                        color = fg, fontSize = 12.sp,
                        modifier = Modifier.clickable { onNavigate(dir.parentFile ?: workspace) }
                            .padding(horizontal = 12.dp, vertical = 4.dp))
                }
                key(tick) {
                    val expanded = remember(tick) { mutableStateMapOf<String, Boolean>() }
                    LazyColumn(Modifier.weight(1f)) {
                        val files = FileUtils.listFiles(dir)
                        items(files, key = { it.absolutePath }) { f ->
                            FileRow(f, fg, depth = 0,
                                expanded = expanded,
                                onOpen = onOpen, currentFile = currentFile)
                        }
                    }
                }
                SideHeader("OUTLINE", sectionColor, fg)
                Text(
                    currentFile?.let { "${it.name} — ${it.length()} bytes" } ?: "No active editor",
                    color = sectionColor, fontSize = 11.sp, modifier = Modifier.padding(10.dp)
                )
            }
            "workbench.view.search" -> {
                SideHeader("SEARCH", sectionColor, fg)
                BoxSearch(searchQuery, onSearch, fg, sectionColor)
                val all = remember(tick) { allFiles(workspace).filter {
                    searchQuery.isBlank() || it.name.contains(searchQuery, true)
                }.take(100) }
                LazyColumn(Modifier.weight(1f)) {
                    items(all, key = { it.absolutePath }) { f ->
                        Text("  ${f.name}", color = fg, fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(f) }
                                .padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                }
            }
            "workbench.view.scm" -> {
                SideHeader("SOURCE CONTROL", sectionColor, fg)
                Text("CHANGES", color = sectionColor, fontSize = 11.sp, modifier = Modifier.padding(10.dp, 6.dp, 10.dp, 2.dp))
                    Text("● ${currentFile?.name ?: "—"} (unsaved/saved in workspace)",
                    color = fg, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    Text("Branch: main* • auto-save ligado (afterDelay)",
                    color = sectionColor, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
            }
            "workbench.view.debug" -> {
                SideHeader("RUN AND DEBUG", sectionColor, fg)
                Text("Terminal embutido na aba TERMINAL, ou Termux externo.",
                    color = fg, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                TextButton(onClick = onRun) { Text("▷ Run in Termux") }
            }
            "workbench.view.extensions" -> {
                SideHeader("EXTENSIONS", sectionColor, fg)
                val exts = listOf("Kotlin" to true, "Python" to true, "JSON" to true, "Markdown" to true, "C/C++" to false)
                LazyColumn {
                    items(exts) { (n, on) ->
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Extension, null, tint = fg, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(n, color = fg, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Icon(if (on) Icons.Filled.Check else Icons.Filled.Add, null,
                                tint = sectionColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxSearch(query: String, onSearch: (String) -> Unit, fg: Color, hint: Color) {
    Box(
        Modifier.padding(8.dp).background(Color(0x993C3C3C)).padding(6.dp)
    ) {
        BasicTextField(
            value = query, onValueChange = onSearch,
            textStyle = TextStyle(color = fg, fontSize = 13.sp),
            cursorBrush = SolidColor(fg),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search files…", color = hint, fontSize = 13.sp)
                inner()
            }
        )
    }
}

@Composable
private fun SideHeader(title: String, color: Color, fg: Color, actions: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f))
        actions?.invoke()
    }
}

@Composable
private fun FileRow(
    file: File, fg: Color, depth: Int,
    expanded: MutableMap<String, Boolean>,
    onOpen: (File) -> Unit, currentFile: File?
) {
    val pad = (8 + depth * 12).dp
    if (file.isDirectory) {
        val open = expanded.getOrDefault(file.absolutePath, depth < 1)
        Row(Modifier.fillMaxWidth().clickable {
            expanded[file.absolutePath] = !open
        }.padding(start = pad, top = 5.dp, bottom = 5.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(if (open) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight, null,
                tint = fg, modifier = Modifier.size(15.dp))
            Icon(if (open) Icons.Filled.FolderOpen else Icons.Filled.Folder, null,
                tint = Color(0xFFDCB67A), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(file.name, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (open) {
            for (child in FileUtils.listFiles(file)) {
                FileRow(child, fg, depth + 1, expanded, onOpen, currentFile)
            }
        }
    } else {
        Row(Modifier.fillMaxWidth()
            .background(if (file == currentFile) Color(0xFF094771) else Color.Transparent)
            .clickable { onOpen(file) }
            .padding(start = pad + 20.dp, top = 5.dp, bottom = 5.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = Color(0xFF75BEFF), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(file.name, color = fg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun allFiles(root: File, out: MutableList<File> = mutableListOf(), depth: Int = 0): List<File> {
    if (depth > 4 || out.size > 300) return out
    for (f in root.listFiles() ?: return out) {
        if (f.isDirectory) allFiles(f, out, depth + 1)
        else if (FileUtils.isTextFile(f)) out.add(f)
    }
    return out
}

// ---------- STATUS BAR ----------
private data class Problem(val file: String, val line: Int, val severity: String, val msg: String)

private fun collectProblems(code: String, file: String): List<Problem> {
    if (code.isEmpty()) return emptyList()
    val out = mutableListOf<Problem>()
    // lineSequence(): code.lines() materializava uma List<String> do arquivo
    // inteiro a cada tecla (o remember abaixo roda por mudança de rawText).
    code.lineSequence().forEachIndexed { i, l ->
        if (out.size >= MAX_LOCAL_PROBLEMS) return@forEachIndexed
        val t = l.trim()
        if ("TODO" in t) out += Problem(file, i + 1, "warning", t.take(80))
        if ("FIXME" in t || "XXX" in t) out += Problem(file, i + 1, "error", t.take(80))
        if ("ERROR" in t) out += Problem(file, i + 1, "error", t.take(80))
    }
    return out
}

private const val MAX_LOCAL_PROBLEMS = 50

private fun lineColOf(s: TextFieldValue): String {
    val (line, col) = lineColAt(s.text, s.selection.start)
    return "Ln ${line + 1}, Col ${col + 1}"
}

/**
 * Linha (0-based) e coluna do cursor, sem copiar o texto.
 *
 * `text.take(caret)` (o jeito óbvio) aloca uma cópia do prefixo inteiro do
 * documento — 100 KB por tecla num arquivo grande, só para desenhar
 * "Ln 12, Col 4". Aqui o custo é só o número de linhas até o cursor.
 */
fun lineColAt(text: String, caret: Int): Pair<Int, Int> {
    val end = caret.coerceIn(0, text.length)
    var line = 0
    var lastBreak = -1
    var from = 0
    while (true) {
        val nl = text.indexOf('\n', from)
        if (nl < 0 || nl >= end) break
        line++
        lastBreak = nl
        from = nl + 1
    }
    return line to (end - lastBreak - 1)
}

// ---------- THEME PICKER ----------
@Composable
private fun ThemePickerDialog(current: EditorTheme, onPick: (EditorTheme) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preferences: Color Theme") },
        text = {
            Column {
                for (t in ThemeManager.themes) {
                    Row(Modifier.fillMaxWidth().clickable { onPick(t) }.padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(18.dp).background(t.background.toCompose())
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(t.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (t == current) Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// ---------- NEW FILE/FOLDER ----------
@Composable
private fun NameDialog(title: String, hint: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var v by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            BasicTextField(
                value = v, onValueChange = { v = it }, singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                decorationBox = { inner ->
                    if (v.isEmpty()) Text(hint, fontSize = 14.sp, color = Color.Gray)
                    inner()
                }
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(v) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ---------- LINUX EMBUTIDO (setup proot + Alpine) ----------
@Composable
private fun LinuxSetupSheet(
    state: PrefixState,
    onDownload: () -> Unit,
    onOpenShell: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Termux neste app") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "O mesmo userland do Termux, instalado dentro do app: bash, " +
                        "coreutils, git, python, apt. Sem proot e sem root — os " +
                        "binários rodam direto, porque o alvo do app é API 28.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Baixa ${formatMb(TermuxEnv.BOOTSTRAP_BYTES)} do repositório " +
                        "oficial do Termux, confere o sha256 e extrai. " +
                        "Independe do app Termux instalado no celular.",
                    fontSize = 12.sp,
                    color = Color(0xFF9DA5B4)
                )
                Spacer(Modifier.height(12.dp))
                when (state) {
                    is PrefixState.Missing -> Text("Não instalado.", fontSize = 13.sp)
                    is PrefixState.Downloading -> {
                        Text(
                            "Baixando ${state.label}… ${formatMb(state.done)}" +
                                if (state.total > 0) " / ${formatMb(state.total)}" else "",
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.total > 0) {
                            LinearProgressIndicator(
                                progress = { state.done.toFloat() / state.total.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    is PrefixState.Verifying -> {
                        Text("Verificando sha256…", fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is PrefixState.Extracting -> {
                        Text(
                            "Extraindo ${state.done} de ${state.total} arquivos…",
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.done.toFloat() / state.total.coerceAtLeast(1).toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is PrefixState.Ready -> Text("Pronto. Abra um shell.", fontSize = 13.sp)
                    is PrefixState.Error -> Text(
                        "Erro: ${state.message}",
                        fontSize = 13.sp,
                        color = Color(0xFFF85149)
                    )
                }
            }
        },
        confirmButton = {
            when (state) {
                is PrefixState.Ready -> TextButton(onClick = onOpenShell) { Text("Abrir shell") }
                is PrefixState.Error, is PrefixState.Missing ->
                    TextButton(onClick = onDownload) { Text("Instalar") }
                else -> {}
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

private fun formatMb(bytes: Long): String {
    if (bytes < 0) return "?"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb < 10) "%.1f MB".format(mb) else "%d MB".format(mb.toLong())
}
