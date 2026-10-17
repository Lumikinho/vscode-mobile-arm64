package com.vscodemobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tipografia: sans do sistema na UI. Monoespaçada só no editor (via editorTextStyle). */
val VsCodeTypography = Typography()

/** Shapes discretos 4–6dp, sem cantos exagerados. */
val VsCodeShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(6.dp)
)

/** Espaçamentos do workbench. */
object VsCodeSpacing {
    val s2: Dp = 2.dp
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s16: Dp = 16.dp
    val s24: Dp = 24.dp
}

/** Estilo de texto do editor (monoespaçado). Tamanho ajustável por pinça. */
fun VsCodeColors.editorTextStyle(fontSize: Int = 13): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = fontSize.sp,
    lineHeight = (fontSize + 6).sp,
    color = editorForeground
)

@Composable
fun VsCodeTheme(
    colors: VsCodeColors = DarkModern,
    content: @Composable () -> Unit
) {
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.editorBackground,
            onBackground = colors.editorForeground,
            surface = colors.sideBarBackground,
            onSurface = colors.sideBarForeground,
            surfaceVariant = colors.inputBackground,
            onSurfaceVariant = colors.sideBarForeground,
            outline = colors.sideBarBorder,
            outlineVariant = colors.tabBorder,
            error = colors.error
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.editorBackground,
            onBackground = colors.editorForeground,
            surface = colors.sideBarBackground,
            onSurface = colors.sideBarForeground,
            surfaceVariant = colors.inputBackground,
            onSurfaceVariant = colors.sideBarForeground,
            outline = colors.sideBarBorder,
            outlineVariant = colors.tabBorder,
            error = colors.error
        )
    }
    CompositionLocalProvider(LocalVsCodeColors provides colors) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = scheme,
            typography = VsCodeTypography,
            shapes = VsCodeShapes,
            content = content
        )
    }
}
