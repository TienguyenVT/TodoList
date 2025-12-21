package com.example.myapplication.ui.screens.calendar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.Collection
import com.example.myapplication.model.Event
import com.example.myapplication.model.FestivalUtils
import com.example.myapplication.model.Task
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.example.myapplication.utils.PerfLogger

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    collections: List<Collection>,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var sheetExpanded by remember { mutableStateOf(false) }

    val tasksByDate by remember(tasks) {
        derivedStateOf { tasks.filter { it.dueDate != null }.groupBy { it.dueDate!! } }
    }
    val collectionNameMap by remember(collections) {
        derivedStateOf { collections.associate { it.id to it.name } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicColors.background)
    ) {
        Header(
            selectedDate = selectedDate,
            selectedMonth = selectedMonth,
            taskCountForSelected = (tasksByDate[selectedDate ?: LocalDate.now()] ?: emptyList()).size,
            onPrevMonth = { selectedMonth = selectedMonth.minusMonths(1) },
            onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) }
        )

        CompactMonthGrid(
            yearMonth = selectedMonth,
            tasksByDate = tasksByDate,
            selectedDate = selectedDate,
            onSelectDate = { selectedDate = it },
            onToggleMonth = { /* no-op */ }
        )

        Spacer(modifier = Modifier.height(8.dp))

        val collapsedHeight = 500.dp
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        val expandedHeight = maxOf(screenHeightDp - 40.dp, 320.dp).coerceAtMost(800.dp)
        val targetHeight = if (sheetExpanded) expandedHeight else collapsedHeight
        val animatedHeight by animateDpAsState(targetHeight)
        val thresholdPx = with(LocalDensity.current) { 20.dp.toPx() }

        Box(
            Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(NeumorphicColors.surface)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (dragAmount < -thresholdPx) sheetExpanded = true
                        if (dragAmount > thresholdPx) sheetExpanded = false
                    }
                }
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeumorphicColors.textSecondary.copy(alpha = 0.28f))
                    .clickable { sheetExpanded = !sheetExpanded }
            )

            TaskListSection(
                selectedDate = selectedDate ?: LocalDate.now(),
                tasksByDate = tasksByDate,
                collectionNameMap = collectionNameMap,
                onTaskToggle = onTaskToggle,
                onTaskDelete = onTaskDelete
            )
        }
    }
}

@Composable
private fun Header(
    selectedDate: LocalDate?,
    selectedMonth: YearMonth,
    taskCountForSelected: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val today = selectedDate ?: LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeumorphicColors.textPrimary
            )
            Text(
                text = stringResource(R.string.tasks_count, taskCountForSelected),
                fontSize = 12.sp,
                color = NeumorphicColors.textSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.prev_month),
                    tint = NeumorphicColors.textPrimary
                )
            }
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 14.sp,
                color = NeumorphicColors.textPrimary
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month),
                    tint = NeumorphicColors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun TaskListSection(
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    collectionNameMap: Map<Int, String>,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 8.dp)) {
        val dayTasks = remember(tasksByDate, selectedDate) { tasksByDate[selectedDate] ?: emptyList() }
        val dayEvents = remember(selectedDate) { FestivalUtils.getEventsForDate(selectedDate) }
        val combinedItems = remember(dayEvents, dayTasks) {
            buildList {
                dayEvents.forEach { add(CalendarListItem.EventItem(it)) }
                dayTasks.forEach { add(CalendarListItem.TaskItem(it)) }
            }
        }
        
        // OPTIMIZATION: Limit initial render to 15 items
        val INITIAL_ITEM_LIMIT = 15
        var isFullyLoaded by remember { mutableStateOf(false) }
        
        LaunchedEffect(combinedItems) {
            isFullyLoaded = false
            kotlinx.coroutines.delay(150) // Wait for animation
            isFullyLoaded = true
        }
        
        val limitedItems = remember(combinedItems, isFullyLoaded) {
            if (isFullyLoaded) combinedItems else combinedItems.take(INITIAL_ITEM_LIMIT)
        }

        if (dayTasks.isEmpty() && dayEvents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_items_today), color = NeumorphicColors.textSecondary)
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = limitedItems,
                    key = { index, item ->
                        when (item) {
                            is CalendarListItem.EventItem -> "ev_${index}_${item.event.title}"
                            is CalendarListItem.TaskItem -> "task_${item.task.id}"
                        }
                    }
                ) { index, item ->
                    when (item) {
                        is CalendarListItem.EventItem -> EventRow(item.event)
                        is CalendarListItem.TaskItem -> CompactTaskItem(
                            task = item.task,
                            collectionNameMap = collectionNameMap,
                            onToggle = { onTaskToggle(item.task.id) },
                            onDelete = { onTaskDelete(item.task.id) }
                        )
                    }

                    if (index != combinedItems.lastIndex) {
                        HorizontalDivider(
                            color = NeumorphicColors.background.copy(alpha = 0.12f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
            
            // Performance logging
            androidx.compose.runtime.SideEffect {
                PerfLogger.logRender(
                    file = "CalendarScreen.kt",
                    function = "TaskListSection",
                    itemCount = combinedItems.size
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: Event) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(color = NeumorphicColors.accentBlue)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(event.title, color = NeumorphicColors.textPrimary)
            event.description?.let {
                Text(it, color = NeumorphicColors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

private sealed interface CalendarListItem {
    data class EventItem(val event: Event) : CalendarListItem
    data class TaskItem(val task: Task) : CalendarListItem
}
