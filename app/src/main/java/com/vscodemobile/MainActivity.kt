package com.vscodemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vscodemobile.editor.ThemeManager
import com.vscodemobile.ui.VsCodeApp
import java.io.File

class MainActivity : ComponentActivity() {

    private val workspace: File by lazy { filesDir.resolve("workspace") }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(ThemeManager.PREFS, MODE_PRIVATE)
        val initial = ThemeManager.byName(prefs.getString(ThemeManager.KEY, null) ?: ThemeManager.themes[0].name)
        setContent {
            VsCodeApp(
                workspace = workspace,
                filesDirPath = filesDir.absolutePath,
                initialTheme = initial,
                onThemeChange = { prefs.edit().putString(ThemeManager.KEY, it.name).apply() }
            )
        }
    }
}
