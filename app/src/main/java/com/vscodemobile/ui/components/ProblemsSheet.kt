package com.vscodemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vscodemobile.ui.theme.Codicon
import com.vscodemobile.ui.theme.CodiconIcon
import com.vscodemobile.ui.theme.DarkModern
import com.vscodemobile.ui.theme.LightModern
import com.vscodemobile.ui.theme.LocalVsCodeColors
import com.vscodemobile.ui.theme.VsCodeTheme

enum class ProblemSeverity { ERROR, WARNING }

data class ProblemItem(
    val file: String,
    val line: Int,
    val severity: ProblemSeverity,
    val message: String
)

/**
 * Conteúdo do painel (vai dentro do bottom sheet arrastável do parent):
 * abas Problems/Output/Terminal com borda de ativo, como panelTitle.*.
 */
@Composable
fun ProblemsSheetContent(
    activeTab: Int,
    onTabSelect: (Int) -> Unit,
    problemCount: Int,
    problems: List<ProblemItem>,
    output: List<String>,
    terminalContent: @Composable () -> Unit,
    onOpenProblem: (ProblemItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    contentHeight: Dp = 430.dp
) {
    val colors = LocalVsCodeColors.current
    Column(modifier.fillMaxWidth().height(contentHeight)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            PanelTab("PROBLEMS $problemCount", selected = activeTab == 0, onClick = { onTabSelect(0) })
            PanelTab("OUTPUT", selected = activeTab == 1, onClick = { onTabSelect(1) })
            PanelTab("TERMINAL", selected = activeTab == 2, onClick = { onTabSelect(2) })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                CodiconIcon(Codicon.Close, tint = colors.panelTabInactiveForeground, sizeSp = 18)
            }
        }
        when (activeTab) {
            0 -> LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                if (problems.isEmpty()) item {
                    Text(
                        "No problems have been detected in the workspace.",
                        color = colors.panelTabInactiveForeground,
                        fontSize = 12.sp
                    )
                }
                items(problems) { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenProblem(p) }
                            .padding(vertical = 4.dp)
                            .semantics {
                                contentDescription =
                                    "${p.file} linha ${p.line}: ${p.message}"
                            }
                    ) {
                        CodiconIcon(
                            if (p.severity == ProblemSeverity.ERROR) Codicon.Error else Codicon.Warning,
                            tint = if (p.severity == ProblemSeverity.ERROR) colors.error else colors.warning,
                            sizeSp = 15
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${p.file}:${p.line} — ${p.message}",
                            color = colors.panelTabActiveForeground,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            1 -> LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                items(output) {
                    Text(it, color = colors.panelTabActiveForeground, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
            else -> terminalContent()
        }
    }
}

@Composable
private fun PanelTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalVsCodeColors.current
    Column(
        Modifier
            .height(48.dp)
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = if (selected) colors.panelTabActiveForeground else colors.panelTabInactiveForeground,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(3.dp))
        androidx.compose.foundation.layout.Box(
            Modifier
                .width(28.dp)
                .height(2.dp)
                .background(if (selected) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}

private val previewProblems = listOf(
    ProblemItem("main.kt", 3, ProblemSeverity.WARNING, "TODO: tratar nome vazio"),
    ProblemItem("main.kt", 9, ProblemSeverity.ERROR, "FIXME: índice fora do limite")
)

@Preview(name = "Problems escuro 360dp", widthDp = 360, heightDp = 500, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewProblemsDark() {
    VsCodeTheme(colors = DarkModern) {
        ProblemsSheetContent(
            activeTab = 0, onTabSelect = {}, problemCount = 2,
            problems = previewProblems, output = emptyList(),
            terminalContent = {}, onOpenProblem = {}, onClose = {}
        )
    }
}

@Preview(name = "Problems claro 360dp", widthDp = 360, heightDp = 500, showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun PreviewProblemsLight() {
    VsCodeTheme(colors = LightModern) {
        ProblemsSheetContent(
            activeTab = 0, onTabSelect = {}, problemCount = 2,
            problems = previewProblems, output = emptyList(),
            terminalContent = {}, onOpenProblem = {}, onClose = {}
        )
    }
}

@Preview(name = "Problems tablet", widthDp = 1280, heightDp = 500, showBackground = true, backgroundColor = 0xFF181818)
@Composable
private fun PreviewProblemsTablet() {
    VsCodeTheme(colors = DarkModern) {
        ProblemsSheetContent(
            activeTab = 0, onTabSelect = {}, problemCount = 2,
            problems = previewProblems, output = emptyList(),
            terminalContent = {}, onOpenProblem = {}, onClose = {}
        )
    }
}
