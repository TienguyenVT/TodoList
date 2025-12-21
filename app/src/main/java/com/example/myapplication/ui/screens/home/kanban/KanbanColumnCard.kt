package com.example.myapplication.ui.screens.home.kanban

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.NeumorphicColors
import com.example.myapplication.utils.PerfLogger

@Composable
fun KanbanColumnCard(
    uiState: KanbanColumnUiState,
    collections: Map<Int, String>,
    onGloballyPositioned: (LayoutCoordinates) -> Unit,
    boardActions: KanbanBoardActions,
    modifier: Modifier = Modifier
) {
    val targetColor by animateColorAsState(
        targetValue = if (uiState.isDragOver) NeumorphicColors.accentMint.copy(alpha = 0.12f) else NeumorphicColors.surface,
        label = "kanban_drop_target"
    )

    Card(
        modifier = modifier
            .onGloballyPositioned { onGloballyPositioned(it) },
        colors = CardDefaults.cardColors(
            containerColor = NeumorphicColors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(targetColor)
                .padding(12.dp)
        ) {
            // Column header
            KanbanColumnHeader(uiState.column, uiState.tasks.size)

            // Task list - 3 column grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(uiState.tasks, key = { it.task.id }) { kanbanTask ->
                    DraggableKanbanTask(
                        kanbanTask = kanbanTask,
                        collections = collections,
                        isBeingDragged = uiState.draggedItem == kanbanTask,
                        dragActions = boardActions.dragActions,
                        taskActions = boardActions.taskActions
                    )
                }
            }
            
            // Performance logging render
            LaunchedEffect(uiState.tasks.size) {
                 PerfLogger.logRender(
                    file = "KanbanColumnCard.kt",
                    function = "KanbanColumnCard(${uiState.column.title})",
                    itemCount = uiState.tasks.size
                )
            }
        }
    }
}

@Composable
fun KanbanColumnHeader(column: KanbanColumn, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = column.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.textPrimary
        )
        Text(
            text = stringResource(R.string.task_count_format, count),
            fontSize = 12.sp,
            color = NeumorphicColors.textSecondary
        )
    }
}
