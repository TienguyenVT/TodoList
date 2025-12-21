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


