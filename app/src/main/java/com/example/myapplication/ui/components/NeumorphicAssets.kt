package com.example.myapplication.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.NeumorphicColors

object NeumorphicAssets {
    // Precomputed brushes and shapes to avoid recreating them on every recomposition
    val cardBrush: Brush = Brush.linearGradient(listOf(NeumorphicColors.surface, NeumorphicColors.surface))
    val cardPressedBrush: Brush = Brush.linearGradient(listOf(NeumorphicColors.darkShadow.copy(0.1f), NeumorphicColors.lightShadow.copy(0.1f)))
    val cardShape = RoundedCornerShape(20.dp)
}
