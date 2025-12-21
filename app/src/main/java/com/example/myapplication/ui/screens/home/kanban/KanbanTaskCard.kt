package com.example.myapplication.ui.screens.home.kanban

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.NeumorphicColors

@Composable
fun DraggableKanbanTask(
    kanbanTask: KanbanTask,
    collections: Map<Int, String>,
    isBeingDragged: Boolean,
    dragActions: DragActions,
    taskActions: TaskActions
) {
    val task = kanbanTask.task
    var cardRectInWindow by remember { mutableStateOf<Rect?>(null) }
    var handleRectInWindow by remember { mutableStateOf<Rect?>(null) }

    val cardAlpha by animateFloatAsState(
        targetValue = if (isBeingDragged) 0f else 1f,
        label = "kanban_drag_alpha"
    )

    Card(
        modifier = Modifier
            .alpha(cardAlpha)
            .onGloballyPositioned { cardRectInWindow = it.boundsInWindow() },
        colors = CardDefaults.cardColors(
            containerColor = if (isBeingDragged)
                NeumorphicColors.accentBlue.copy(alpha = 0.1f)
            else
                NeumorphicColors.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isBeingDragged) 8.dp else 2.dp
        )
    ) {
        KanbanTaskContent(
            task = task,
            collectionName = task.collectionId?.let { collections[it] ?: it.toString() },
            isOverlay = false,
            callbacks = KanbanTaskCallbacks(
                onToggle = { taskActions.onToggle(task.id) },
                onDelete = { taskActions.onDelete(task.id) },
                onHandlePositioned = { handleRectInWindow = it.boundsInWindow() },
                onDragStart = { offset ->
                    val card = cardRectInWindow
                    val handle = handleRectInWindow
                    if (card != null && handle != null) {
                        val pointerInWindow = handle.topLeft + offset
                        val anchorInItem = pointerInWindow - card.topLeft
                        dragActions.onStart(kanbanTask, pointerInWindow, anchorInItem)
                    }
                },
                onDrag = dragActions.onDrag,
                onDragEnd = dragActions.onEnd
            )
        )
    }
}

@Composable
fun KanbanTaskContent(
    task: Task,
    collectionName: String?,
    isOverlay: Boolean,
    callbacks: KanbanTaskCallbacks
) {
    val priorityColor = when (task.priority) {
        Priority.HIGH -> NeumorphicColors.priorityHigh
        Priority.NORMAL -> NeumorphicColors.priorityNormal
        Priority.LOW -> NeumorphicColors.priorityLow
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top priority color line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(priorityColor)
        )

        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            // Task title
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NeumorphicColors.textPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Task metadata and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Collection Name
                Column(modifier = Modifier.weight(1f)) {
                    if (collectionName != null) {
                        Text(
                            text = stringResource(R.string.collection_prefix, collectionName),
                            fontSize = 12.sp,
                            color = NeumorphicColors.textSecondary
                        )
                    }
                }

                // Actions and Drag Handle
                TaskControlRow(
                    isCompleted = task.isCompleted,
                    isOverlay = isOverlay,
                    callbacks = callbacks
                )
            }
        }
    }
}

data class KanbanTaskCallbacks(
    val onToggle: () -> Unit,
    val onDelete: () -> Unit,
    val onHandlePositioned: (LayoutCoordinates) -> Unit,
    val onDragStart: (Offset) -> Unit,
    val onDrag: (Offset) -> Unit,
    val onDragEnd: () -> Unit
)

@Composable
private fun TaskControlRow(
    isCompleted: Boolean,
    isOverlay: Boolean,
    callbacks: KanbanTaskCallbacks
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isOverlay) {
            IconButton(onClick = callbacks.onToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.cd_done),
                    tint = if (isCompleted) NeumorphicColors.accentMint else NeumorphicColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = callbacks.onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = NeumorphicColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Interactive Drag handle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .onGloballyPositioned(callbacks.onHandlePositioned)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = callbacks.onDragStart,
                            onDragEnd = callbacks.onDragEnd,
                            onDragCancel = callbacks.onDragEnd,
                            onDrag = { change, _ -> callbacks.onDrag(change.position) }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                DragHandleIcon()
            }
        } else {
            // Overlay Drag handle (Visual only)
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                DragHandleIcon()
            }
        }
    }
}

@Composable
private fun DragHandleIcon() {
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = stringResource(R.string.cd_drag),
        tint = NeumorphicColors.textSecondary,
        modifier = Modifier.size(20.dp)
    )
}
