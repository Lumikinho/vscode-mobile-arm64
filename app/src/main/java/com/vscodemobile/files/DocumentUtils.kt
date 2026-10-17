package com.vscodemobile.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** Leitura/escrita de documentos SAF (arquivos locais fora do app). */
object DocumentUtils {

    fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Lê o documento SAF classificando: texto, binário ou ilegível.
     *
     * Lê bytes crus porque a detecção precisa do byte nulo que denuncia
     * binário — decodificar antes perderia justamente esse sinal.
     */
    fun readDoc(context: Context, uri: Uri): DocContent = try {
        val stream = context.contentResolver.openInputStream(uri)
        if (stream == null) {
            DocContent.Unreadable
        } else {
            val bytes = stream.use { it.readBytes() }
            if (FileUtils.looksBinary(bytes)) {
                DocContent.Binary
            } else {
                DocContent.Text(String(bytes, Charsets.UTF_8))
            }
        }
    } catch (_: Exception) {
        DocContent.Unreadable
    }

    /** Lê o documento SAF, ou devolve `null` se não deu / não for texto. */
    fun readTextOrNull(context: Context, uri: Uri): String? =
        (readDoc(context, uri) as? DocContent.Text)?.content

    /**
     * Grava no SAF.
     *
     * O modo "wt" trunca o arquivo antes de escrever: um erro no meio deixava
     * o documento pela metade. Não há escrita atômica via SAF, então o máximo
     * que dá para garantir é não dizer que salvou quando não salvou — e
     * devolver `false` quando o stream nem abre (antes o `?.` pulava a
     * escrita e a função retornava `true` mesmo assim).
     */
    fun writeText(context: Context, uri: Uri, text: String): Boolean = try {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
        if (stream == null) {
            false
        } else {
            stream.bufferedWriter().use { it.write(text) }
            true
        }
    } catch (_: Exception) {
        false
    }

    fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "pdf" -> "application/pdf"
        "json" -> "application/json"
        else -> "text/plain"
    }
}