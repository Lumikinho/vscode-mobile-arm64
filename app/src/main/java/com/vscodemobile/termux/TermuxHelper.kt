package com.vscodemobile.termux

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.vscodemobile.R
import java.io.File

object TermuxHelper {

    const val PKG = "com.termux"
    const val ACTION_RUN_COMMAND = "com.termux.app.RUN_COMMAND"

    /** Abre o Termux na pasta do arquivo atual via intent. */
    fun openInTermux(context: Context, currentFile: File?) {
        val workDir = (currentFile?.parentFile?.absolutePath
            ?: "/data/data/com.termux/files/home")

        // 1) Tenta RUN_COMMAND (Termux:API moderno) — abre shell no workdir
        try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setClassName(PKG, "com.termux.app.RunCommandService")
                putExtra("com.termux.execute.background", false)
                putExtra("com.termux.execute.workdir", workDir)
                putExtra(
                    "com.termux.execute.command",
                    arrayOf("sh", "-c", "cd '$workDir' 2>/dev/null; exec bash")
                )
            }
            // RUN_COMMAND é um Service; usa startForegroundService abaixo do O
            context.startService(intent)
            // Também traz o app para frente
            launchApp(context)
            return
        } catch (_: Exception) {
            // cai para o fallback
        }

        // 2) Fallback: só abre o app Termux
        if (!launchApp(context)) {
            Toast.makeText(context, R.string.termux_not_found, Toast.LENGTH_LONG).show()
        }
    }

    private fun launchApp(context: Context): Boolean {
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(PKG)
                ?: return false
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
