package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.DarkModern
import com.vscodemobile.ui.theme.LightModern
import com.vscodemobile.ui.theme.LocalVsCodeColors
import com.vscodemobile.ui.theme.VsCodeTheme
import com.vscodemobile.ui.theme.editorTextStyle
import kotlin.math.ceil
import kotlin.math.roundToInt

private val BRACKETS = mapOf('(' to ')', '[' to ']', '{' to '}')
private val CLOSERS = mapOf(')' to '(', ']' to '[', '}' to '{')

/**
 * Linha (0-based) e coluna do cursor. Puro cálculo sobre o texto — camada UI.
 *
 * Sem `text.take(caret)`: esse caminho copia o prefixo inteiro do documento a
 * cada tecla. Aqui só se percorre até o cursor.
 */
fun cursorLineCol(text: String, caret: Int): Pair<Int, Int> {
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

/** Nível de indentação (tab = 4) pelos espaços/tabs iniciais da linha. */
fun indentLevelOf(line: String): Int {
    var spaces = 0
    for (c in line) {
        when (c) {
            ' ' -> spaces++
            '\t' -> spaces += 4
            else -> break
        }
    }
    return spaces / 4
}

/** Parêntese correspondente ao redor do cursor (busca ingênua; ignora strings). */
fun matchingBracket(text: String, caret: Int): Pair<Int, Int>? {
    val candidates = listOf(caret - 1, caret)
    for (pos in candidates) {
        if (pos < 0 || pos >= text.length) continue
        val ch = text[pos]
        val open = BRACKETS[ch]
        if (open != null) {
            var depth = 0
            for (i in pos until text.length) {
                if (text[i] == ch) depth++
                else if (text[i] == open) {
                    depth--
                    if (depth == 0) return pos to i
                }
            }
            return null
        }
        val opener = CLOSERS[ch]
        if (opener != null) {
            var depth = 0
            for (i in pos downTo 0) {
                if (text[i] == ch) depth++
                else if (text[i] == opener) {
                    depth--
                    if (depth == 0) return i to pos
                }
            }
            return null
        }
    }
    return null
}

/**
 * Superfície do editor: gutter discreto, destaque da linha atual, guias de
 * indentação, bracket matching e pinça para zoom. Recebe o AnnotatedString
 * pronto do highlight — não altera a lógica do editor.
 *
 * Gutter e realces são **virtualizados**: só a faixa visível (+ overscan) é
 * compostá. Antes eram N Texts + N Boxes remontados a cada tecla, com N = número
 * de linhas do arquivo (até 3000 pelo cutoff de highlight).
 */
@Composable
fun EditorSurface(
    state: TextFieldValue,
    highlighted: AnnotatedString,
    fontSizeSp: Int,
    onFontSizeChange: (Int) -> Unit,
    empty: Boolean,
    onChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalVsCodeColors.current
    val style = colors.editorTextStyle(fontSizeSp)
    val lineH = (fontSizeSp + 6).dp
    // Largura aprox. de 4 espaços em monospace (0.6em por caractere)
    val indentStep = (fontSizeSp * 2.4f).dp
    val density = LocalDensity.current

    val zoom = rememberTransformableState { zoomChange, _, _ ->
        onFontSizeChange((fontSizeSp * zoomChange).roundToInt().coerceIn(10, 28))
    }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    var viewportPx by remember { mutableIntStateOf(0) }

    // viewportPx é a altura VISÍVEL: medida na Box externa. Dentro da cadeia de
    // scroll, onSizeChanged mediria o conteúdo inteiro e a virtualização não
    // faria nada.
    Box(modifier.background(colors.editorBackground).onSizeChanged { viewportPx = it.height }) {
        if (empty) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("VSCode Mobile", fontSize = 20.sp, color = style.color.copy(alpha = 0.7f))
                Text(
                    "Toque em Explorer na barra inferior para abrir arquivos",
                    fontSize = 12.sp,
                    color = style.color.copy(alpha = 0.5f)
                )
            }
            return@Box
        }

        val lines = remember(state.text) { state.text.split('\n') }
        val caret = state.selection.start.coerceIn(0, state.text.length)
        val (cursorLine, _) = cursorLineCol(state.text, caret)

        // Janela visível: só essa faixa de linhas vira composable. O scroll
        // continua valendo o arquivo inteiro porque o BasicTextField define
        // a altura do conteúdo — as colunas do gutter/realces só recortam.
        val linePx = with(density) { lineH.toPx() }
        val window by remember(lines, linePx, viewportPx) {
            derivedStateOf {
                if (linePx <= 0f) {
                    0 until minOf(lines.size, OVERSCAN_LINES * 2)
                } else {
                    val first = ((vScroll.value / linePx).toInt() - OVERSCAN_LINES)
                        .coerceAtLeast(0)
                    val count = ceil(viewportPx / linePx).toInt() + OVERSCAN_LINES * 2
                    first until (first + count).coerceAtMost(lines.size)
                }
            }
        }
        val firstVisible = window.first
        val lastVisible = window.last
        val windowOffset = lineH * firstVisible

        // Bracket matching como spans extras sobre o highlight recebido
        val display = remember(highlighted, caret) {
            val match = matchingBracket(state.text, caret)
            if (match == null || highlighted.text != state.text) {
                if (highlighted.text == state.text) highlighted
                else AnnotatedString(state.text)
            } else {
                buildAnnotatedString {
                    append(highlighted)
                    val bg = SpanStyle(background = colors.accent.copy(alpha = 0.3f))
                    addStyle(bg, match.first, (match.first + 1).coerceAtMost(state.text.length))
                    addStyle(bg, match.second, (match.second + 1).coerceAtMost(state.text.length))
                }
            }
        }
        val fieldValue = if (display.text == state.text) state.copy(annotatedString = display) else state

        Row(
            Modifier
                .fillMaxSize()
                .transformable(zoom)
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll)
                .padding(8.dp)
        ) {
            // Gutter: números discretos, linha ativa em destaque (só a janela)
            Column(Modifier.padding(end = 10.dp)) {
                Spacer(Modifier.height(windowOffset))
                for (i in firstVisible until lastVisible) {
                    Text(
                        "${i + 1}",
                        style = style.copy(
                            color = if (i == cursorLine) colors.lineNumberActive else colors.lineNumber,
                            fontSize = (fontSizeSp - 1).sp
                        ),
                        modifier = Modifier.height(lineH)
                    )
                }
            }
            Box {
                // Camada de realces por linha: linha atual + guias de indentação
                Column {
                    Spacer(Modifier.height(windowOffset))
                    for (i in firstVisible until lastVisible) {
                        Box(Modifier.height(lineH)) {
                            if (i == cursorLine) {
                                Box(
                                    Modifier
                                        .width(EDITOR_LAYER_WIDTH)
                                        .height(lineH)
                                        .background(colors.lineHighlight)
                                )
                            }
                            val level = indentLevelOf(lines[i])
                            for (g in 0 until level) {
                                Box(
                                    Modifier
                                        .offset(x = indentStep * g)
                                        .width(1.dp)
                                        .height(lineH)
                                        .background(colors.indentGuide)
                                )
                            }
                        }
                    }
                }
                BasicTextField(
                    value = fieldValue,
                    onValueChange = onChange,
                    textStyle = style,
                    cursorBrush = SolidColor(style.color),
                    modifier = Modifier.width(EDITOR_LAYER_WIDTH),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

/** Linhas extras compostas acima e abaixo da tela, para o scroll não piscar. */
private const val OVERSCAN_LINES = 8

/**
 * Largura da camada de texto/realce. Mantida em 1200dp como antes: não é
 * medida, precisa ser larga o bastante para a linha mais longa caber, e
 * qualquer outro valor muda o alcance do scroll horizontal.
 */
private val EDITOR_LAYER_WIDTH = 1200.dp

private const val SAMPLE = """fun main() {
    // Bem-vindo ao VSCode Mobile!
    val msg = "Olá, ARM64"
    if (msg.isNotEmpty()) {
        println(msg)
    }
}
"""

@Preview(name = "Editor escuro 360dp", widthDp = 360, heightDp = 600, showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun PreviewEditorDark() {
    VsCodeTheme(colors = DarkModern) {
        val highlighted = androidx.compose.ui.text.AnnotatedString(SAMPLE)
        EditorSurface(
            state = TextFieldValue(SAMPLE, androidx.compose.ui.text.TextRange(78)),
            highlighted = highlighted,
            fontSizeSp = 13,
            onFontSizeChange = {},
            empty = false,
            onChange = {}
        )
    }
}

@Preview(name = "Editor claro 360dp", widthDp = 360, heightDp = 600, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewEditorLight() {
    VsCodeTheme(colors = LightModern) {
        val highlighted = androidx.compose.ui.text.AnnotatedString(SAMPLE)
        EditorSurface(
            state = TextFieldValue(SAMPLE, androidx.compose.ui.text.TextRange(10)),
            highlighted = highlighted,
            fontSizeSp = 13,
            onFontSizeChange = {},
            empty = false,
            onChange = {}
        )
    }
}

@Preview(name = "Editor tablet", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF1F1F1F)
@Composable
private fun PreviewEditorTablet() {
    VsCodeTheme(colors = DarkModern) {
        val highlighted = androidx.compose.ui.text.AnnotatedString(SAMPLE)
        EditorSurface(
            state = TextFieldValue(SAMPLE, androidx.compose.ui.text.TextRange(78)),
            highlighted = highlighted,
            fontSizeSp = 13,
            onFontSizeChange = {},
            empty = false,
            onChange = {}
        )
    }
}
