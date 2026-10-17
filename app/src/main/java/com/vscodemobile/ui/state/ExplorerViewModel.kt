package com.vscodemobile.ui.state

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vscodemobile.ui.components.FileNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Listagem da pasta local fora do ciclo da composição: sobrevive ao fechar
 * da palette/sheet e cancela cargas obsoletas. Erros vão para [errors]
 * (a UI mostra em Snackbar — nunca como falha de cancelamento).
 */
class ExplorerViewModel(app: Application) : AndroidViewModel(app) {

    private val _nodes = MutableStateFlow<List<FileNode>>(emptyList())
    val nodes: StateFlow<List<FileNode>> = _nodes.asStateFlow()

    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var job: Job? = null
    private var currentRoot: Uri? = null

    /**
     * Só a raiz corrente é consultada (por [load] e [toggleHidden]), então o
     * cache guarda **uma** árvore. Antes era um Map sem eviction: trocar de pasta
     * SAF N vezes mantinha N árvores inteiras na heap.
     */
    private var cachedKey: String? = null
    private var cachedNodes: List<FileNode> = emptyList()

    fun load(root: Uri, force: Boolean = false) {
        currentRoot = root
        val key = root.toString()
        if (!force && key == cachedKey) {
            _nodes.value = filtered(cachedNodes)
            return
        }
        job?.cancel()
        job = viewModelScope.launch {
            try {
                val all = withContext(Dispatchers.IO) { safTree(getApplication(), root) }
                cachedKey = key
                cachedNodes = all
                _nodes.value = filtered(all)
                Log.d("VsCodeMobile", "itens na pasta $root: ${all.size}")
                if (all.isEmpty()) _errors.emit("Pasta vazia ou sem acesso")
            } catch (e: CancellationException) {
                throw e // cancelamento (troca de pasta/refresh) não é falha
            } catch (e: Exception) {
                Log.e("VsCodeMobile", "falha ao listar $root", e)
                _errors.emit("Falha ao listar pasta: ${e.message}")
            }
        }
    }

    fun refresh() {
        currentRoot?.let { load(it, force = true) }
    }

    fun toggleHidden() {
        _showHidden.value = !_showHidden.value
        val root = currentRoot ?: return
        if (root.toString() == cachedKey) _nodes.value = filtered(cachedNodes)
    }

    fun clear() {
        job?.cancel()
        currentRoot = null
        cachedKey = null
        cachedNodes = emptyList()
        _nodes.value = emptyList()
    }

    private fun filtered(all: List<FileNode>): List<FileNode> {
        if (_showHidden.value) return all
        return all.filterNot { it.name.startsWith(".") }.map {
            if (it.isDirectory) it.copy(children = filtered(it.children)) else it
        }
    }

    /**
     * Árvore da pasta local (SAF), com limite de profundidade.
     *
     * Usa [rawTreeList] como caminho padrão: uma query por diretório. O
     * DocumentFile seria mais legível, mas `listFiles()` faz uma query por
     * arquivo só para ler nome/tipo/tamanho — e nunca lança exceção, ou seja,
     * o fallback daqui nunca era alcançado na prática.
     */
    private fun safTree(context: Context, root: Uri): List<FileNode> =
        try {
            rawTreeList(root)
        } catch (_: Exception) {
            val tree = DocumentFile.fromTreeUri(context, root)
                ?: throw IllegalArgumentException("URI não é árvore SAF")
            tree.listFiles()
                .sortedWith(compareBy({ !it.isDirectory }, { (it.name ?: "").lowercase() })
                )
                .map { it.toSafNode(context, 0) }
        }

    private fun DocumentFile.toSafNode(context: Context, depth: Int): FileNode {
        val kids = if (isDirectory && depth < 2) {
            listFiles()
                .sortedWith(compareBy({ !it.isDirectory }, { (it.name ?: "").lowercase() }))
                .map { it.toSafNode(context, depth + 1) }
        } else emptyList()
        return FileNode(name ?: "?", uri.toString(), isDirectory, kids, length())
    }

    /** Listagem crua via DocumentsContract: nome, tipo e tamanho numa só consulta. */
    private fun rawTreeList(treeUri: Uri, context: Context = getApplication()): List<FileNode> {
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        return queryKids(context, treeUri, treeId, 0)
    }

    private fun queryKids(context: Context, treeUri: Uri, parentId: String, depth: Int): List<FileNode> {
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
                val kids = if (isDir && depth < 2) queryKids(context, treeUri, docId, depth + 1) else emptyList()
                out += FileNode(name, docUri.toString(), isDir, kids, size)
            }
        }
        return out.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
}
