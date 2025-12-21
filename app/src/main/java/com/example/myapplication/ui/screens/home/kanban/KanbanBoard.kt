package com.example.myapplication.ui.screens.home.kanban

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myapplication.R
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.NeumorphicColors
import com.example.myapplication.utils.PerfLogger
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

data class TaskActions(
    val onToggle: (Int) -> Unit,
    val onDelete: (Int) -> Unit,
    val onStatusChange: ((Int, KanbanColumn) -> Unit)? = null
)

data class DragActions(
    val onStart: (KanbanTask, Offset, Offset) -> Unit,
    val onDrag: (Offset) -> Unit,
    val onEnd: () -> Unit
)

@Composable
fun KanbanBoard(
    tasks: List<Task>,
    collections: Map<Int, String>,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit,
    onTaskStatusChange: (Int, KanbanColumn) -> Unit
) {
    val limitedTasksByColumn = rememberKanbanTasks(tasks)
    
    // Track dragged item
    var dragInfo by remember { mutableStateOf<DragInfo?>(null) }
    var dragOverColumn by remember { mutableStateOf<KanbanColumn?>(null) }
    
    // Track bounds
    val columnBounds = remember { mutableStateMapOf<KanbanColumn, Rect>() }
    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var boardWidthPx by remember { mutableStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val taskActions = remember(onTaskToggle, onTaskDelete, onTaskStatusChange) {
        TaskActions(onTaskToggle, onTaskDelete, onTaskStatusChange)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicColors.background)
            .onGloballyPositioned {
                boardBounds = it.boundsInWindow()
                boardWidthPx = it.size.width.toFloat()
            }
    ) {
        KanbanColumnsLayout(
            uiState = KanbanBoardUiState(
                tasksByColumn = limitedTasksByColumn,
                collections = collections,
                dragInfo = dragInfo,
                dragOverColumn = dragOverColumn,
                columnBounds = columnBounds
            ),
            callbacks = KanbanBoardCallbacks(
                onDragInfoChange = { dragInfo = it },
                onDragOverChange = { newDragOver -> 
                     if (newDragOver != dragOverColumn && newDragOver != null) {
                         haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                     }
                     dragOverColumn = newDragOver
                },
                onColumnBoundsChange = { col, rect -> columnBounds[col] = rect }
            ),
            taskActions = taskActions
        )

        KanbanBoardOverlay(
            dragInfo = dragInfo,
            collections = collections,
            boardBounds = boardBounds,
            boardWidthPx = boardWidthPx,
            density = density
        )
    }
}

@Composable
private fun KanbanBoardOverlay(
    dragInfo: DragInfo?,
    collections: Map<Int, String>,
    boardBounds: Rect?,
    boardWidthPx: Float,
    density: androidx.compose.ui.unit.Density
) {
    if (dragInfo != null) {
        val boardTopLeft = boardBounds?.topLeft ?: Offset.Zero
        val cardWidthDp = with(density) { (boardWidthPx / 3f).toDp() }
        
        DraggedTaskOverlay(
            dragInfo = dragInfo,
            collections = collections,
            boardTopLeftInWindow = boardTopLeft,
            cardWidth = cardWidthDp
        )
    }
}

@Composable
private fun rememberKanbanTasks(tasks: List<Task>): Map<KanbanColumn, List<KanbanTask>> {
    val tasksByColumn = remember(tasks) {
        tasks.groupBy { task ->
            when (task.status) {
                1 -> KanbanColumn.COMPLETED
                2 -> KanbanColumn.IN_PROGRESS
                else -> KanbanColumn.UNCOMPLETED
            }
        }.mapValues { entry ->
            entry.value.map { KanbanTask(it, entry.key) }
        }
    }
    
    val INITIAL_ITEM_LIMIT = 15
    var isFullyLoaded by remember { mutableStateOf(false) }
    
    LaunchedEffect(tasks) {
        isFullyLoaded = false
        kotlinx.coroutines.delay(150)
        isFullyLoaded = true
    }
    
    return remember(tasksByColumn, isFullyLoaded) {
        if (isFullyLoaded) tasksByColumn
        else tasksByColumn.mapValues { (_, tasks) -> tasks.take(INITIAL_ITEM_LIMIT) }
    }
}

