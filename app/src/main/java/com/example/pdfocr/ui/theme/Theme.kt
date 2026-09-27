package com.example.pdfocr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0055BB),
    secondary = Color(0xFF565E71),
    tertiary = Color(0xFF00695C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    secondary = Color(0xFFAEC2FF),
    tertiary = Color(0xFF82D5C5)
)

@Composable
fun PdfOcrReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
