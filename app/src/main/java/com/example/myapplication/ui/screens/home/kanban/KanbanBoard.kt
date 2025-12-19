package com.example.myapplication.ui.screens.home.kanban

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myapplication.R
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.NeumorphicColors
import kotlin.math.roundToInt

enum class KanbanColumn(val title: String) {
    UNCOMPLETED("Chưa hoàn thành"),
    IN_PROGRESS("Đang thực hiện"),
    COMPLETED("Đã hoàn thành")
}

data class KanbanTask(
    val task: Task,
    val column: KanbanColumn
)

private data class DragInfo(
    val item: KanbanTask,
    val pointerInWindow: Offset,
    val anchorInItem: Offset
)

@Composable
fun KanbanBoard(
    tasks: List<Task>,
    collections: Map<Int, String>,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit,
    onTaskStatusChange: (Int, KanbanColumn) -> Unit
) {
    // Group tasks by Kanban columns
    val tasksByColumn = remember(tasks) {
        tasks.groupBy { task ->
            when {
                task.isCompleted -> KanbanColumn.COMPLETED
                task.priority == Priority.HIGH -> KanbanColumn.IN_PROGRESS
                else -> KanbanColumn.UNCOMPLETED
            }
        }.mapValues { entry ->
            entry.value.map { KanbanTask(it, entry.key) }
        }
    }

    // Track dragged item
    var dragInfo by remember { mutableStateOf<DragInfo?>(null) }
    var dragOverColumn by remember { mutableStateOf<KanbanColumn?>(null) }

    // Track bounds of each column (row now) in window coordinates
    val columnBounds = remember { mutableStateMapOf<KanbanColumn, Rect>() }
    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicColors.background)
            .onGloballyPositioned { boardBounds = it.boundsInWindow() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // stacked rows: one card per KanbanColumn
            KanbanColumn.values().forEach { column ->
                KanbanColumnCard(
                    column = column,
                    tasks = tasksByColumn[column] ?: emptyList(),
                    collections = collections,
                    draggedItem = dragInfo?.item,
                    isDragOver = dragOverColumn == column,
                    onGloballyPositioned = { coords ->
                        columnBounds[column] = coords.boundsInWindow()
                    },
                    onDragStart = { item, pointerInWindow, anchorInItem ->
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        dragInfo = DragInfo(item = item, pointerInWindow = pointerInWindow, anchorInItem = anchorInItem)
                        dragOverColumn = columnBounds.entries.firstOrNull { (_, rect) -> rect.contains(pointerInWindow) }?.key
                    },
                    onDrag = { pointerInWindow ->
                        dragInfo = dragInfo?.copy(pointerInWindow = pointerInWindow)
                        val hovered = columnBounds.entries.firstOrNull { (_, rect) -> rect.contains(pointerInWindow) }?.key
                        if (hovered != dragOverColumn) {
                            dragOverColumn = hovered
                            if (hovered != null) {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onDragEnd = {
                        val target = dragOverColumn
                        val item = dragInfo?.item
                        if (target != null && item != null && target != item.column) {
                            onTaskStatusChange(item.task.id, target)
                        }
                        dragInfo = null
                        dragOverColumn = null
                    },
                    onTaskToggle = onTaskToggle,
                    onTaskDelete = onTaskDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 250.dp)
                )
            }
        }

        val overlay = dragInfo
        if (overlay != null) {
            val boardTopLeft = boardBounds?.topLeft ?: Offset.Zero
            DraggedTaskOverlay(
                kanbanTask = overlay.item,
                collections = collections,
                pointerInWindow = overlay.pointerInWindow,
                anchorInItem = overlay.anchorInItem,
                boardTopLeftInWindow = boardTopLeft,
                modifier = Modifier.zIndex(10f)
            )
        }
    }
}