@Composable
private fun KanbanColumnsLayout(
    uiState: KanbanBoardUiState,
    callbacks: KanbanBoardCallbacks,
    taskActions: TaskActions
) {
    val haptics = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KanbanColumn.entries.forEach { column ->
            val columnUiState = KanbanColumnUiState(
                column = column,
                tasks = uiState.tasksByColumn[column] ?: emptyList(),
                isDragOver = uiState.dragOverColumn == column,
                draggedItem = uiState.dragInfo?.item
            )

            KanbanColumnCard(
                uiState = columnUiState,
                collections = uiState.collections,
                onGloballyPositioned = { coords -> callbacks.onColumnBoundsChange(column, coords.boundsInWindow()) },
                dragActions = DragActions(
                    onStart = { item, pointer, anchor ->
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        callbacks.onDragInfoChange(DragInfo(item, pointer, anchor))
                        callbacks.onDragOverChange(uiState.columnBounds.entries.firstOrNull { (_, rect) -> rect.contains(pointer) }?.key)
                    },
                    onDrag = { pointer ->
                        callbacks.onDragInfoChange(uiState.dragInfo?.copy(pointerInWindow = pointer))
                        callbacks.onDragOverChange(uiState.columnBounds.entries.firstOrNull { (_, rect) -> rect.contains(pointer) }?.key)
                    },
                    onEnd = {
                        val target = uiState.dragOverColumn
                        val item = uiState.dragInfo?.item
                        if (target != null && item != null && target != item.column) {
                            taskActions.onStatusChange?.invoke(item.task.id, target)
                        }
                        callbacks.onDragInfoChange(null)
                        callbacks.onDragOverChange(null)
                    }
                ),
                taskActions = taskActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 250.dp)
            )
        }
    }
}

private data class KanbanBoardUiState(
    val tasksByColumn: Map<KanbanColumn, List<KanbanTask>>,
    val collections: Map<Int, String>,
    val dragInfo: DragInfo?,
    val dragOverColumn: KanbanColumn?,
    val columnBounds: Map<KanbanColumn, Rect>
)

private data class KanbanBoardCallbacks(
    val onDragInfoChange: (DragInfo?) -> Unit,
    val onDragOverChange: (KanbanColumn?) -> Unit,
    val onColumnBoundsChange: (KanbanColumn, Rect) -> Unit
)


data class KanbanColumnUiState(
    val column: KanbanColumn,
    val tasks: List<KanbanTask>,
    val isDragOver: Boolean,
    val draggedItem: KanbanTask?
)

@Composable
private fun DraggedTaskOverlay(
    dragInfo: DragInfo,
    collections: Map<Int, String>,
    boardTopLeftInWindow: Offset,
    cardWidth: Dp,
    modifier: Modifier = Modifier
) {
    val task = dragInfo.item.task
    val offsetX = (dragInfo.pointerInWindow.x - boardTopLeftInWindow.x - dragInfo.anchorInItem.x).roundToInt()
    val offsetY = (dragInfo.pointerInWindow.y - boardTopLeftInWindow.y - dragInfo.anchorInItem.y).roundToInt()

    Card(
        modifier = modifier
            .offset { androidx.compose.ui.unit.IntOffset(offsetX, offsetY) }
            .width(cardWidth)
            .shadow(12.dp, MaterialTheme.shapes.medium)
            .zIndex(10f),
        colors = CardDefaults.cardColors(containerColor = NeumorphicColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        KanbanTaskContent(
            task = task,
            collectionName = task.collectionId?.let { collections[it] ?: it.toString() },
            isOverlay = true,
            callbacks = KanbanTaskCallbacks(
                onToggle = {},
                onDelete = {},
                onHandlePositioned = {},
                onDragStart = {},
                onDrag = {},
                onDragEnd = {}
            )
        )
    }
}

@Composable
private fun KanbanColumnCard(
    uiState: KanbanColumnUiState,
    collections: Map<Int, String>,
    onGloballyPositioned: (LayoutCoordinates) -> Unit,
    dragActions: DragActions,
    taskActions: TaskActions,
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
                        dragActions = dragActions,
                        taskActions = taskActions
                    )
                }
            }
            
            // Performance logging render
            LaunchedEffect(uiState.tasks.size) {
                 PerfLogger.logRender(
                    file = "KanbanBoard.kt",
                    function = "KanbanColumnCard(${uiState.column.title})",
                    itemCount = uiState.tasks.size
                )
            }
        }
    }
}

@Composable
private fun KanbanColumnHeader(column: KanbanColumn, count: Int) {
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

@Composable
private fun DraggableKanbanTask(
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
private fun KanbanTaskContent(
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
