package com.example.liveboxmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Orange = Color(0xFFFF7900)
private val OrangeDark = Color(0xFFE35A00)

private val LightColors = lightColorScheme(
    primary = OrangeDark,
    onPrimary = Color.White,
    secondary = Color(0xFF424242),
    surface = Color(0xFFFAFAFA)
)

private val DarkColors = darkColorScheme(
    primary = Orange,
    onPrimary = Color.Black,
    secondary = Color(0xFFB0B0B0),
    surface = Color(0xFF1C1C1C)
)

@Composable
fun LiveboxManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
