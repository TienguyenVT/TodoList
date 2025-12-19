package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Task
import com.example.myapplication.ui.components.AnimateEntrance
import com.example.myapplication.ui.components.TaskCard
import com.example.myapplication.ui.theme.NeumorphicColors

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MyDayScreen(tasks: List<Task>, onTaskToggle: (Int) -> Unit, onTaskDelete: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(8.dp))

        // Phase 1: Header appears first
        AnimateEntrance(delayMillis = 0) {
            Column {
                Text(
                    "Hôm nay",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.textPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("vi"))
                    ),
                    fontSize = 16.sp,
                    color = NeumorphicColors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // Phase 2: Content (empty state or list) appears slightly after
        AnimateEntrance(delayMillis = 100) {
            if (tasks.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("✨", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Hôm nay sạch sẽ!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeumorphicColors.textPrimary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { onTaskToggle(task.id) },
                            onDelete = { onTaskDelete(task.id) }
                        )
                    }
                }
            }
        }
    }
}