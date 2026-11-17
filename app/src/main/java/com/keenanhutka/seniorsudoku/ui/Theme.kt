package com.keenanhutka.seniorsudoku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Paper = lightColorScheme(
    background = Color(0xFFF4EFE4),
    onBackground = Color(0xFF1C1812),
    surface = Color(0xFFF4EFE4),
    onSurface = Color(0xFF1C1812),
    primary = Color(0xFF8A5A22),
    onPrimary = Color(0xFFF8F3EA),
)

@Composable
fun SeniorSudokuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Paper, content = content)
}
