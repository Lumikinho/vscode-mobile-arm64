package com.vscodemobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vscodemobile.editor.EditorTheme

/**
 * Tokens do workbench do VS Code, valores extraídos de dark_modern.json e
 * light_modern.json oficiais (microsoft/vscode, extensions/theme-defaults).
 */
@Immutable
data class VsCodeColors(
    val isDark: Boolean,
    // Editor
    val editorBackground: Color,
    val editorForeground: Color,
    val lineNumber: Color,
    val lineNumberActive: Color,
    val lineHighlight: Color,
    val selection: Color,
    val indentGuide: Color,
    val bracket1: Color,
    val bracket2: Color,
    val bracket3: Color,
    // Side bar / explorer
    val sideBarBackground: Color,
    val sideBarForeground: Color,
    val sideBarBorder: Color,
    val sectionHeaderForeground: Color,
    val treeIndentGuide: Color,
    // Activity bar
    val activityBackground: Color,
    val activityForeground: Color,
    val activityInactiveForeground: Color,
    val activityActiveBorder: Color,
    // Title bar
    val titleBarBackground: Color,
    val titleBarForeground: Color,
    val titleBarBorder: Color,
    // Tabs
    val tabBarBackground: Color,
    val tabActiveBackground: Color,
    val tabActiveForeground: Color,
    val tabActiveBorderTop: Color,
    val tabInactiveBackground: Color,
    val tabInactiveForeground: Color,
    val tabBorder: Color,
    // Status bar (neutra no Modern, nunca bloco cheio)
    val statusBackground: Color,
    val statusForeground: Color,
    val statusBorder: Color,
    // Panel
    val panelBackground: Color,
    val panelBorder: Color,
    val panelTabActiveForeground: Color,
    val panelTabInactiveForeground: Color,
    // Listas
    val listSelectionBackground: Color,
    val listSelectionForeground: Color,
    val listHoverBackground: Color,
    // Inputs / botões / semânticas
    val inputBackground: Color,
    val inputBorder: Color,
    val inputPlaceholder: Color,
    val focusBorder: Color,
    val accent: Color,
    val onAccent: Color,
    val error: Color,
    val warning: Color
)

val DarkModern = VsCodeColors(
    isDark = true,
    editorBackground = Color(0xFF1F1F1F),
    editorForeground = Color(0xFFCCCCCC),
    lineNumber = Color(0xFF6E7681),
    lineNumberActive = Color(0xFFCCCCCC),
    lineHighlight = Color(0x0FFFFFFF),
    selection = Color(0xFF094771),
    indentGuide = Color(0xFF404040),
    bracket1 = Color(0xFFFFD700),
    bracket2 = Color(0xFFDA70D6),
    bracket3 = Color(0xFF179FFF),
    sideBarBackground = Color(0xFF181818),
    sideBarForeground = Color(0xFFCCCCCC),
    sideBarBorder = Color(0xFF2B2B2B),
    sectionHeaderForeground = Color(0xFFCCCCCC),
    treeIndentGuide = Color(0xFF585858),
    activityBackground = Color(0xFF181818),
    activityForeground = Color(0xFFD7D7D7),
    activityInactiveForeground = Color(0xFF868686),
    activityActiveBorder = Color(0xFF0078D4),
    titleBarBackground = Color(0xFF181818),
    titleBarForeground = Color(0xFFCCCCCC),
    titleBarBorder = Color(0xFF2B2B2B),
    tabBarBackground = Color(0xFF181818),
    tabActiveBackground = Color(0xFF1F1F1F),
    tabActiveForeground = Color(0xFFFFFFFF),
    tabActiveBorderTop = Color(0xFF0078D4),
    tabInactiveBackground = Color(0xFF181818),
    tabInactiveForeground = Color(0xFF9D9D9D),
    tabBorder = Color(0xFF2B2B2B),
    statusBackground = Color(0xFF181818),
    statusForeground = Color(0xFFCCCCCC),
    statusBorder = Color(0xFF2B2B2B),
    panelBackground = Color(0xFF181818),
    panelBorder = Color(0xFF2B2B2B),
    panelTabActiveForeground = Color(0xFFCCCCCC),
    panelTabInactiveForeground = Color(0xFF9D9D9D),
    listSelectionBackground = Color(0xFF094771),
    listSelectionForeground = Color(0xFFFFFFFF),
    listHoverBackground = Color(0xFF2A2D2E),
    inputBackground = Color(0xFF313131),
    inputBorder = Color(0xFF3C3C3C),
    inputPlaceholder = Color(0xFF989898),
    focusBorder = Color(0xFF0078D4),
    accent = Color(0xFF0078D4),
    onAccent = Color(0xFFFFFFFF),
    error = Color(0xFFF85149),
    warning = Color(0xFFCCA700)
)