@Composable
private fun KanbanColumnCard(
    column: KanbanColumn,
    tasks: List<KanbanTask>,
    collections: Map<Int, String>,
    draggedItem: KanbanTask?,
    isDragOver: Boolean,
    onGloballyPositioned: (LayoutCoordinates) -> Unit,
    onDragStart: (KanbanTask, Offset, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColor by animateColorAsState(
        targetValue = if (isDragOver) NeumorphicColors.accentMint.copy(alpha = 0.12f) else NeumorphicColors.surface,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = column.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.textPrimary
                    )
                    Text(
                        text = "${tasks.size} công việc",
                        fontSize = 12.sp,
                        color = NeumorphicColors.textSecondary
                    )
                }
            }

            // Task list
            val listState = rememberLazyListState()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                if (isDragOver && draggedItem != null && draggedItem.column != column) {
                    item(key = "drop_hint_${column.name}") {
                        Surface(
                            color = NeumorphicColors.accentMint.copy(alpha = 0.10f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = NeumorphicColors.accentMint.copy(alpha = 0.45f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Thả để chuyển vào \"${column.title}\"",
                                fontSize = 12.sp,
                                color = NeumorphicColors.textSecondary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                items(tasks, key = { it.task.id }) { kanbanTask ->
                    DraggableKanbanTask(
                        kanbanTask = kanbanTask,
                        collections = collections,
                        isBeingDragged = draggedItem == kanbanTask,
                        onDragStart = { pointerInWindow: Offset, anchorInItem: Offset ->
                            onDragStart(kanbanTask, pointerInWindow, anchorInItem)
                        },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onToggle = { onTaskToggle(kanbanTask.task.id) },
                        onDelete = { onTaskDelete(kanbanTask.task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableKanbanTask(
    kanbanTask: KanbanTask,
    collections: Map<Int, String>,
    isBeingDragged: Boolean,
    onDragStart: (Offset, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
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
            .fillMaxWidth()
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
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .then(Modifier)
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
                Column {
                    val priorityLabel = when (task.priority) {
                        Priority.HIGH -> stringResource(R.string.priority_high)
                        Priority.NORMAL -> stringResource(R.string.priority_normal)
                        Priority.LOW -> stringResource(R.string.priority_low)
                    }
                    Text(
                        text = priorityLabel,
                        fontSize = 12.sp,
                        color = NeumorphicColors.textSecondary
                    )

                    task.collectionId?.let { cid ->
                        val name = collections[cid] ?: cid.toString()
                        Text(
                            text = stringResource(R.string.collection_prefix, name),
                            fontSize = 12.sp,
                            color = NeumorphicColors.textSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.cd_done),
                            tint = if (task.isCompleted) NeumorphicColors.accentMint else NeumorphicColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete),
                            tint = NeumorphicColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Drag handle: long-press then drag
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .onGloballyPositioned { handleRectInWindow = it.boundsInWindow() }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val card = cardRectInWindow
                                        val handle = handleRectInWindow
                                        if (card != null && handle != null) {
                                            val pointerInWindow = handle.topLeft + offset
                                            val anchorInItem = pointerInWindow - card.topLeft
                                            onDragStart(pointerInWindow, anchorInItem)
                                        }
                                    },
                                    onDragEnd = {
                                        onDragEnd()
                                    },
                                    onDragCancel = {
                                        // Cleanup drag state when gesture is cancelled
                                        onDragEnd()
                                    }
                                ) { change, _ ->
                                    val handle = handleRectInWindow
                                    if (handle != null) {
                                        onDrag(handle.topLeft + change.position)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = stringResource(R.string.cd_drag),
                            tint = NeumorphicColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (isBeingDragged) {
        Spacer(modifier = Modifier.height(0.dp))
    }
}

@Composable
private fun DraggedTaskOverlay(
    kanbanTask: KanbanTask,
    collections: Map<Int, String>,
    pointerInWindow: Offset,
    anchorInItem: Offset,
    boardTopLeftInWindow: Offset,
    modifier: Modifier = Modifier
) {
    val task = kanbanTask.task
    val offsetX = (pointerInWindow.x - boardTopLeftInWindow.x - anchorInItem.x).roundToInt()
    val offsetY = (pointerInWindow.y - boardTopLeftInWindow.y - anchorInItem.y).roundToInt()

    Card(
        modifier = modifier
            .offset { androidx.compose.ui.unit.IntOffset(offsetX, offsetY) }
            .widthIn(max = 360.dp)
            .shadow(12.dp, MaterialTheme.shapes.medium)
            .zIndex(10f),
        colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NeumorphicColors.textPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val priorityLabel = when (task.priority) {
                        Priority.HIGH -> stringResource(R.string.priority_high)
                        Priority.NORMAL -> stringResource(R.string.priority_normal)
                        Priority.LOW -> stringResource(R.string.priority_low)
                    }
                    Text(
                        text = priorityLabel,
                        fontSize = 12.sp,
                        color = NeumorphicColors.textSecondary
                    )

                    task.collectionId?.let { cid ->
                        val name = collections[cid] ?: cid.toString()
                        Text(
                            text = stringResource(R.string.collection_prefix, name),
                            fontSize = 12.sp,
                            color = NeumorphicColors.textSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.cd_drag),
                    tint = NeumorphicColors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
