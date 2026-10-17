package com.vscodemobile.editor

import androidx.compose.ui.graphics.Color

data class EditorTheme(
    val name: String,
    val background: Int,
    val foreground: Int,
    val keyword: Int,
    val string: Int,
    val comment: Int,
    val lineNumberBg: Int,
    val isDark: Boolean
)

/** Cores do "chrome" do VS Code (activity bar, sidebar, tabs, status bar). */
data class VsCodeChrome(
    val activityBar: Color,
    val sideBar: Color,
    val sideBarSection: Color,
    val tabBar: Color,
    val tabActive: Color,
    val tabInactive: Color,
    val statusBar: Color,
    val statusBarFg: Color,
    val titleBar: Color,
    val border: Color,
    val panel: Color
)

object ThemeManager {
    const val PREFS = "editor_themes"
    const val KEY = "current_theme"

    val themes = listOf(
        EditorTheme(
            "Dark+ (VSCode)", 0xFF1E1E1E.toInt(), 0xFFD4D4D4.toInt(),
            0xFF569CD6.toInt(), 0xFFCE9178.toInt(), 0xFF6A9955.toInt(),
            0xFF252526.toInt(), true
        ),
        EditorTheme(
            "Light+ (VSCode)", android.graphics.Color.WHITE, 0xFF333333.toInt(),
            0xFF0000FF.toInt(), 0xFFA31515.toInt(), 0xFF008000.toInt(),
            0xFFF3F3F3.toInt(), false
        ),
        EditorTheme(
            "Monokai", 0xFF272822.toInt(), 0xFFF8F8F2.toInt(),
            0xFFF92672.toInt(), 0xFFE6DB74.toInt(), 0xFF75715E.toInt(),
            0xFF1E1F1C.toInt(), true
        ),
        EditorTheme(
            "Dracula", 0xFF282A36.toInt(), 0xFFF8F8F2.toInt(),
            0xFFBD93F9.toInt(), 0xFFF1FA8C.toInt(), 0xFF6272A4.toInt(),
            0xFF21222C.toInt(), true
        )
    )

    fun byName(name: String): EditorTheme =
        themes.firstOrNull { it.name == name } ?: themes[0]

    /** Chrome espelhado do workbench do VS Code desktop. */
    fun chromeOf(theme: EditorTheme): VsCodeChrome = when (theme.name) {
        "Light+ (VSCode)" -> VsCodeChrome(
            activityBar = Color(0xFFE9E9E9),
            sideBar = Color(0xFFF3F3F3),
            sideBarSection = Color(0xFF767676),
            tabBar = Color(0xFFECECEC),
            tabActive = Color.White,
            tabInactive = Color(0xFFECECEC),
            statusBar = Color(0xFF007ACC),
            statusBarFg = Color.White,
            titleBar = Color(0xFFDDDDDD),
            border = Color(0xFFE0E0E0),
            panel = Color(0xFFF3F3F3)
        )
        "Monokai" -> VsCodeChrome(
            activityBar = Color(0xFF1E1F1C),
            sideBar = Color(0xFF1E1F1C),
            sideBarSection = Color(0xFF75715E),
            tabBar = Color(0xFF1E1F1C),
            tabActive = Color(0xFF272822),
            tabInactive = Color(0xFF1E1F1C),
            statusBar = Color(0xFFA6E22E),
            statusBarFg = Color(0xFF272822),
            titleBar = Color(0xFF1E1F1C),
            border = Color(0xFF3E3D32),
            panel = Color(0xFF1E1F1C)
        )
        "Dracula" -> VsCodeChrome(
            activityBar = Color(0xFF21222C),
            sideBar = Color(0xFF21222C),
            sideBarSection = Color(0xFF6272A4),
            tabBar = Color(0xFF21222C),
            tabActive = Color(0xFF282A36),
            tabInactive = Color(0xFF21222C),
            statusBar = Color(0xFFBD93F9),
            statusBarFg = Color(0xFF282A36),
            titleBar = Color(0xFF21222C),
            border = Color(0xFF44475A),
            panel = Color(0xFF21222C)
        )
        else -> VsCodeChrome(
            // Dark+ desktop: activity #333, sidebar #252526, tab #2D2D2D, status #007ACC
            activityBar = Color(0xFF333333),
            sideBar = Color(0xFF252526),
            sideBarSection = Color(0xFFBBBBBB),
            tabBar = Color(0xFF2D2D2D),
            tabActive = Color(0xFF1E1E1E),
            tabInactive = Color(0xFF2D2D2D),
            statusBar = Color(0xFF007ACC),
            statusBarFg = Color.White,
            titleBar = Color(0xFF3C3C3C),
            border = Color(0xFF2D2D2D),
            panel = Color(0xFF1E1E1E)
        )
    }
}

fun Int.toCompose(): Color = Color(this)
