package com.example.myapplication.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.Collection
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.model.FestivalUtils
import com.example.myapplication.model.Event
import com.example.myapplication.ui.theme.NeumorphicColors
import android.content.pm.ApplicationInfo
import android.util.Log
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    collections: List<Collection>,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    // Compact sheet state (collapsed or expanded)
    var sheetExpanded by remember { mutableStateOf(false) }

    // group tasks by date (derived for performance) - ignore tasks without a dueDate so keys are non-null
    val tasksByDate by remember(tasks) { derivedStateOf { tasks.filter { it.dueDate != null }.groupBy { it.dueDate!! } } }
    val collectionNameMap by remember(collections) { derivedStateOf { collections.associate { it.id to it.name } } }

    Column(modifier = Modifier.fillMaxSize().background(NeumorphicColors.background)) {
        // Header: day string and month selector (high info density)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val today = selectedDate ?: LocalDate.now()
            Column {
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphicColors.textPrimary
                )
                val taskCount = (tasksByDate[selectedDate ?: LocalDate.now()] ?: emptyList()).size
                Text(
                    text = stringResource(R.string.tasks_count, taskCount),
                    fontSize = 12.sp,
                    color = NeumorphicColors.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
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
                IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_month),
                        tint = NeumorphicColors.textPrimary
                    )
                }
            }
        }

        // Compact month grid
        CompactMonthGrid(
            yearMonth = selectedMonth,
            tasksByDate = tasksByDate,
            selectedDate = selectedDate,
            onSelectDate = { selectedDate = it },
            onToggleMonth = { /* no-op; month selector is above */ }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Draggable/lightweight sheet area for tasks
        val collapsedHeight = 500.dp
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        val expandedHeight = maxOf(screenHeightDp - 40.dp, 320.dp).coerceAtMost(800.dp)
        val targetHeight = if (sheetExpanded) expandedHeight else collapsedHeight
        val animatedHeight by animateDpAsState(targetHeight)

        // Density-aware drag threshold (20.dp converted to pixels)
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
                        // simple toggle on drag up/down threshold (density-independent)
                        if (dragAmount < -thresholdPx) sheetExpanded = true
                        if (dragAmount > thresholdPx) sheetExpanded = false
                        change.consume()
                    }
                }
        ) {
            // draggable handle
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeumorphicColors.textSecondary.copy(alpha = 0.28f))
                    .clickable { sheetExpanded = !sheetExpanded }
            )

            // Task list body
            Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 8.dp)) {
                val date = selectedDate ?: LocalDate.now()
                val dayTasks = remember(tasksByDate, date) { tasksByDate[date] ?: emptyList() }
                val dayEvents = remember(date) { FestivalUtils.getEventsForDate(date) }
                val combinedItems = remember(dayEvents, dayTasks) {
                    buildList {
                        dayEvents.forEach { add(CalendarListItem.EventItem(it)) }
                        dayTasks.forEach { add(CalendarListItem.TaskItem(it)) }
                    }
                }

                if (dayTasks.isEmpty() && dayEvents.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_items_today), color = NeumorphicColors.textSecondary)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = combinedItems,
                            key = { index, item ->
                                when (item) {
                                    is CalendarListItem.EventItem -> "ev_${index}_${item.event.title}"
                                    is CalendarListItem.TaskItem -> "task_${item.task.id}"
                                }
                            }
                        ) { index, item ->
                            when (item) {
                                is CalendarListItem.EventItem -> {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Dot(color = NeumorphicColors.accentBlue)
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(item.event.title, color = NeumorphicColors.textPrimary)
                                            item.event.description?.let {
                                                Text(it, color = NeumorphicColors.textSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                is CalendarListItem.TaskItem -> {
                                    CompactTaskItem(
                                        task = item.task,
                                        collectionNameMap = collectionNameMap,
                                        onToggle = { onTaskToggle(item.task.id) },
                                        onDelete = { onTaskDelete(item.task.id) }
                                    )
                                }
                            }

                            if (index != combinedItems.lastIndex) {
                                HorizontalDivider(
                                    color = NeumorphicColors.background.copy(alpha = 0.12f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface CalendarListItem {
    data class EventItem(val event: Event) : CalendarListItem
    data class TaskItem(val task: Task) : CalendarListItem
}

@Composable
private fun CompactMonthGrid(
    yearMonth: YearMonth,
    tasksByDate: Map<LocalDate, List<Task>>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onToggleMonth: () -> Unit
) {
    val context = LocalContext.current
    val isDebuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    val firstOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstOfMonth.dayOfWeek.value + 6) % 7 // Monday=0 .. Sunday=6
    val dayList = (1..daysInMonth).toList()

    // Precompute indicators map: date -> set of priorities present and events present
    val indicators by remember(tasksByDate, yearMonth) {
        derivedStateOf {
            val map = mutableMapOf<Int, Set<Priority>>()
            for (d in 1..daysInMonth) {
                val date = yearMonth.atDay(d)
                val priorities = tasksByDate[date]?.map { it.priority }?.toSet() ?: emptySet()
                if (priorities.isNotEmpty()) map[d] = priorities
            }
            map
        }
    }

    val eventsMap by remember(yearMonth) {
        derivedStateOf { FestivalUtils.getEventsForMonth(yearMonth).mapKeys { entry -> entry.key.dayOfMonth } }
    }

    // Precompute lunar conversions for the month to avoid recomputing per cell
    val lunarMap by remember(yearMonth) {
        derivedStateOf {
            val m = mutableMapOf<Int, com.example.myapplication.model.LunarUtils.LunarDate?>()
            for (d in 1..daysInMonth) {
                val date = yearMonth.atDay(d)
                try {
                    m[d] = com.example.myapplication.model.LunarUtils.convertSolar2Lunar(date)
                } catch (ex: IllegalArgumentException) {
                    Log.e("CalendarScreen", "Lunar conversion failed for date=$date: ${ex.message}", ex)
                    m[d] = null
                } catch (ex: ArithmeticException) {
                    Log.e("CalendarScreen", "Lunar conversion arithmetic error for date=$date: ${ex.message}", ex)
                    m[d] = null
                } catch (ex: Exception) {
                    // Unexpected exception - log; only rethrow in debug to avoid crashing production
                    Log.e("CalendarScreen", "Unexpected error converting lunar for date=$date", ex)
                    if (isDebuggable) throw ex else m[d] = null
                }
            }
            m
        }
    }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val weekdayLabels = stringArrayResource(id = R.array.weekday_short)
            weekdayLabels.forEach { dow ->
                Text(
                    text = dow,
                    fontSize = 12.sp,
                    color = NeumorphicColors.textSecondary,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // compute number of rows needed for the month (including leading padding)
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7 // integer ceil
        val cellSize = 44.dp
        val verticalSpacing = 4.dp
        val gridHeight = (cellSize * rows) + (verticalSpacing * (rows - 1)) + 8.dp // extra padding

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(verticalSpacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = {
                // leading padding cells
                val padList = List(startOffset) { it }
                items(padList, key = { "pad_$it" }) { Box(Modifier.size(cellSize)) }

                // day cells
                items(items = dayList, key = { d -> yearMonth.atDay(d).toEpochDay() }) { day ->
                    val date = yearMonth.atDay(day)
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()
                    val priorities = indicators[day] ?: emptySet()

                    CalendarDayCell(
                        day = day,
                        cellSize = cellSize,
                        isSelected = isSelected,
                        isToday = isToday,
                        priorities = priorities,
                        events = eventsMap[day] ?: emptyList(),
                        lunar = lunarMap[day],
                        onClick = { onSelectDate(date) }
                    )
                }
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    cellSize: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    isToday: Boolean,
    priorities: Set<Priority>,
    events: List<com.example.myapplication.model.Event>,
    lunar: com.example.myapplication.model.LunarUtils.LunarDate?,
    onClick: () -> Unit
) {
    // Minimal surfaces; only use elevation when necessary and keep it tiny
    val accent = NeumorphicColors.accentBlue
    val containerColor = when {
        isSelected -> accent
        isToday -> NeumorphicColors.accentMint.copy(0.12f)
        else -> NeumorphicColors.surface
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Day number and lunar small label
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else NeumorphicColors.textPrimary
            )

            lunar?.let {
                // show lunar day and month compactly, e.g. "17/11" or when leap month include L
                val lm = if (it.isLeap) "L${it.month}" else it.month.toString()
                Text(
                    text = "${it.day}/${lm}",
                    fontSize = 9.sp,
                    color = NeumorphicColors.textSecondary
                )
            }
        }

        // Event and priority indicators - small dots under the date number
        if (priorities.isNotEmpty() || events.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // priority dots (công việc người dùng đặt)
                if (Priority.LOW in priorities) {
                    Dot(color = NeumorphicColors.priorityLow)      // xanh lá
                }
                if (Priority.NORMAL in priorities) {
                    Dot(color = NeumorphicColors.priorityNormal)  // vàng
                }
                if (Priority.HIGH in priorities) {
                    Dot(color = NeumorphicColors.priorityHigh)    // đỏ
                }

                // event dot: một chấm xanh biển nếu có ít nhất một sự kiện
                if (events.isNotEmpty()) {
                    Dot(color = NeumorphicColors.accentBlue)
                }
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun CompactTaskItem(
    task: Task,
    collectionNameMap: Map<Int, String>,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    // Slim list tile: left time (if none -> small placeholder), middle title + meta, right checkbox
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: time (we only have date in Task; show 'All day' as compact label)
        Text(
            text = stringResource(R.string.all_day),
            fontSize = 12.sp,
            color = NeumorphicColors.textSecondary,
            modifier = Modifier.width(52.dp),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, fontSize = 14.sp, color = NeumorphicColors.textPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            // Secondary meta: show priority + maybe collection id if available
            val meta = buildString {
                val priorityLabel = when (task.priority) {
                    Priority.HIGH -> stringResource(R.string.priority_high)
                    Priority.NORMAL -> stringResource(R.string.priority_normal)
                    Priority.LOW -> stringResource(R.string.priority_low)
                }
                append(priorityLabel)
                task.collectionId?.let { cid ->
                    val name = collectionNameMap[cid] ?: cid.toString()
                    append(" • ")
                    append(stringResource(R.string.collection_prefix, name))
                }
            }
            Text(meta, fontSize = 12.sp, color = NeumorphicColors.textSecondary)
        }

        // Right: subtle checkbox (small Card) + delete button
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = if (task.isCompleted) NeumorphicColors.accentMint else NeumorphicColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().clickable { onToggle() }, contentAlignment = Alignment.Center) {
                    if (task.isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.cd_done), tint = NeumorphicColors.textPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete), tint = NeumorphicColors.textSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}