val LightModern = VsCodeColors(
    isDark = false,
    editorBackground = Color(0xFFFFFFFF),
    editorForeground = Color(0xFF3B3B3B),
    lineNumber = Color(0xFF6E7681),
    lineNumberActive = Color(0xFF171184),
    lineHighlight = Color(0x0A000000),
    selection = Color(0xFFADD6FF),
    indentGuide = Color(0xFFD3D3D3),
    bracket1 = Color(0xFF0431FA),
    bracket2 = Color(0xFF319331),
    bracket3 = Color(0xFF7B3814),
    sideBarBackground = Color(0xFFF8F8F8),
    sideBarForeground = Color(0xFF3B3B3B),
    sideBarBorder = Color(0xFFE5E5E5),
    sectionHeaderForeground = Color(0xFF3B3B3B),
    treeIndentGuide = Color(0xFFBDBDBD),
    activityBackground = Color(0xFFF8F8F8),
    activityForeground = Color(0xFF1F1F1F),
    activityInactiveForeground = Color(0xFF616161),
    activityActiveBorder = Color(0xFF005FB8),
    titleBarBackground = Color(0xFFF8F8F8),
    titleBarForeground = Color(0xFF1E1E1E),
    titleBarBorder = Color(0xFFE5E5E5),
    tabBarBackground = Color(0xFFF8F8F8),
    tabActiveBackground = Color(0xFFFFFFFF),
    tabActiveForeground = Color(0xFF3B3B3B),
    tabActiveBorderTop = Color(0xFF005FB8),
    tabInactiveBackground = Color(0xFFF8F8F8),
    tabInactiveForeground = Color(0xFF616161),
    tabBorder = Color(0xFFE5E5E5),
    statusBackground = Color(0xFFF8F8F8),
    statusForeground = Color(0xFF3B3B3B),
    statusBorder = Color(0xFFE5E5E5),
    panelBackground = Color(0xFFF8F8F8),
    panelBorder = Color(0xFFE5E5E5),
    panelTabActiveForeground = Color(0xFF3B3B3B),
    panelTabInactiveForeground = Color(0xFF3B3B3B),
    listSelectionBackground = Color(0xFFE8E8E8),
    listSelectionForeground = Color(0xFF000000),
    listHoverBackground = Color(0xFFF2F2F2),
    inputBackground = Color(0xFFFFFFFF),
    inputBorder = Color(0xFFCECECE),
    inputPlaceholder = Color(0xFF767676),
    focusBorder = Color(0xFF005FB8),
    accent = Color(0xFF005FB8),
    onAccent = Color(0xFFFFFFFF),
    error = Color(0xFFF85149),
    warning = Color(0xFFBF8803)
)

/** Mapeia o tema legado (seletor atual do app) para os tokens novos. */
fun vscodeColorsFor(theme: EditorTheme): VsCodeColors = when (theme.name) {
    "Light+ (VSCode)" -> LightModern
    "Monokai" -> DarkModern.copy(
        editorBackground = Color(theme.background),
        editorForeground = Color(theme.foreground),
        lineNumber = Color(theme.comment),
        selection = Color(0xFF49483E),
        tabActiveBackground = Color(theme.background)
    )
    "Dracula" -> DarkModern.copy(
        editorBackground = Color(theme.background),
        editorForeground = Color(theme.foreground),
        lineNumber = Color(theme.comment),
        selection = Color(0xFF44475A),
        accent = Color(0xFFBD93F9),
        activityActiveBorder = Color(0xFFBD93F9),
        tabActiveBorderTop = Color(0xFFBD93F9),
        focusBorder = Color(0xFFBD93F9),
        tabActiveBackground = Color(theme.background)
    )
    else -> DarkModern
}

val LocalVsCodeColors = staticCompositionLocalOf { DarkModern }
