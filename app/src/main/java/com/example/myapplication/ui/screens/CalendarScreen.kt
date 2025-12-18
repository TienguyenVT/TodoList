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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Priority
import com.example.myapplication.model.Task
import com.example.myapplication.model.FestivalUtils
import com.example.myapplication.model.Event
import com.example.myapplication.ui.theme.NeumorphicColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    onTaskToggle: (Int) -> Unit,
    onTaskDelete: (Int) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    // Compact sheet state (collapsed or expanded)
    var sheetExpanded by remember { mutableStateOf(false) }

    // group tasks by date (derived for performance) - ignore tasks without a dueDate so keys are non-null
    val tasksByDate by remember(tasks) { derivedStateOf { tasks.filter { it.dueDate != null }.groupBy { it.dueDate!! } } }

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
                Text(
                    text = "${(tasksByDate[selectedDate ?: LocalDate.now()] ?: emptyList()).size} công việc",
                    fontSize = 12.sp,
                    color = NeumorphicColors.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev", tint = NeumorphicColors.textPrimary)
                }
                Text(
                    text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    fontSize = 14.sp,
                    color = NeumorphicColors.textPrimary
                )
                IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = NeumorphicColors.textPrimary)
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
        val collapsedHeight = 140.dp
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        val expandedHeight = maxOf(screenHeightDp - 80.dp, 220.dp)
        val targetHeight = if (sheetExpanded) expandedHeight else collapsedHeight
        val animatedHeight by animateDpAsState(targetHeight)

        Box(
            Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(NeumorphicColors.surface)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        // simple toggle on drag up/down threshold
                        if (dragAmount < -20) sheetExpanded = true
                        if (dragAmount > 20) sheetExpanded = false
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

                if (dayTasks.isEmpty() && dayEvents.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không có việc trong ngày này", color = NeumorphicColors.textSecondary)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // First show events section if present
                        if (dayEvents.isNotEmpty()) {
                            item {
                                Text("Sự kiện", fontWeight = FontWeight.SemiBold, color = NeumorphicColors.textPrimary, modifier = Modifier.padding(vertical = 8.dp))
                                dayEvents.forEach { ev ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Dot(color = if (ev.isLunar) NeumorphicColors.accentPeach else NeumorphicColors.accentBlue)
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(ev.title, color = NeumorphicColors.textPrimary)
                                            ev.description?.let { Text(it, color = NeumorphicColors.textSecondary, fontSize = 12.sp) }
                                        }
                                    }
                                }
                                Divider(color = NeumorphicColors.background.copy(alpha = 0.12f), thickness = 0.5.dp)
                            }
                        }
                        items(items = dayTasks, key = { it.id }) { task ->
                            CompactTaskItem(
                                task = task,
                                onToggle = { onTaskToggle(task.id) },
                                onDelete = { onTaskDelete(task.id) }
                            )
                            Divider(color = NeumorphicColors.background.copy(alpha = 0.12f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMonthGrid(
    yearMonth: YearMonth,
    tasksByDate: Map<LocalDate, List<Task>>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onToggleMonth: () -> Unit
) {
    val firstOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstOfMonth.dayOfWeek.value % 7) // Sunday=0
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
                } catch (_: Exception) {
                    m[d] = null
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
            listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7").forEach { dow ->
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // priority dots
                if (Priority.HIGH in priorities) Dot(color = NeumorphicColors.accentPeach)
                if (Priority.NORMAL in priorities) Dot(color = NeumorphicColors.accentBlue)
                if (Priority.LOW in priorities && priorities.size == 1) Dot(color = NeumorphicColors.accentMint)

                // event dots (distinct small marker)
                events.take(2).forEach { ev ->
                    Dot(color = if (ev.isLunar) NeumorphicColors.accentPeach else NeumorphicColors.accentBlue)
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
private fun CompactTaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    // Slim list tile: left time (if none -> small placeholder), middle title + meta, right checkbox
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: time (we only have date in Task; show 'All day' as compact label)
        Text(
            text = "All day",
            fontSize = 12.sp,
            color = NeumorphicColors.textSecondary,
            modifier = Modifier.width(64.dp),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, fontSize = 14.sp, color = NeumorphicColors.textPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            // Secondary meta: show priority + maybe collection id if available
            val meta = buildString {
                append(when (task.priority) {
                    Priority.HIGH -> "Cao"
                    Priority.NORMAL -> "Bình thường"
                    Priority.LOW -> "Thấp"
                })
                if (task.collectionId != null) append(" • Danh mục ${task.collectionId}")
            }
            Text(meta, fontSize = 12.sp, color = NeumorphicColors.textSecondary)
        }

        // Right: subtle checkbox (small Card)
        Card(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = if (task.isCompleted) NeumorphicColors.accentMint else NeumorphicColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().clickable { onToggle() }, contentAlignment = Alignment.Center) {
                if (task.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = NeumorphicColors.textPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}