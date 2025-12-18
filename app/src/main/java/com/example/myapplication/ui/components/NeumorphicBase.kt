package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.NeumorphicColors

@Composable
fun NeumorphicCard(isPressed: Boolean = false, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .shadow(if (isPressed) 0.dp else 10.dp, RoundedCornerShape(20.dp), ambientColor = NeumorphicColors.darkShadow, spotColor = NeumorphicColors.lightShadow)
            .background(
                if (isPressed) Brush.linearGradient(listOf(NeumorphicColors.darkShadow.copy(0.1f), NeumorphicColors.lightShadow.copy(0.1f)))
                else Brush.linearGradient(listOf(NeumorphicColors.surface, NeumorphicColors.surface)),
                RoundedCornerShape(20.dp)
            )
    ) { content() }
}

@Composable
fun NeumorphicButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(NeumorphicColors.accentBlue, NeumorphicColors.accentBlue.copy(0.8f))), RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = NeumorphicColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NeumorphicTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(
        Modifier.fillMaxWidth().shadow(0.dp, RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(NeumorphicColors.darkShadow.copy(0.1f), NeumorphicColors.lightShadow.copy(0.05f))), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, color = NeumorphicColors.textSecondary, fontSize = 16.sp)
        BasicTextField(value = value, onValueChange = onValueChange, textStyle = TextStyle(color = NeumorphicColors.textPrimary, fontSize = 16.sp), modifier = Modifier.fillMaxWidth())
    }
}