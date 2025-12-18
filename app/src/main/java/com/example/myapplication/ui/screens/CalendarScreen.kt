package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Task
import com.example.myapplication.ui.components.TaskCard
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(tasks: List<Task>, onTaskToggle: (Int) -> Unit, onTaskDelete: (Int) -> Unit) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Lịch", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textPrimary, modifier = Modifier.padding(vertical = 16.dp))

        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) { Icon(Icons.Default.KeyboardArrowLeft, "Prev", tint = NeumorphicColors.textPrimary) }
            Text(selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = NeumorphicColors.textPrimary)
            IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) { Icon(Icons.Default.KeyboardArrowRight, "Next", tint = NeumorphicColors.textPrimary) }
        }
        CalendarGrid(selectedMonth, tasks, selectedDate) { selectedDate = it }
        Spacer(Modifier.height(24.dp))
        selectedDate?.let { date ->
            Text("Công việc ngày ${date.format(DateTimeFormatter.ofPattern("d MMM"))}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NeumorphicColors.textPrimary, modifier = Modifier.padding(bottom = 16.dp))
            val dateTasks = tasks.filter { it.dueDate == date }
            if (dateTasks.isEmpty()) Text("Không có việc", fontSize = 14.sp, color = NeumorphicColors.textSecondary)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(dateTasks, key = { it.id }) { task -> TaskCard(task, { onTaskToggle(task.id) }, { onTaskDelete(task.id) }) } }
        }
    }
}

@Composable
fun CalendarGrid(yearMonth: YearMonth, tasks: List<Task>, selectedDate: LocalDate?, onDateSelected: (LocalDate) -> Unit) {
    val firstDay = yearMonth.atDay(1); val days = yearMonth.lengthOfMonth(); val startDay = firstDay.dayOfWeek.value % 7
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7").forEach { Text(it, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(startDay) { Box(Modifier.aspectRatio(1f)) }
            items(days) { day ->
                val date = yearMonth.atDay(day + 1)
                val isSelected = date == selectedDate; val isToday = date == LocalDate.now(); val hasTasks = tasks.any { it.dueDate == date }
                Box(Modifier.aspectRatio(1f).shadow(if(isSelected) 0.dp else 6.dp, RoundedCornerShape(12.dp)).background(if(isSelected) NeumorphicColors.accentBlue else if(isToday) NeumorphicColors.accentMint.copy(0.5f) else NeumorphicColors.surface, RoundedCornerShape(12.dp)).clickable{onDateSelected(date)}, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text((day + 1).toString(), fontSize = 14.sp, fontWeight = if(isToday) FontWeight.Bold else FontWeight.Normal, color = NeumorphicColors.textPrimary); if(hasTasks) Box(Modifier.size(4.dp).clip(CircleShape).background(NeumorphicColors.accentPeach)) }
                }
            }
        }
    }
}