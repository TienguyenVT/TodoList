package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun ZenTaskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = NeumorphicColors.background,
            surface = NeumorphicColors.surface,
            primary = NeumorphicColors.textPrimary
        ),
        content = content
    )
}