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

    fun readText(context: Context, uri: Uri): String = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
    } catch (_: Exception) {
        ""
    }

    fun writeText(context: Context, uri: Uri, text: String): Boolean = try {
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(text)
        }
        true
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
