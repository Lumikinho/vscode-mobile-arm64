package com.vscodemobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.Codicon
import com.vscodemobile.ui.theme.CodiconIcon
import com.vscodemobile.ui.theme.DarkModern
import com.vscodemobile.ui.theme.LightModern
import com.vscodemobile.ui.theme.LocalVsCodeColors
import com.vscodemobile.ui.theme.VsCodeTheme

/** Limites da fonte do terminal (sp), compartilhados com a persistência. */
const val MIN_TERM_FONT_SP = 10
const val MAX_TERM_FONT_SP = 28

data class NavEntry(val key: String, val icon: String, val label: String)

private enum class BarMode { NAVIGATION, EDITING, TERMINAL }

sealed interface BottomBarState {
    data class Navigation(
        val entries: List<NavEntry>,
        val activeKey: String,
        val onSelect: (String) -> Unit
    ) : BottomBarState

    data class Editing(
        val ctrlActive: Boolean,
        val altActive: Boolean,
        val canUndo: Boolean,
        val canRedo: Boolean,
        val onKey: (EditKey) -> Unit,
        val onExit: () -> Unit
    ) : BottomBarState

    data class Terminal(
        val running: Boolean,
        val fontSp: Int,
        val onFontChange: (Int) -> Unit,
        val onStart: () -> Unit,
        val onStop: () -> Unit,
        val onClear: () -> Unit,
        val onTogglePanel: () -> Unit,
        val onExit: () -> Unit
    ) : BottomBarState
}

sealed interface EditKey {
    data class Text(val value: String) : EditKey
    data object Tab : EditKey
    data object Esc : EditKey
    data object ArrowLeft : EditKey
    data object ArrowRight : EditKey
    data object ArrowUp : EditKey
    data object ArrowDown : EditKey
    data object Undo : EditKey
    data object Redo : EditKey
    data object Ctrl : EditKey
    data object Alt : EditKey
}

/**
 * Barra inferior adaptativa do workbench. Três modos com transição de ~200ms:
 * navegação (padrão), teclas de edição (IME aberto) e controles do terminal.
 */
@Composable
fun AdaptiveBottomBar(state: BottomBarState) {
    val colors = LocalVsCodeColors.current
    val mode = when (state) {
        is BottomBarState.Navigation -> BarMode.NAVIGATION
        is BottomBarState.Editing -> BarMode.EDITING
        is BottomBarState.Terminal -> BarMode.TERMINAL
    }
    AnimatedContent(
        targetState = mode,
        transitionSpec = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) togetherWith fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
        label = "AdaptiveBottomBar"
    ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(colors.activityBackground)
            ) {
                // Borda superior de 1dp, como as bordas do workbench (sem sombra)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.sideBarBorder)
                )
                when (state) {
                    is BottomBarState.Navigation -> NavigationContent(state)
                    is BottomBarState.Editing -> EditingContent(state)
                    is BottomBarState.Terminal -> TerminalContent(state)
                }
            }
    }
}

@Composable
private fun NavigationContent(state: BottomBarState.Navigation) {
    Row(Modifier.fillMaxWidth()) {
        for (entry in state.entries) {
            NavItem(entry, entry.key == state.activeKey, state.onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavItem(
    entry: NavEntry,
    selected: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    Box(
        modifier
            .height(60.dp)
            .clickable { onSelect(entry.key) },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Indicador no topo do item ativo, como a Activity Bar top/bottom
            Box(
                Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .background(if (selected) colors.activityActiveBorder else colors.activityBackground)
            )
            Spacer(Modifier.height(6.dp))
            CodiconIcon(
                entry.icon,
                tint = if (selected) colors.activityForeground else colors.activityInactiveForeground,
                sizeSp = 22
            )
            Text(
                entry.label,
                fontSize = 10.sp,
                color = if (selected) colors.activityForeground else colors.activityInactiveForeground
            )
        }
    }
}

private data class KeyDef(
    val label: String? = null,
    val icon: String? = null,
    val key: EditKey,
    val highlighted: Boolean = false,
    val dimmed: Boolean = false
)

@Composable
private fun EditingContent(state: BottomBarState.Editing) {
    val colors = LocalVsCodeColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .background(colors.activityBackground)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditTextKey("Esc", EditKey.Esc, state)
            EditTextKey("Tab", EditKey.Tab, state)
            EditTextKey("Ctrl", EditKey.Ctrl, state, highlighted = state.ctrlActive)
            EditTextKey("Alt", EditKey.Alt, state, highlighted = state.altActive)
            EditIconKey(Codicon.ArrowLeft, EditKey.ArrowLeft, state)
            EditIconKey(Codicon.ArrowUp, EditKey.ArrowUp, state)
            EditIconKey(Codicon.ArrowDown, EditKey.ArrowDown, state)
            EditIconKey(Codicon.ArrowRight, EditKey.ArrowRight, state)
            for (s in listOf(";", ":", "=", "<", ">", "/", "_", "(", ")", "{", "}", "[", "]", "\"", "'", ",")) {
                EditSymbolKey(s, state)
            }
            EditTextKey("↶", EditKey.Undo, state, dimmed = !state.canUndo)
            EditTextKey("↷", EditKey.Redo, state, dimmed = !state.canRedo)
        }
        IconButton(onClick = state.onExit, modifier = Modifier.size(48.dp)) {
            CodiconIcon(Codicon.Check, tint = colors.activityForeground, sizeSp = 20)
        }
    }
}

@Composable
private fun EditTextKey(label: String, key: EditKey, state: BottomBarState.Editing, highlighted: Boolean = false, dimmed: Boolean = false) {
    val colors = LocalVsCodeColors.current
    Box(
        Modifier
            .size(width = 52.dp, height = 48.dp)
            .clickable { state.onKey(key) }
            .background(if (highlighted) colors.accent.copy(alpha = 0.25f) else colors.activityBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = when {
                highlighted -> colors.activityForeground
                dimmed -> colors.activityInactiveForeground.copy(alpha = 0.4f)
                else -> colors.activityForeground
            }
        )
    }
}

@Composable
private fun EditSymbolKey(symbol: String, state: BottomBarState.Editing) {
    val colors = LocalVsCodeColors.current
    Box(
        Modifier
            .size(width = 48.dp, height = 48.dp)
            .clickable { state.onKey(EditKey.Text(symbol)) },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = colors.activityForeground)
    }
}

