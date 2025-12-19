package com.example.myapplication.ui.screens.kanban

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
 
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.NeumorphicColors

@Composable
fun KanbanHomeScreen(
    tasks: List<Task>,
    collections: Map<Int, String>,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit,
    onTaskStatusChange: (Int, KanbanColumn) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(NeumorphicColors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                val completedCount = tasks.count { it.isCompleted }
                val totalCount = tasks.size

                if (maxWidth < 420.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Bảng Kanban",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.textPrimary
                            )
                            Text(
                                text = "Kéo thả công việc giữa các cột",
                                fontSize = 14.sp,
                                color = NeumorphicColors.textSecondary
                            )
                        }
                        Text(
                            text = "$completedCount/$totalCount hoàn thành",
                            fontSize = 14.sp,
                            color = NeumorphicColors.textSecondary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bảng Kanban",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.textPrimary
                            )
                            Text(
                                text = "Kéo thả công việc giữa các cột",
                                fontSize = 14.sp,
                                color = NeumorphicColors.textSecondary
                            )
                        }

                        Text(
                            text = "$completedCount/$totalCount hoàn thành",
                            fontSize = 14.sp,
                            color = NeumorphicColors.textSecondary
                        )
                    }
                }
            }

            // Kanban Board
            KanbanBoard(
                tasks = tasks,
                collections = collections,
                onTaskToggle = onTaskToggle,
                onTaskDelete = onTaskDelete,
                onTaskStatusChange = onTaskStatusChange
            )
        }

        // FAB removed from Home tab per request
    }
}
