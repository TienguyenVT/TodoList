package com.example.myapplication.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Priority
import com.example.myapplication.ui.components.NeumorphicButton
import com.example.myapplication.ui.components.NeumorphicTextField
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.LocalDate

@Composable
fun AddTaskSheet(onAddTask: (String, LocalDate?, Priority, Int?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NORMAL) }

    Column(Modifier.padding(24.dp)) {
        Text("Thêm việc mới", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textPrimary)
        Spacer(Modifier.height(24.dp))
        NeumorphicTextField(value = title, onValueChange = { title = it }, placeholder = "Bạn muốn làm gì?")
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Priority.entries.forEach { p ->
                val isSelected = priority == p
                Box(Modifier.shadow(if (isSelected) 0.dp else 6.dp, RoundedCornerShape(12.dp))
                    .background(if (isSelected) NeumorphicColors.darkShadow.copy(0.15f) else NeumorphicColors.surface, RoundedCornerShape(12.dp))
                    .clickable { priority = p }.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(p.name, color = if (isSelected) NeumorphicColors.textPrimary else NeumorphicColors.textSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        NeumorphicButton("Lưu công việc") { if (title.isNotBlank()) onAddTask(title, LocalDate.now(), priority, null) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun AddCollectionSheet(onAddCollection: (String, Color) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.padding(24.dp)) {
        Text("Danh mục mới", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textPrimary)
        Spacer(Modifier.height(24.dp))
        NeumorphicTextField(value = name, onValueChange = { name = it }, placeholder = "Tên danh mục")
        Spacer(Modifier.height(32.dp))
        NeumorphicButton("Tạo danh mục") { if (name.isNotBlank()) onAddCollection(name, NeumorphicColors.accentBlue) }
        Spacer(Modifier.height(24.dp))
    }
}