@Composable
private fun EditIconKey(icon: String, key: EditKey, state: BottomBarState.Editing) {
    val colors = LocalVsCodeColors.current
    Box(
        Modifier
            .size(width = 52.dp, height = 48.dp)
            .clickable { state.onKey(key) },
        contentAlignment = Alignment.Center
    ) {
        CodiconIcon(icon, tint = colors.activityForeground, sizeSp = 20)
    }
}

@Composable
private fun TerminalContent(state: BottomBarState.Terminal) {
    val colors = LocalVsCodeColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TermButton(Codicon.Play, "Iniciar / abrir", enabled = !state.running, onClick = state.onStart)
        TermButton(Codicon.DebugStop, "Parar", enabled = state.running, onClick = state.onStop)
        TermButton(Codicon.ClearAll, "Limpar", enabled = true, onClick = state.onClear)
        TermButton(Codicon.LayoutPanel, "Alternar painel", enabled = true, onClick = state.onTogglePanel)
        Spacer(Modifier.weight(1f))
        TermFontKey("A-", enabled = state.fontSp > MIN_TERM_FONT_SP) {
            state.onFontChange(state.fontSp - 1)
        }
        Text(
            "${state.fontSp}",
            color = colors.activityInactiveForeground,
            fontSize = 11.sp,
            modifier = Modifier.semantics { contentDescription = "Tamanho da fonte do terminal: ${state.fontSp}" }
        )
        TermFontKey("A+", enabled = state.fontSp < MAX_TERM_FONT_SP) {
            state.onFontChange(state.fontSp + 1)
        }
        IconButton(onClick = state.onExit, modifier = Modifier.size(48.dp)) {
            CodiconIcon(Codicon.Close, tint = colors.activityForeground, sizeSp = 18)
        }
    }
}

@Composable
private fun TermButton(icon: String, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalVsCodeColors.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { contentDescription = desc }
    ) {
        CodiconIcon(
            icon,
            tint = if (enabled) colors.activityForeground else colors.activityInactiveForeground.copy(alpha = 0.4f),
            sizeSp = 20
        )
    }
}

@Composable
private fun TermFontKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalVsCodeColors.current
    Box(
        Modifier
            .size(width = 44.dp, height = 40.dp)
            .clickable(enabled = enabled) { onClick() }
            .semantics { contentDescription = if (label == "A-") "Diminuir fonte do terminal" else "Aumentar fonte do terminal" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = if (enabled) colors.activityForeground else colors.activityInactiveForeground.copy(alpha = 0.4f)
        )
    }
}

// ---------- Previews ----------

private val previewEntries = listOf(
    NavEntry("explorer", Codicon.Files, "Explorer"),
    NavEntry("search", Codicon.Search, "Buscar"),
    NavEntry("scm", Codicon.SourceControl, "Git"),
    NavEntry("debug", Codicon.DebugAlt, "Executar"),
    NavEntry("extensions", Codicon.Extensions, "Extensões")
)

@Preview(name = "BottomBar navegação escura 360dp", widthDp = 360, heightDp = 120, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewBarNavDark() {
    VsCodeTheme(colors = DarkModern) {
        AdaptiveBottomBar(BottomBarState.Navigation(previewEntries, "explorer") {})
    }
}

@Preview(name = "BottomBar navegação clara 360dp", widthDp = 360, heightDp = 120, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewBarNavLight() {
    VsCodeTheme(colors = LightModern) {
        AdaptiveBottomBar(BottomBarState.Navigation(previewEntries, "debug") {})
    }
}

@Preview(name = "BottomBar edição escura 360dp", widthDp = 360, heightDp = 120, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewBarEditingDark() {
    VsCodeTheme(colors = DarkModern) {
        AdaptiveBottomBar(
            BottomBarState.Editing(
                ctrlActive = true, altActive = false,
                canUndo = true, canRedo = false,
                onKey = {}, onExit = {}
            )
        )
    }
}

@Preview(name = "BottomBar terminal clara 360dp", widthDp = 360, heightDp = 120, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewBarTerminalLight() {
    VsCodeTheme(colors = LightModern) {
        AdaptiveBottomBar(
            BottomBarState.Terminal(
                running = true, fontSp = 12, onFontChange = {},
                onStart = {}, onStop = {},
                onClear = {}, onTogglePanel = {}, onExit = {}
            )
        )
    }
}

@Preview(name = "BottomBar navegação tablet", widthDp = 1280, heightDp = 120, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewBarNavTablet() {
    VsCodeTheme(colors = DarkModern) {
        AdaptiveBottomBar(BottomBarState.Navigation(previewEntries, "search") {})
    }
}
