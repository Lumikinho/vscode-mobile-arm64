package com.vscodemobile.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "package", "import", "class", "fun", "val", "var", "if", "else",
        "for", "while", "return", "object", "interface", "public", "private",
        "protected", "def", "function", "const", "let", "new",
        "try", "catch", "finally", "throw", "switch", "case", "break",
        "continue", "do", "static", "void", "int", "float", "double",
        "String", "null", "true", "false", "this", "super", "from", "as"
    )

    private val keywordRegex = Regex("\\b(${KEYWORDS.joinToString("|")})\\b")
    private val stringRegex = Regex("\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'|`[^`]*`")
    private val lineCommentRegex = Regex("//.*|#.*|--.*")
    private val blockCommentRegex = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)

    /** Highlight para o editor Compose (AnnotatedString). */
    fun highlightAnnotated(code: String, theme: EditorTheme): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        val kw = Color(theme.keyword)
        val str = Color(theme.string)
        val cmt = Color(theme.comment)
        val fg = Color(theme.foreground)

        // Marca intervalos já pintados (string/comentário) para keyword não sobrescrever
        val taken = BooleanArray(code.length)
        fun claim(r: IntRange) {
            for (i in r.first..r.last.coerceAtMost(code.length - 1)) taken[i] = true
        }

        val blocks = mutableListOf<Triple<Int, Int, SpanStyle>>()
        fun collect(regex: Regex, style: SpanStyle) {
            for (m in regex.findAll(code)) {
                blocks += Triple(m.range.first, m.range.last + 1, style)
                claim(m.range)
            }
        }

        collect(blockCommentRegex, SpanStyle(color = cmt, fontStyle = FontStyle.Italic))
        collect(stringRegex, SpanStyle(color = str))
        // comentários de linha: só fora de string
        for (m in lineCommentRegex.findAll(code)) {
            if (m.range.any { !taken[it.coerceIn(taken.indices)] }) {
                blocks += Triple(m.range.first, (m.range.last + 1).coerceAtMost(code.length),
                    SpanStyle(color = cmt, fontStyle = FontStyle.Italic))
                claim(m.range)
            }
        }
        for (m in keywordRegex.findAll(code)) {
            if (m.range.all { !taken[it] }) {
                blocks += Triple(m.range.first, m.range.last + 1, SpanStyle(color = kw))
            }
        }

        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = fg), 0, code.length)
            for ((s, e, style) in blocks) addStyle(style, s, e.coerceAtMost(code.length))
        }
    }

    fun languageOf(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "kt", "kts" -> "Kotlin"
        "java" -> "Java"
        "js" -> "JavaScript"
        "ts", "tsx" -> "TypeScript"
        "py" -> "Python"
        "c", "h" -> "C"
        "cpp", "hpp" -> "C++"
        "cs" -> "C#"
        "go" -> "Go"
        "rs" -> "Rust"
        "html", "xml" -> "HTML"
        "css" -> "CSS"
        "json" -> "JSON"
        "md" -> "Markdown"
        "sh" -> "Shell"
        "gradle" -> "Gradle"
        "yaml", "yml" -> "YAML"
        "sql" -> "SQL"
        else -> "Plain Text"
    }
}
