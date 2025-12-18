package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.format.DateTimeFormatter

@Composable
fun TaskCard(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, animationSpec = tween(300), label = "drag")

    Box(
        modifier = Modifier.fillMaxWidth().offset(x = animatedOffset.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 100) onToggle() else if (offsetX < -100) onDelete()
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount -> offsetX = (offsetX + dragAmount).coerceIn(-150f, 150f) }
                )
            }
    ) {
        NeumorphicCard(isPressed = task.isCompleted, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    NeumorphicCheckbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(task.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NeumorphicColors.textPrimary.copy(alpha = if (task.isCompleted) 0.5f else 1f), textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                        task.dueDate?.let { Text(it.format(DateTimeFormatter.ofPattern("d MMM")), fontSize = 12.sp, color = NeumorphicColors.textSecondary, modifier = Modifier.padding(top = 4.dp)) }
                    }
                }
                PriorityIndicator(task.priority)
            }
        }
    }
}

@Composable
fun NeumorphicCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier.size(28.dp)
            .shadow(if (checked) 0.dp else 6.dp, RoundedCornerShape(8.dp), ambientColor = NeumorphicColors.darkShadow, spotColor = NeumorphicColors.lightShadow)
            .background(if (checked) NeumorphicColors.accentBlue else NeumorphicColors.surface, RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Default.Check, "Checked", tint = NeumorphicColors.textPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun PriorityIndicator(priority: Priority) {
    val color = when (priority) { Priority.HIGH -> NeumorphicColors.accentPeach; Priority.NORMAL -> NeumorphicColors.accentBlue; Priority.LOW -> NeumorphicColors.accentMint }
    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
}