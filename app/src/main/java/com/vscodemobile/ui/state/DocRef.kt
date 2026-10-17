package com.vscodemobile.ui.state

import android.net.Uri
import java.io.File

/**
 * Documento aberto no editor: arquivo do workspace (File) ou documento local
 * via Storage Access Framework (Uri com permissão persistida).
 */
sealed interface DocRef {
    val id: String
    val name: String

    data class Local(val file: File) : DocRef {
        override val id: String get() = file.absolutePath
        override val name: String get() = file.name.ifBlank { file.absolutePath }
    }

    data class Saf(val uri: Uri, val displayName: String) : DocRef {
        override val id: String get() = uri.toString()
        override val name: String get() = displayName
    }
}
